package gr.madgik.catalogue.openaire.invitations;

import com.fasterxml.jackson.core.JsonProcessingException;
import gr.madgik.catalogue.domain.User;

import java.util.Date;

public interface InvitationService {

    String create(User inviter, String inviteeEmail);

    String create(User inviter, String inviteeEmail, Date expiration) throws JsonProcessingException;

    boolean accept(String invitation, String inviteeEmail);

}
