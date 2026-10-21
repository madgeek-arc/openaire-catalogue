package eu.openaire.catalogue.mail;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import eu.openaire.catalogue.config.OpenAIREProperties;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.config.NodeProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.manager.AdapterManager;
import gr.uoa.di.madgik.resourcecatalogue.manager.DatasourceManager;
import gr.uoa.di.madgik.resourcecatalogue.manager.DeployableApplicationManager;
import gr.uoa.di.madgik.resourcecatalogue.manager.InteroperabilityRecordManager;
import gr.uoa.di.madgik.resourcecatalogue.manager.OrganisationManager;
import gr.uoa.di.madgik.resourcecatalogue.manager.ServiceManager;
import gr.uoa.di.madgik.resourcecatalogue.manager.TrainingResourceManager;
import gr.uoa.di.madgik.resourcecatalogue.service.EmailService;
import gr.uoa.di.madgik.resourcecatalogue.service.MailService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Replaces the default {@link EmailService}. Provider onboarding emails use the OpenAIRE portal wording
 * for their subjects; every other notification is inherited unchanged.
 */
@Component
public class OpenAIREEmailService extends EmailService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIREEmailService.class);
    private static final String NO_TEMPLATE_STATUS = "no template status";

    private final MailService mailService;
    private final Configuration cfg;
    private final SecurityService securityService;
    private final ServiceManager serviceManager;
    private final DatasourceManager datasourceManager;
    private final String projectName;
    private final String homepage;
    private final String registrationEmail;
    private final boolean providerNotifications;

    public OpenAIREEmailService(MailService mailService, Configuration cfg,
                                SecurityService securityService,
                                @Lazy OrganisationManager organisationManager,
                                @Lazy ServiceManager serviceManager,
                                @Lazy DatasourceManager datasourceManager,
                                @Lazy TrainingResourceManager trainingResourceManager,
                                @Lazy DeployableApplicationManager deployableApplicationManager,
                                @Lazy InteroperabilityRecordManager interoperabilityRecordManager,
                                @Lazy AdapterManager adapterManager,
                                CatalogueProperties properties,
                                NodeProperties nodeProperties,
                                OpenAIREProperties openAIREProperties) {
        super(mailService, cfg, securityService, organisationManager, serviceManager, datasourceManager,
                trainingResourceManager, deployableApplicationManager, interoperabilityRecordManager, adapterManager,
                properties, nodeProperties);
        this.mailService = mailService;
        this.cfg = cfg;
        this.securityService = securityService;
        this.serviceManager = serviceManager;
        this.datasourceManager = datasourceManager;
        this.projectName = openAIREProperties.getProjectName();
        this.homepage = properties.getHomepage();
        this.registrationEmail = properties.getEmails().getRegistrationEmails().getTo();
        this.providerNotifications = properties.getEmails().isProviderNotifications();
    }

    @Override
    @Async
    public void sendOnboardingEmailsToProviderAdmins(OrganisationBundle organisationBundle,
                                                     String afterReturningFrom) {
        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", homepage);
        root.put("registrationEmail", registrationEmail);
        root.put("bundle", organisationBundle);
        addResourceTemplate(root, organisationBundle, afterReturningFrom);

        Map<String, Object> teamRoot = new HashMap<>(root);
        teamRoot.put("user", registeredUser(organisationBundle));
        send("registrationTeamMailTemplate.ftl", teamRoot, teamSubject(organisationBundle, root), registrationEmail,
                true);

        Map<String, User> admins = new LinkedHashMap<>();
        for (User user : securityService.getOrganisationUsers(organisationBundle.getId())) {
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                admins.putIfAbsent(user.getEmail().toLowerCase(), user);
            }
        }
        String subject = providerSubject(organisationBundle, root);
        for (User user : admins.values()) {
            Map<String, Object> userRoot = new HashMap<>(root);
            userRoot.put("user", user);
            send("providerMailTemplate.ftl", userRoot, subject, user.getEmail().toLowerCase(), false);
        }
    }

    private void addResourceTemplate(Map<String, Object> root, OrganisationBundle organisation, String source) {
        if (source == null) {
            return;
        }
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(Integer.MAX_VALUE);
        filter.addFilter("published", false);
        Bundle template = switch (source) {
            case "serviceManager" -> first(serviceManager.getAllEOSCResourcesOfAProvider(
                    organisation.getId(), filter, securityService.getAdminAccess()).getResults());
            case "datasourceManager" -> first(datasourceManager.getAllEOSCResourcesOfAProvider(
                    organisation.getId(), filter, securityService.getAdminAccess()).getResults());
            default -> null;
        };
        if (template == null) {
            return;
        }
        root.put("resourceBundleId", template.getId());
        Object name = switch (template) {
            case ServiceBundle service -> service.getService().get("name");
            case DatasourceBundle datasource -> datasource.getDatasource().get("name");
            default -> null;
        };
        root.put("resourceBundleName", name != null ? name : template.getId());
    }

    private static <T extends Bundle> T first(List<T> results) {
        return results == null || results.isEmpty() ? null : results.getFirst();
    }

    private User registeredUser(OrganisationBundle organisation) {
        return organisation.getLoggingInfo().stream()
                .filter(info -> LoggingInfo.ActionType.REGISTERED.getKey().equals(info.getActionType()))
                .findFirst()
                .map(info -> {
                    User user = new User();
                    user.setEmail(info.getUserEmail() != null && !info.getUserEmail().isBlank()
                            ? info.getUserEmail().toLowerCase() : "no email provided");
                    String[] parts = info.getUserFullName() != null && !info.getUserFullName().isBlank()
                            ? info.getUserFullName().split(" ") : new String[]{"Unknown"};
                    user.setName(parts[0]);
                    user.setSurname(parts.length > 1 ? parts[1] : "Unknown");
                    return user;
                })
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail("no email provided");
                    user.setName("Unknown");
                    user.setSurname("Unknown");
                    return user;
                });
    }

    private String providerSubject(OrganisationBundle organisation, Map<String, Object> root) {
        String name = organisationName(organisation);
        String templateStatus = organisation.getTemplateStatus();
        if (templateStatus == null || NO_TEMPLATE_STATUS.equals(templateStatus)) {
            return switch (String.valueOf(organisation.getStatus())) {
                case "pending" -> "[%1$s Portal] Your application for registering [%2$s] as a new %1$s Provider to the %1$s Portal has been received and is under review"
                        .formatted(projectName, name);
                case "rejected" -> "[%1$s Portal] Your application for registering [%2$s] as a new %1$s Provider to the %1$s Portal has been rejected"
                        .formatted(projectName, name);
                case "approved" -> "[%1$s Portal] Your application for registering [%2$s] as a new %1$s Provider to the %1$s Portal has been approved"
                        .formatted(projectName, name);
                default -> "[%s Portal] Provider Registration".formatted(projectName);
            };
        }
        String resource = String.valueOf(root.getOrDefault("resourceBundleName", name));
        if (templateStatus.contains("pending")) {
            return "[%1$s Portal] Your application for registering [%2$s] as a new Resource to the %1$s Portal has been received and is under review"
                    .formatted(projectName, resource);
        } else if (templateStatus.contains("approved")) {
            return organisation.isActive()
                    ? "[%1$s Portal] Your application for registering [%2$s] as a new Resource to the %1$s Portal has been approved"
                    .formatted(projectName, resource)
                    : "[%s Portal] Your Resource Provider [%s] has been set to inactive".formatted(projectName, name);
        } else if (templateStatus.contains("rejected")) {
            return "[%1$s Portal] Your application for registering [%2$s] as a new Resource to the %1$s Portal has been rejected"
                    .formatted(projectName, resource);
        }
        return "[%s Portal] Provider Registration".formatted(projectName);
    }

    private String teamSubject(OrganisationBundle organisation, Map<String, Object> root) {
        String name = organisationName(organisation);
        String id = organisation.getId();
        String templateStatus = organisation.getTemplateStatus();
        if (templateStatus == null || NO_TEMPLATE_STATUS.equals(templateStatus)) {
            return switch (String.valueOf(organisation.getStatus())) {
                case "pending" -> "[%1$s Portal] A new application for registering [%2$s] - ([%3$s]) as a new %1$s Provider to the %1$s Portal has been received and should be reviewed"
                        .formatted(projectName, name, id);
                case "approved" -> "[%1$s Portal] The application of [%2$s] - ([%3$s]) for registering as a new %1$s Provider has been approved"
                        .formatted(projectName, name, id);
                case "rejected" -> "[%1$s Portal] The application of [%2$s] - ([%3$s]) for registering as a new %1$s Provider has been rejected"
                        .formatted(projectName, name, id);
                default -> "[%s Portal] Provider Registration".formatted(projectName);
            };
        }
        String resource = String.valueOf(root.getOrDefault("resourceBundleName", name));
        String resourceId = String.valueOf(root.getOrDefault("resourceBundleId", id));
        if (templateStatus.contains("pending")) {
            return "[%1$s Portal] A new application for registering [%2$s] as a new Resource to the %1$s Portal has been received and should be reviewed"
                    .formatted(projectName, resourceId);
        } else if (templateStatus.contains("approved")) {
            return organisation.isActive()
                    ? "[%s Portal] The application of [%s] - ([%s]) for registering as a new Resource has been approved"
                    .formatted(projectName, resource, resourceId)
                    : "[%s Portal] The Resource Provider [%s] has been set to inactive".formatted(projectName, name);
        } else if (templateStatus.contains("rejected")) {
            return "[%s Portal] The application of [%s] - ([%s]) for registering as a %s Resource has been rejected"
                    .formatted(projectName, resource, resourceId, projectName);
        }
        return "[%s Portal] Provider Registration".formatted(projectName);
    }

    private static String organisationName(OrganisationBundle organisation) {
        Object name = organisation.getOrganisation().get("name");
        return name != null ? name.toString() : organisation.getId();
    }

    private void send(String templateName, Map<String, Object> root, String subject, String to,
                      boolean onboardingTeam) {
        if (to == null || to.isBlank()) {
            logger.error("Recipient is empty for template '{}'", templateName);
            return;
        }
        try (StringWriter out = new StringWriter()) {
            Template template = cfg.getTemplate(templateName);
            template.process(root, out);
            String body = out.toString();
            if (onboardingTeam || providerNotifications) {
                mailService.sendMail(List.of(to), subject, body);
            }
            logger.info("\nRecipients: {}\nTitle: {}\nMail body: \n{}", String.join(", ", new ArrayList<>(List.of(to))),
                    subject, body);
        } catch (IOException e) {
            logger.error("Error finding mail template '{}'", templateName, e);
        } catch (TemplateException e) {
            logger.error("Error processing mail template '{}'", templateName, e);
        } catch (MessagingException e) {
            logger.error("Could not send mail", e);
        }
    }
}
