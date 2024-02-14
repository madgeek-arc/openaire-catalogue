package gr.madgik.catalogue.openaire.service.repository;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.repository.RegistryCoreRepository;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
public class ServiceRepository extends RegistryCoreRepository<ServiceBundle, String> implements ResourceRepository<ServiceBundle, String> {


    public ServiceRepository(GenericItemService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "service";
    }

    @Override
    public ServiceBundle get(@NotNull String s, String catalogue) {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("resource_internal_id", s);
        if (catalogue != null) {
            filter.addFilter("catalogue_id", catalogue);
        }
        Paging<ServiceBundle> results = get(filter);
        if (results.getResults() != null && !results.getResults().isEmpty()) {
            return results.getResults().get(0);
        }
        return null;
    }
}
