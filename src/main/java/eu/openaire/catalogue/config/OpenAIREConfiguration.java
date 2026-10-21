package eu.openaire.catalogue.config;

import eu.openaire.catalogue.invitations.InvitationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({OpenAIREProperties.class, InvitationProperties.class})
public class OpenAIREConfiguration {
}
