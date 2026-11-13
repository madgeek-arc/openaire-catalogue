package eu.openaire.catalogue.config;

import eu.openaire.catalogue.resource.ResourceKind;
import eu.openaire.catalogue.resource.Channels;
import gr.uoa.di.madgik.catalogue.domain.Model;
import gr.uoa.di.madgik.catalogue.service.ModelResourceTypeMapper;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps the OpenAIRE models to resource types whose records are read as JSON maps, and adds the index fields of the
 * bundle envelope (status, activity, metadata, logging dates and channels).
 */
public class OpenAIREModelResourceTypeMapper extends ModelResourceTypeMapper {

    @Override
    protected Map<String, String> customResourceTypeProperties(ResourceType resourceType) {
        if (isRecordType(resourceType)) {
            return Map.of("class", LinkedHashMap.class.getName());
        }
        return Map.of();
    }

    @Override
    protected List<IndexField> additionalIndexFields(Model model, ResourceType resourceType) {
        List<IndexField> fields = new ArrayList<>();
        if (!isRecordType(resourceType)) {
            return fields;
        }
        String string = String.class.getName();
        String bool = Boolean.class.getName();
        String date = Instant.class.getName();
        fields.add(additionalIndexField(resourceType, "status", null, "$.status", string, false));
        fields.add(additionalIndexField(resourceType, "active", null, "$.active", bool, false));
        fields.add(additionalIndexField(resourceType, "suspended", null, "$.suspended", bool, false));
        fields.add(additionalIndexField(resourceType, "registeredAt", null, "$.metadata.registeredAt", date, false));
        fields.add(additionalIndexField(resourceType, "modifiedAt", null, "$.metadata.modifiedAt", date, false));
        fields.add(additionalIndexField(resourceType, "latestOnboardingDate", null, "$.latestOnboardingInfo.date", date, false));
        fields.add(additionalIndexField(resourceType, "latestOnboardingActionType", null, "$.latestOnboardingInfo.actionType", string, false));
        fields.add(additionalIndexField(resourceType, "latestUpdateDate", null, "$.latestUpdateInfo.date", date, false));
        fields.add(additionalIndexField(resourceType, "latestUpdateActionType", null, "$.latestUpdateInfo.actionType", string, false));
        fields.add(additionalIndexField(resourceType, Channels.PROPERTY, null,
                "$.properties." + Channels.PROPERTY + "[*]", string, true));
        return fields;
    }

    private static boolean isRecordType(ResourceType resourceType) {
        return Arrays.stream(ResourceKind.values()).anyMatch(kind -> kind.type().equals(resourceType.getName()));
    }
}
