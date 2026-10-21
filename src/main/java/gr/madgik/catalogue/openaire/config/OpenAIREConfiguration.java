package gr.madgik.catalogue.openaire.config;

import gr.madgik.catalogue.openaire.invitations.InvitationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({OpenAIREProperties.class, InvitationProperties.class})
public class OpenAIREConfiguration {
}
