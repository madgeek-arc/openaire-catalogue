package gr.madgik.catalogue.openaire;

import gr.athenarc.catalogue.CatalogueApplication;
import gr.athenarc.catalogue.config.CatalogueLibConfiguration;
import gr.athenarc.catalogue.config.LibConfiguration;
import gr.athenarc.catalogue.config.RegistryCoreConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@org.springframework.context.annotation.Configuration
@ComponentScan(value = {"gr.athenarc.catalogue",
        "eu.openminted.registry.core",
        "gr.madgik.catalogue"
},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = CatalogueApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = RegistryCoreConfiguration.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = LibConfiguration.class)
        })
public class Configuration implements CatalogueLibConfiguration {

    @Override
    public String generatedClassesPackageName() {
        return "eu.einfracentral.domain";
    }
}
