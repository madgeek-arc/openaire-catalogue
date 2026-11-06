package eu.openaire.catalogue.config;

import eu.openaire.catalogue.invitations.InvitationProperties;
import gr.uoa.di.madgik.catalogue.service.ModelResourceTypeMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableConfigurationProperties({OpenAIREProperties.class, InvitationProperties.class})
public class OpenAIREConfiguration {

    @Bean
    @Primary
    ModelResourceTypeMapper openAIREModelResourceTypeMapper() {
        return new OpenAIREModelResourceTypeMapper();
    }
}
