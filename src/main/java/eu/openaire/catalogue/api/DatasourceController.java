package eu.openaire.catalogue.api;

import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.ResourceKind;
import eu.openaire.catalogue.security.AccessRules;

/**
 * Datasource routes of one channel. A datasource holds the datasource fields of a service, and has the id of that
 * service.
 */
public abstract class DatasourceController extends RecordController {

    protected DatasourceController(String channel, BundleService bundles, AccessRules access) {
        super(ResourceKind.DATASOURCE, channel, bundles, access);
    }
}
