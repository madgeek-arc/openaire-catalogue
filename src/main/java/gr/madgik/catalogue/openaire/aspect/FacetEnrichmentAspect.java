package gr.madgik.catalogue.openaire.aspect;

import gr.uoa.di.madgik.registry.domain.Paging;
import gr.madgik.catalogue.openaire.FacetLabelService;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FacetEnrichmentAspect {

    private final FacetLabelService facetLabelService;

    public FacetEnrichmentAspect(FacetLabelService facetLabelService) {
        this.facetLabelService = facetLabelService;
    }

    @AfterReturning(pointcut = "within(gr.madgik.catalogue.openaire.BundleResourceOperations+) && execution(* getWithEnrichedFacets(gr.uoa.di.madgik.registry.domain.FacetFilter) )",
            returning = "paging")
    public void enrichPagingFacets(Paging<?> paging) {
        paging.setFacets(facetLabelService.createLabels(paging.getFacets()));
    }
}
