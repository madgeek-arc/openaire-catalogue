package eu.openaire.catalogue.invitations;

import eu.openaire.catalogue.config.OpenAIREProperties;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JWSInvitationServiceTest {

    private JWSInvitationService service;

    @BeforeEach
    void setUp() {
        OpenAIREProperties properties = new OpenAIREProperties();
        properties.getInvitations().getJws().setSecret("0123456789abcdef0123456789abcdef");
        service = new JWSInvitationService(properties);
    }

    @Test
    void inviteeCanVerifyTheInvitationRepeatedly() {
        String token = service.create("admin@example.org", "Invitee@Example.org");

        assertDoesNotThrow(() -> service.verify(token, " invitee@example.org "));
        assertDoesNotThrow(() -> service.verify(token, "invitee@example.org"));
    }

    @Test
    void otherUsersAreRejected() {
        String token = service.create("admin@example.org", "invitee@example.org");

        assertThrows(ResourceException.class, () -> service.verify(token, "someone@example.org"));
    }

    @Test
    void expiredInvitationIsRejected() {
        String token = service.create("admin@example.org", "invitee@example.org", Instant.now().minus(1, ChronoUnit.HOURS));

        assertThrows(ResourceException.class, () -> service.verify(token, "invitee@example.org"));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = service.create("admin@example.org", "invitee@example.org");
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "." + new StringBuilder(parts[2]).reverse();

        assertThrows(ResourceException.class, () -> service.verify(tampered, "invitee@example.org"));
    }
}
