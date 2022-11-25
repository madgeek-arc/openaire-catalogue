package gr.madgik.catalogue.openaire.resource.repository;

import eu.einfracentral.domain.DatasourceBundle;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.madgik.catalogue.repository.RegistryCoreRepository;
import org.springframework.stereotype.Component;

@Component
public class DatasourceRepository extends RegistryCoreRepository<DatasourceBundle, String> implements ResourceRepository<DatasourceBundle, String> {


    public DatasourceRepository(GenericItemService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "datasource";
    }

    @Override
    public DatasourceBundle get(String s, String catalogue) {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("datasource_id", s);
        filter.addFilter("catalogue_id", catalogue);
        Paging<DatasourceBundle> results = get(filter);
        if (results.getResults() != null && !results.getResults().isEmpty()) {
            return results.getResults().get(0);
        }
        return null;
    }
}
