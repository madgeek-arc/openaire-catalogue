package gr.madgik.catalogue.openaire.invitations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "invitation")
public class InvitationProperties {

    String hashAlgorithm = "SHA3-256";
    JWS jws;

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public JWS getJws() {
        return jws;
    }

    public void setJws(JWS jws) {
        this.jws = jws;
    }

    public static class JWS {

        String secret;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}
