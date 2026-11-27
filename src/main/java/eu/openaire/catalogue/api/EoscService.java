package eu.openaire.catalogue.api;

import gr.uoa.di.madgik.resourcecatalogue.domain.Service;

/**
 * A lot1 service with its id repeated at the top level.
 */
public record EoscService(String id, Service service) {

    static EoscService of(Service service) {
        return new EoscService(service.getId(), service);
    }
}
