package eu.openaire.catalogue.invitations;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.util.Base64URL;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Invitations as HMAC-signed JWS tokens. The payload carries the invitation id, inviter, a hash of the
 * invitee email and the expiration (epoch milliseconds). Nothing is stored.
 */
@Service
public class JWSInvitationService implements InvitationService {

    private static final Logger logger = LoggerFactory.getLogger(JWSInvitationService.class);

    private final InvitationProperties properties;

    private volatile JWSSigner signer;
    private volatile JWSVerifier verifier;

    public JWSInvitationService(InvitationProperties properties) {
        this.properties = properties;
    }

    @Override
    public String create(String inviterEmail, String inviteeEmail) {
        return create(inviterEmail, inviteeEmail, Instant.now().plus(properties.getTtl()));
    }

    @Override
    public String create(String inviterEmail, String inviteeEmail, Instant expiration) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("id", UUID.randomUUID().toString());
        claims.put("inviterEmail", inviterEmail);
        claims.put("inviteeHash", Base64.getEncoder().encodeToString(hashEmail(inviteeEmail)));
        claims.put("expiration", expiration.toEpochMilli());

        try {
            JWSObject jws = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(claims));
            jws.sign(signer());
            return jws.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not sign invitation", e);
        }
    }

    @Override
    public void verify(String invitationToken, String inviteeEmail) {
        if (invitationToken == null) {
            throw new ResourceException("Invitation token is missing.", HttpStatus.BAD_REQUEST);
        }
        String[] parts = invitationToken.split("\\.");
        if (parts.length != 3) {
            throw new ResourceException("Invalid Token", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> claims;
        try {
            JWSObject jws = new JWSObject(new Base64URL(parts[0]), new Base64URL(parts[1]), new Base64URL(parts[2]));
            if (!jws.verify(verifier())) {
                throw new ResourceException("Token has been modified.", HttpStatus.FORBIDDEN);
            }
            claims = jws.getPayload().toJSONObject();
        } catch (ParseException | JOSEException e) {
            logger.error("Could not verify invitation token", e);
            throw new ResourceException("Invalid Token", HttpStatus.BAD_REQUEST);
        }
        if (claims == null || claims.get("inviteeHash") == null || claims.get("expiration") == null) {
            throw new ResourceException("Invalid Token", HttpStatus.BAD_REQUEST);
        }

        Instant expiration;
        byte[] inviteeHash;
        try {
            inviteeHash = Base64.getDecoder().decode(claims.get("inviteeHash").toString());
            expiration = Instant.ofEpochMilli(((Number) claims.get("expiration")).longValue());
        } catch (IllegalArgumentException | ClassCastException e) {
            throw new ResourceException("Invalid Token", HttpStatus.BAD_REQUEST);
        }
        if (Instant.now().isAfter(expiration)) {
            throw new ResourceException("Invitation has expired.", HttpStatus.FORBIDDEN);
        }

        if (inviteeEmail == null || !MessageDigest.isEqual(inviteeHash, hashEmail(inviteeEmail))) {
            logger.error("Invitee email does not match the invitation");
            throw new ResourceException("Invitation was not issued for this user.", HttpStatus.FORBIDDEN);
        }
    }

    private JWSSigner signer() {
        if (signer == null) {
            synchronized (this) {
                if (signer == null) {
                    initCrypto();
                }
            }
        }
        return signer;
    }

    private JWSVerifier verifier() {
        signer();
        return verifier;
    }

    private void initCrypto() {
        String secret = properties.getJws().getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Property 'invitation.jws.secret' is not set");
        }
        try {
            this.verifier = new MACVerifier(secret);
            this.signer = new MACSigner(secret);
        } catch (JOSEException e) {
            throw new IllegalStateException("Invalid 'invitation.jws.secret'", e);
        }
    }

    private byte[] hashEmail(String email) {
        try {
            return MessageDigest.getInstance(properties.getHashAlgorithm())
                    .digest(email.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unsupported hash algorithm " + properties.getHashAlgorithm(), e);
        }
    }
}
