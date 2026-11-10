package eu.openaire.catalogue.resource;

import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;

import java.util.Map;

/**
 * A stored change to a record, published after the write succeeds.
 *
 * @param bundle the bundle as stored, or as it was before a deletion
 */
public record BundleEvent(ResourceKind kind, Action action, Map<String, Object> bundle, UserInfo user) {

    public enum Action {
        CREATED, UPDATED,
        /**
         * A rejected record was updated, and is pending again.
         */
        RESUBMITTED,
        APPROVED, REJECTED, ACTIVATED, DEACTIVATED, DELETED
    }
}
