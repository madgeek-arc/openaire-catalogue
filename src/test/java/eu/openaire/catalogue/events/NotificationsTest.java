package eu.openaire.catalogue.events;

import eu.openaire.catalogue.config.OpenAIREProperties;
import eu.openaire.catalogue.resource.BundleEvent;
import eu.openaire.catalogue.resource.BundleEvent.Action;
import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.ResourceKind;
import freemarker.template.Configuration;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationsTest {

    private static final UserInfo OWNER = new UserInfo("sub", "owner@example.org", "Owner", "User",
            List.of("ROLE_USER"));

    private final JavaMailSender sender = mock(JavaMailSender.class);
    private Notifications notifications;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        OpenAIREProperties properties = new OpenAIREProperties();
        properties.getEmails().setEnabled(true);
        properties.getEmails().setRegistrationTo("team@example.org");
        Configuration templates = new Configuration(Configuration.VERSION_2_3_34);
        templates.setClassForTemplateLoading(getClass(), "/templates");
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        when(sender.createMimeMessage()).thenAnswer(call -> new MimeMessage(Session.getInstance(new Properties())));
        notifications = new Notifications(properties, mock(BundleService.class), templates, provider);
    }

    @Test
    void aResubmissionIsSentToTheRegistrationTeamOnly() throws Exception {
        notifications.notify(new BundleEvent(ResourceKind.PROVIDER, Action.RESUBMITTED, provider(), OWNER));

        MimeMessage message = sent(1).getFirst();
        assertEquals("team@example.org", message.getRecipients(Message.RecipientType.TO)[0].toString());
        assertTrue(message.getSubject().contains("has been updated and should be reviewed again"),
                message.getSubject());
        assertTrue(message.getContent().toString().contains("should be reviewed again"));
    }

    @Test
    void aRejectionIsSentToTheTeamAndTheProviderUsers() throws Exception {
        notifications.notify(new BundleEvent(ResourceKind.PROVIDER, Action.REJECTED, provider(), OWNER));

        List<MimeMessage> messages = sent(2);
        assertEquals("team@example.org", messages.get(0).getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("owner@example.org", messages.get(1).getRecipients(Message.RecipientType.TO)[0].toString());
    }

    @Test
    void anOrdinaryUpdateSendsNothing() {
        notifications.notify(new BundleEvent(ResourceKind.PROVIDER, Action.UPDATED, provider(), OWNER));

        sent(0);
    }

    private static Map<String, Object> provider() {
        return Map.of("id", "openaire", "status", "pending",
                "provider", Map.of("id", "openaire", "name", "OpenAIRE",
                        "users", List.of(Map.of("email", "owner@example.org"))));
    }

    private List<MimeMessage> sent(int count) {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, times(count)).send(captor.capture());
        return captor.getAllValues();
    }
}
