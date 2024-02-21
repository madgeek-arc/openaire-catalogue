package gr.madgik.catalogue.openaire.invitations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.util.Base64URL;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.catalogue.exception.ResourceException;
import gr.madgik.catalogue.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

@Service
public class JWSInvitationService implements InvitationService {

    private static final Logger logger = LoggerFactory.getLogger(JWSInvitationService.class);

    private final JWSSigner signer;
    private final JWSVerifier verifier;
    private final MessageDigest messageDigest;
    private final InvitationRepository invitationRepository;
    private final ObjectMapper objectMapper;


    public JWSInvitationService(InvitationProperties invitationProperties, InvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
        this.objectMapper = new ObjectMapper();

        try {
            // Create HMAC signer/verifier
            this.signer = new MACSigner(invitationProperties.getJws().getSecret());
            this.verifier = new MACVerifier(invitationProperties.getJws().getSecret());

            // Create MessageDigest
            this.messageDigest = MessageDigest.getInstance(invitationProperties.getHashAlgorithm());
        } catch (JOSEException | NoSuchAlgorithmException e) {
            throw new ServiceException(e);
        }
    }

    @Override
    public String create(User inviter, String inviteeEmail) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
        return create(inviter, inviteeEmail, calendar.getTime());
    }

    @Override
    public String create(User inviter, String inviteeEmail, Date expiration) {
        Invitation invitation = new Invitation(inviter.getEmail(), hashEmail(inviteeEmail), expiration);
        invitation = invitationRepository.save(invitation);

        // Create an HMAC-protected JWS object with the invitation as payload
        JWSObject jwsObject = null;

        try {
            jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(convertToJsonString(invitation)));

            // Apply the HMAC to the JWS object
            jwsObject.sign(signer);
        } catch (JOSEException e) {
            logger.error(e.getMessage(), e);
        }

        return jwsObject.serialize();
    }

    @Override
    public boolean accept(String invitationToken, String inviteeEmail) {
        String[] parts = invitationToken.split("\\.");
        if (parts.length != 3) {
            throw new ResourceException("Invalid Token", HttpStatus.BAD_REQUEST);
        }

        Invitation invitation;
        try {
            JWSObject jwsObject = new JWSObject(new Base64URL(parts[0]), new Base64URL(parts[1]), new Base64URL(parts[2]));
            if (!jwsObject.verify(verifier)) {
                throw new ResourceException("Token has been modified.", HttpStatus.FORBIDDEN);
            }
            invitation = reconstructInvitation(jwsObject.getPayload().toJSONObject());

            if (new Date().after(invitation.getExpiration())) {
                throw new ResourceException("Invitation has expired.", HttpStatus.FORBIDDEN);
            }

        } catch (ParseException | JOSEException e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        Invitation dbInvitation = invitationRepository.findById(invitation.getId()).orElse(new Invitation());
        if (dbInvitation.getId() == null) {
            logger.error("Invitation does not exist in the DB. It has probably already been used.");
            return false;
        } else {
            if (!invitation.equals(dbInvitation) || !Arrays.equals(invitation.getInviteeHash(), hashEmail(inviteeEmail))) {
                logger.error("Invitee email does not match invitation");
                return false;
            }
        }

        invitationRepository.deleteById(invitation.getId());
        return true;
    }

    private String convertToJsonString(Invitation invitation) {
        try {
            return objectMapper.writeValueAsString(invitation);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Invitation reconstructInvitation(Object jsonObject) {
        return objectMapper.convertValue(jsonObject, Invitation.class);
    }

    private byte[] hashEmail(String email) {
        email = email.trim().toLowerCase();
        return messageDigest.digest(email.getBytes());
    }
}
