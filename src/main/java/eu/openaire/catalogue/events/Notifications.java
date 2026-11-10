package eu.openaire.catalogue.events;

import eu.openaire.catalogue.config.OpenAIREProperties;
import eu.openaire.catalogue.resource.BundleEvent;
import eu.openaire.catalogue.resource.BundleEvent.Action;
import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.Bundles;
import eu.openaire.catalogue.resource.ResourceKind;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Emails the registration team and the provider users when a record is registered, approved or rejected, and the
 * registration team alone when a rejected record is resubmitted. With emails disabled, or no mail sender configured,
 * the messages are logged only.
 */
@Component
public class Notifications {

    private static final Logger logger = LoggerFactory.getLogger(Notifications.class);
    private static final Set<Action> NOTIFIED = Set.of(Action.CREATED, Action.RESUBMITTED, Action.APPROVED,
            Action.REJECTED);
    private static final Set<Action> USERS_NOTIFIED = Set.of(Action.CREATED, Action.APPROVED, Action.REJECTED);

    private final OpenAIREProperties properties;
    private final BundleService bundles;
    private final Configuration templates;
    private final ObjectProvider<JavaMailSender> mailSender;

    public Notifications(OpenAIREProperties properties, BundleService bundles, Configuration templates,
                         ObjectProvider<JavaMailSender> mailSender) {
        this.properties = properties;
        this.bundles = bundles;
        this.templates = templates;
        this.mailSender = mailSender;
    }

    @Async
    @EventListener
    public void notify(BundleEvent event) {
        if (!NOTIFIED.contains(event.action())) {
            return;
        }
        Map<String, Object> model = new HashMap<>();
        model.put("project", properties.getProjectName());
        model.put("homepage", properties.getHomepage());
        model.put("type", event.kind() == ResourceKind.PROVIDER ? "Provider" : "Resource");
        model.put("id", Bundles.id(event.bundle()));
        model.put("name", name(event));
        model.put("action", event.action().name().toLowerCase());
        model.put("comment", comment(event.bundle()));

        send(properties.getEmails().getRegistrationTo(), teamSubject(model), "mail/registration-team.ftl", model);
        if (properties.getEmails().isProviderNotifications() && USERS_NOTIFIED.contains(event.action())) {
            for (String email : providerUsers(event)) {
                send(email, userSubject(model), "mail/provider-users.ftl", model);
            }
        }
    }

    private String teamSubject(Map<String, Object> model) {
        return switch (String.valueOf(model.get("action"))) {
            case "created" -> "[%s Portal] A new application for registering [%s] - ([%s]) as a new %s %s has been received and should be reviewed"
                    .formatted(model.get("project"), model.get("name"), model.get("id"), model.get("project"), model.get("type"));
            case "resubmitted" -> "[%s Portal] The rejected application of [%s] - ([%s]) for registering as a new %s %s has been updated and should be reviewed again"
                    .formatted(model.get("project"), model.get("name"), model.get("id"), model.get("project"), model.get("type"));
            case "approved" -> "[%s Portal] The application of [%s] - ([%s]) for registering as a new %s %s has been approved"
                    .formatted(model.get("project"), model.get("name"), model.get("id"), model.get("project"), model.get("type"));
            default -> "[%s Portal] The application of [%s] - ([%s]) for registering as a new %s %s has been rejected"
                    .formatted(model.get("project"), model.get("name"), model.get("id"), model.get("project"), model.get("type"));
        };
    }

    private String userSubject(Map<String, Object> model) {
        String state = switch (String.valueOf(model.get("action"))) {
            case "created" -> "has been received and is under review";
            case "approved" -> "has been approved";
            default -> "has been rejected";
        };
        return "[%s Portal] Your application for registering [%s] as a new %s %s to the %s Portal %s"
                .formatted(model.get("project"), model.get("name"), model.get("project"), model.get("type"),
                        model.get("project"), state);
    }

    private List<String> providerUsers(BundleEvent event) {
        try {
            String providerId = switch (event.kind()) {
                case PROVIDER -> Bundles.id(event.bundle());
                case SERVICE -> owner(ResourceKind.SERVICE, event.bundle());
                case DATASOURCE -> owner(ResourceKind.SERVICE,
                        bundles.get(ResourceKind.SERVICE, owner(ResourceKind.DATASOURCE, event.bundle())));
            };
            Map<String, Object> provider = event.kind() == ResourceKind.PROVIDER
                    ? event.bundle()
                    : bundles.get(ResourceKind.PROVIDER, providerId);
            return Bundles.userEmails(Bundles.payload(provider, ResourceKind.PROVIDER));
        } catch (RuntimeException e) {
            logger.warn("Could not find the provider users of {} '{}'", event.kind().type(), Bundles.id(event.bundle()), e);
            return List.of();
        }
    }

    private static String owner(ResourceKind kind, Map<String, Object> bundle) {
        Object owner = Bundles.payload(bundle, kind).get(BundleService.ownerField(kind));
        return owner != null ? owner.toString() : null;
    }

    private static Object name(BundleEvent event) {
        Object name = Bundles.payload(event.bundle(), event.kind()).get("name");
        return name != null ? name : Bundles.id(event.bundle());
    }

    private static String comment(Map<String, Object> bundle) {
        if (bundle.get(Bundles.LOGGING_INFO) instanceof List<?> entries && !entries.isEmpty()
                && entries.getFirst() instanceof Map<?, ?> latest && latest.get("comment") != null) {
            return latest.get("comment").toString();
        }
        return "";
    }

    private void send(String to, String subject, String templateName, Map<String, Object> model) {
        if (to == null || to.isBlank()) {
            return;
        }
        String body;
        try (StringWriter out = new StringWriter()) {
            Template template = templates.getTemplate(templateName);
            template.process(model, out);
            body = out.toString();
        } catch (IOException | TemplateException e) {
            logger.error("Could not render mail template '{}'", templateName, e);
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!properties.getEmails().isEnabled() || sender == null) {
            logger.info("Mail not sent (emails disabled)\nTo: {}\nSubject: {}\n{}", to, subject, body);
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            if (!properties.getEmails().getFrom().isBlank()) {
                helper.setFrom(properties.getEmails().getFrom());
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            sender.send(message);
        } catch (MessagingException | RuntimeException e) {
            logger.error("Could not send mail '{}' to '{}'", subject, to, e);
        }
    }
}
