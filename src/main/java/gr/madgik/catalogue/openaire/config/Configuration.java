package gr.madgik.catalogue.openaire.config;

import org.springframework.context.annotation.ComponentScan;

@org.springframework.context.annotation.Configuration
@ComponentScan(value = {"gr.athenarc.catalogue",
        "gr.uoa.di.madgik.registry",
        "gr.madgik.catalogue"
})
public class Configuration {
}
