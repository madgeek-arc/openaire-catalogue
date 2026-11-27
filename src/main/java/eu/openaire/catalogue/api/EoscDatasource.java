package eu.openaire.catalogue.api;

import gr.uoa.di.madgik.resourcecatalogue.domain.Datasource;

/**
 * A lot1 datasource with its id repeated at the top level.
 */
public record EoscDatasource(String id, Datasource datasource) {

    static EoscDatasource of(Datasource datasource) {
        return new EoscDatasource(datasource.getId(), datasource);
    }
}
