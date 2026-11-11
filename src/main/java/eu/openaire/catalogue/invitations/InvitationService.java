package eu.openaire.catalogue.invitations;

import java.time.Instant;

public interface InvitationService {

    /**
     * Creates an invitation that expires after the configured time to live.
     *
     * @return the signed invitation token
     */
    String create(String inviterEmail, String inviteeEmail);

    /**
     * @return the signed invitation token
     */
    String create(String inviterEmail, String inviteeEmail, Instant expiration);

    /**
     * Checks that the token is authentic, not expired and issued for the given invitee. Invitations are not
     * stored, so a valid token verifies repeatedly until it expires.
     *
     * @throws gr.uoa.di.madgik.registry.exception.ResourceException when the token is not valid
     */
    void verify(String invitationToken, String inviteeEmail);
}
