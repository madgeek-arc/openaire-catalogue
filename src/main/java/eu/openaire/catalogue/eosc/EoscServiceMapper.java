package eu.openaire.catalogue.eosc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a stored OpenAIRE service payload to the EOSC Beyond service fields.
 * <p>
 * {@code type} and {@code jurisdiction} have no stored source and are left out.
 */
public final class EoscServiceMapper {

    private static final List<String> SHARED_FIELDS = List.of(
            "id", "name", "nodePID", "description", "webpage", "logo", "scientificDomains", "categories", "tags",
            "trl", "termsOfUse", "privacyPolicy", "accessPolicy", "orderType", "order");

    private EoscServiceMapper() {
    }

    public static LinkedHashMap<String, Object> map(Map<String, Object> service) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        EoscFields.copy(service, result, SHARED_FIELDS);

        Object owner = service.get("resourceOwner");
        EoscFields.put(result, "resourceOwner", owner != null ? owner : service.get("resourceOrganisation"));
        EoscFields.put(result, "serviceProviders", service.get("resourceProviders"));
        EoscFields.put(result, "alternativePIDs", EoscFields.alternativePids(service.get("alternativeIdentifiers")));
        EoscFields.put(result, "publishingDate", service.get("lastUpdate"));
        EoscFields.put(result, "accessTypes", firstValue(service.get("accessTypes")));
        EoscFields.put(result, "mainContact", EoscFields.mainContact(service.get("mainContact")));
        EoscFields.put(result, "publicContacts", EoscFields.publicContactEmails(service.get("publicContacts")));
        if (service.get("webpage") != null) {
            result.put("urls", List.of(service.get("webpage")));
        }
        return result;
    }

    /** Beyond holds a single access type, while OpenAIRE stores a list. */
    private static Object firstValue(Object value) {
        if (value instanceof List<?> list) {
            return list.isEmpty() ? null : list.getFirst();
        }
        return value;
    }
}
