package gr.madgik.catalogue.openaire.invitations;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "invitation")
public class InvitationProperties {

    private String hashAlgorithm = "SHA3-256";
    private JWS jws = new JWS();

    /**
     * Validity of a newly created invitation.
     */
    private Duration ttl = Duration.ofDays(30);

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public JWS getJws() {
        return jws;
    }

    public void setJws(JWS jws) {
        this.jws = jws;
    }

    public static class JWS {

        /**
         * HMAC secret used to sign invitation tokens. At least 32 characters.
         */
        private String secret;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}
