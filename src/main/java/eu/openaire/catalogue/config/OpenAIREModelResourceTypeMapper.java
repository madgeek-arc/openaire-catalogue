package eu.openaire.catalogue.config;

import eu.openaire.catalogue.api.ServiceChannels;
import gr.uoa.di.madgik.catalogue.domain.Model;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.resourcecatalogue.config.ResourceCatalogueModelResourceTypeMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds the index field of the service channels to the fields of the resource catalogue.
 */
public class OpenAIREModelResourceTypeMapper extends ResourceCatalogueModelResourceTypeMapper {

    @Override
    protected List<IndexField> additionalIndexFields(Model model, ResourceType resourceType) {
        List<IndexField> fields = new ArrayList<>(super.additionalIndexFields(model, resourceType));
        if ("service".equals(resourceType.getName())) {
            fields.add(additionalIndexField(resourceType, ServiceChannels.PROPERTY, null,
                    "$.properties." + ServiceChannels.PROPERTY + "[*]", String.class.getName(), true));
        }
        return fields;
    }
}
