package eu.openaire.catalogue.config;

import gr.uoa.di.madgik.catalogue.service.ModelResourceTypeMapper;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.resourcecatalogue.config.ResourceCatalogueModelMapperConfig;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIREModelResourceTypeMapperTest {

    private static List<IndexField> fields(String name) {
        ResourceType type = new ResourceType();
        type.setName(name);
        type.setProperties(Map.of("class", ServiceBundle.class.getName()));
        return new OpenAIREModelResourceTypeMapper().additionalIndexFields(null, type);
    }

    @Test
    void isTheMapperInjectedAlongsideTheResourceCatalogueOne() {
        new ApplicationContextRunner()
                .withUserConfiguration(ResourceCatalogueModelMapperConfig.class, OpenAIREConfiguration.class)
                .run(context -> assertInstanceOf(OpenAIREModelResourceTypeMapper.class,
                        context.getBean(ModelResourceTypeMapper.class)));
    }

    @Test
    void addsAMultivaluedChannelsFieldToServices() {
        IndexField field = fields("service").stream().filter(f -> "channels".equals(f.getName())).findFirst().orElseThrow();
        assertTrue(field.isMultivalued());
        assertTrue(field.getPath().equals("$.properties.channels[*]"));
    }

    @Test
    void keepsTheBundleFields() {
        assertTrue(fields("service").stream().anyMatch(f -> "catalogue_id".equals(f.getName())));
    }

    @Test
    void leavesOtherTypesWithoutChannels() {
        assertFalse(fields("datasource").stream().anyMatch(f -> "channels".equals(f.getName())));
    }
}
