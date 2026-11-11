package eu.openaire.catalogue.resource;

import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.Locale;
import java.util.Map;

/**
 * Ids of new records, in the legacy pattern: a provider is the slug of its abbreviation (or name), a service is
 * {@code <provider id>.<slug of its name>}, and a datasource takes the id of the service it describes.
 */
public final class BundleIds {

    private BundleIds() {
    }

    /**
     * @throws ResourceException if a field the id is built from is missing
     */
    public static String create(ResourceKind kind, Map<String, Object> payload) {
        return switch (kind) {
            case PROVIDER -> slug(firstNonBlank(payload, "abbreviation", "name"));
            case SERVICE -> required(payload, "resourceOrganisation") + "." + slug(required(payload, "name"));
            case DATASOURCE -> required(payload, "serviceId");
        };
    }

    static String slug(String value) {
        String slug = StringUtils.stripAccents(value)
                .replaceAll("\\s+", " ")
                .trim()
                .replaceAll("[^a-zA-Z0-9\\s\\-_]+", "")
                .replace(" ", "_")
                .toLowerCase(Locale.ROOT);
        if (slug.isEmpty()) {
            throw new ResourceException("Cannot build an id from '" + value + "'", HttpStatus.BAD_REQUEST);
        }
        return slug;
    }

    private static String firstNonBlank(Map<String, Object> payload, String... fields) {
        for (String field : fields) {
            if (payload.get(field) instanceof String value && !value.isBlank()) {
                return value;
            }
        }
        throw new ResourceException("Field '" + fields[0] + "' is required", HttpStatus.BAD_REQUEST);
    }

    private static String required(Map<String, Object> payload, String field) {
        return firstNonBlank(payload, field);
    }
}
