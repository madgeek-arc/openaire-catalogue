package eu.openaire.catalogue.eosc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a stored organisation payload to the EOSC Beyond organisation fields. The {@code users} field holds personal
 * data and is never included.
 */
public final class EoscOrganisationMapper {

    private static final List<String> SHARED_FIELDS = List.of(
            "id", "name", "abbreviation", "nodePID", "website", "legalEntity", "legalStatus", "hostingLegalEntity",
            "description", "logo", "multimedia");

    private EoscOrganisationMapper() {
    }

    public static LinkedHashMap<String, Object> map(Map<String, Object> organisation) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        EoscFields.copy(organisation, result, SHARED_FIELDS);

        if (organisation.get("location") instanceof Map<?, ?> location) {
            EoscFields.put(result, "country", location.get("country"));
        }
        EoscFields.put(result, "alternativePIDs", EoscFields.alternativePids(organisation.get("alternativeIdentifiers")));
        EoscFields.put(result, "mainContact", EoscFields.mainContact(organisation.get("mainContact")));
        EoscFields.put(result, "publicContacts", EoscFields.publicContactEmails(organisation.get("publicContacts")));
        return result;
    }
}
