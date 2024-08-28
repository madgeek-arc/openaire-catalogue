package gr.madgik.catalogue.openaire.repository;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;


public interface Repository<T, ID> extends PagingAndSortingRepository<T, ID> {

    T create(T resource);

    T update(ID id, T resource);

    void deleteById(ID id);

    T get(ID id);

    Paging<T> get(FacetFilter filter);

    default Page<T> get(Pageable filter) {
        throw new UnsupportedOperationException();
    }
}
