package gr.madgik.catalogue.openaire;


import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;

import java.util.List;

public interface BundleResourceOperations<T extends Identifiable, B extends Bundle<T>, ID> {

    boolean validate(Object resource);

    T register(T resource);

    B create(B resource);

    B update(ID id, B resource);

    void delete(ID id);

    T get(ID id);

    B getBundle(ID id);

    Paging<B> getWithEnrichedFacets(FacetFilter filter);

    Paging<B> get(FacetFilter filter);

    List<T> getByIds(ID... ids);

    B verify(ID id, String status, Boolean active);

    B activate(ID id, Boolean active);
}
