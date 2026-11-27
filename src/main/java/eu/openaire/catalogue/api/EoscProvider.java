package eu.openaire.catalogue.api;

import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;

/**
 * A lot1 provider with its id repeated at the top level.
 */
public record EoscProvider(String id, Provider provider) {

    static EoscProvider of(Provider provider) {
        return new EoscProvider(provider.getId(), provider);
    }
}
