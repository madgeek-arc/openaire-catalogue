package gr.madgik.catalogue.eosc.resource.repository;

import eu.einfracentral.domain.ServiceBundle;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.madgik.catalogue.repository.RegistryCoreRepository;
import org.springframework.stereotype.Component;

@Component
public class ServiceRepository extends RegistryCoreRepository<ServiceBundle> implements ResourceRepository<ServiceBundle, String> {


    public ServiceRepository(GenericItemService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "service";
    }

    @Override
    public ServiceBundle get(String s, String catalogue) {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("service_id", s);
        filter.addFilter("catalogue_id", catalogue);
        Paging<ServiceBundle> results = get(filter);
        if (results.getResults() != null && !results.getResults().isEmpty()) {
            return results.getResults().get(0);
        }
        return null;
    }
}
