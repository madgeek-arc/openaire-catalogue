package gr.madgik.catalogue.openaire;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.madgik.catalogue.openaire.dto.BulkOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ResourceOperations<T, ID> {

    List<T> get(ID... ids);

    boolean validate(T t);

    T create(T resource);

    T update(ID id, T resource);

    void delete(ID id);

    T get(ID id);

    Paging<T> get(FacetFilter filter);

    default Page<T> get(Pageable filter) {
        throw new UnsupportedOperationException();
    }

    BulkOperation<T> bulkAdd(List<T> resources);

    BulkOperation<T> bulkUpdate(List<T> resources);
}
