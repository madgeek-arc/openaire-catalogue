package eu.openaire.catalogue.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes the envelope of a bundle kept as a JSON map: the payload sits under the key of its resource type,
 * and the envelope fields (status, activity, logging, metadata, properties) sit beside it.
 */
public final class Bundles {

    public static final String ID = "id";
    public static final String STATUS = "status";
    public static final String ACTIVE = "active";
    public static final String SUSPENDED = "suspended";
    public static final String CATALOGUE_ID = "catalogueId";
    public static final String METADATA = "metadata";
    public static final String LOGGING_INFO = "loggingInfo";
    public static final String LATEST_ONBOARDING_INFO = "latestOnboardingInfo";
    public static final String LATEST_UPDATE_INFO = "latestUpdateInfo";
    public static final String PROPERTIES = "properties";

    // Record statuses.
    public static final String PENDING = "pending";
    public static final String APPROVED = "approved";
    public static final String REJECTED = "rejected";

    private Bundles() {
    }

    public static String id(Map<String, Object> bundle) {
        Object id = bundle.get(ID);
        return id != null ? id.toString() : null;
    }

    public static String status(Map<String, Object> bundle) {
        Object status = bundle.get(STATUS);
        return status != null ? status.toString() : null;
    }

    public static boolean active(Map<String, Object> bundle) {
        return Boolean.TRUE.equals(bundle.get(ACTIVE));
    }

    /**
     * @return the {@code modifiedAt} of the bundle metadata, which changes on every stored write; {@code null} if
     * missing
     */
    public static String version(Map<String, Object> bundle) {
        if (bundle.get(METADATA) instanceof Map<?, ?> metadata && metadata.get("modifiedAt") != null) {
            return metadata.get("modifiedAt").toString();
        }
        return null;
    }

    /**
     * Removes an empty {@code extras} object from a payload.
     */
    public static void dropEmptyExtras(Map<String, Object> payload) {
        if (payload.get("extras") instanceof Map<?, ?> extras && extras.isEmpty()) {
            payload.remove("extras");
        }
    }

    /**
     * @return the payload, an empty map if the bundle has none
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> payload(Map<String, Object> bundle, ResourceKind kind) {
        if (bundle.get(kind.type()) instanceof Map<?, ?> payload) {
            return (Map<String, Object>) payload;
        }
        return new LinkedHashMap<>();
    }

    /**
     * @return the properties of the bundle, created and attached if missing
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> properties(Map<String, Object> bundle) {
        if (bundle.get(PROPERTIES) instanceof Map<?, ?> properties) {
            return (Map<String, Object>) properties;
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        bundle.put(PROPERTIES, properties);
        return properties;
    }

    /**
     * @return the metadata of the bundle, created and attached if missing
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> metadata(Map<String, Object> bundle) {
        if (bundle.get(METADATA) instanceof Map<?, ?> metadata) {
            return (Map<String, Object>) metadata;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        bundle.put(METADATA, metadata);
        return metadata;
    }

    /**
     * Adds a logging entry, newest first, and records it as the latest onboarding or update entry by its type.
     */
    @SuppressWarnings("unchecked")
    public static void log(Map<String, Object> bundle, Map<String, Object> entry) {
        List<Object> entries = bundle.get(LOGGING_INFO) instanceof List<?> existing
                ? new ArrayList<>((List<Object>) existing)
                : new ArrayList<>();
        entries.addFirst(entry);
        bundle.put(LOGGING_INFO, entries);
        if ("onboard".equals(entry.get("type"))) {
            bundle.put(LATEST_ONBOARDING_INFO, entry);
        } else {
            bundle.put(LATEST_UPDATE_INFO, entry);
        }
    }

    /**
     * Trims and lower-cases the emails of the provider users, so that they match the signed-in user.
     */
    @SuppressWarnings("unchecked")
    public static void normaliseUserEmails(Map<String, Object> provider) {
        if (provider.get("users") instanceof List<?> users) {
            for (Object user : users) {
                if (user instanceof Map<?, ?> fields && fields.get("email") instanceof String email) {
                    ((Map<String, Object>) fields).put("email", email.trim().toLowerCase());
                }
            }
        }
    }

    /**
     * @return the emails of the provider users, lower-cased
     */
    public static List<String> userEmails(Map<String, Object> provider) {
        List<String> emails = new ArrayList<>();
        if (provider.get("users") instanceof List<?> users) {
            for (Object user : users) {
                if (user instanceof Map<?, ?> fields && fields.get("email") instanceof String email && !email.isBlank()) {
                    emails.add(email.trim().toLowerCase());
                }
            }
        }
        return emails;
    }
}
