package eu.openaire.catalogue.eosc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Conversions shared by the EOSC Beyond mappers.
 */
final class EoscFields {

    private static final List<String> MAIN_CONTACT_FIELDS = List.of("firstName", "lastName", "email");

    private EoscFields() {
    }

    static void copy(Map<String, Object> source, Map<String, Object> target, List<String> fields) {
        fields.forEach(field -> put(target, field, source.get(field)));
    }

    /** Sets the value unless it is {@code null}, so that fields without data are left out of the output. */
    static void put(Map<String, Object> target, String field, Object value) {
        if (value != null) {
            target.put(field, value);
        }
    }

    static Map<String, Object> mainContact(Object contact) {
        if (!(contact instanceof Map<?, ?> source)) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        MAIN_CONTACT_FIELDS.forEach(field -> put(result, field, source.get(field)));
        return result;
    }

    /** Beyond public contacts are plain email addresses. */
    static List<Object> publicContactEmails(Object contacts) {
        if (!(contacts instanceof List<?> source)) {
            return null;
        }
        return source.stream()
                .map(contact -> contact instanceof Map<?, ?> map ? map.get("email") : contact)
                .filter(Objects::nonNull)
                .map(Object.class::cast)
                .toList();
    }

    static List<Map<String, Object>> alternativePids(Object identifiers) {
        if (!(identifiers instanceof List<?> source)) {
            return null;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object identifier : source) {
            if (identifier instanceof Map<?, ?> map) {
                Map<String, Object> pid = new LinkedHashMap<>();
                put(pid, "pid", map.get("value"));
                put(pid, "pidSchema", map.get("type"));
                result.add(pid);
            }
        }
        return result;
    }
}
