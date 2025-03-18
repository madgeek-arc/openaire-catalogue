package gr.madgik.catalogue.openaire.repository;

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.catalogue.utils.ReflectUtils;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class RegistryCoreRepository<T, ID extends String> implements Repository<T, ID> {

    private static final Logger logger = LoggerFactory.getLogger(RegistryCoreRepository.class);
    protected final GenericResourceService itemService;

    public abstract String getResourceTypeName();

    public RegistryCoreRepository(GenericResourceService itemService) {
        this.itemService = itemService;
    }

    @Override
    public T create(T resource) {
        try {
            String id = ReflectUtils.getId(resource.getClass(), resource);
            itemService.get(getResourceTypeName(), id);
            throw new ResourceAlreadyExistsException();
        } catch (NoSuchMethodException | InvocationTargetException | NoSuchFieldException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (ResourceNotFoundException e) {
            // skip
        }
        return itemService.add(getResourceTypeName(), resource);
    }

    @Override
    public T update(ID id, T resource) {
        try {
            return itemService.update(getResourceTypeName(), id, resource);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String id) {
        itemService.delete(getResourceTypeName(), id);
    }

    @Override
    public T get(String id) {
        return itemService.get(getResourceTypeName(), id);
    }

    //    @Override
    public Paging<T> get(FacetFilter filter) {
        filter.setResourceType(getResourceTypeName());
        Paging<T> paging = itemService.getResults(filter);
        return paging;
    }

    public Optional<T> findById(String s) {
        return Optional.of(itemService.get(getResourceTypeName(), s));
    }

    public void delete(T entity) {
        try {
            String id = ReflectUtils.getId(entity.getClass(), entity);
            itemService.delete(getResourceTypeName(), id);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Iterable<T> findAll(Sort sort) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getResourceTypeName());
        ff.setQuantity(10000); // FIXME
        ff.setOrderBy(sortToFacetFilterOrder(sort));
        return (Iterable<T>) itemService.getResults(ff).getResults();
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getResourceTypeName());
        ff.setQuantity(10000); // FIXME
        ff.setOrderBy(sortToFacetFilterOrder(pageable.getSort()));

        pageable.getPageNumber();

        ff.setFrom((int) pageable.getOffset() + pageable.getPageNumber() * pageable.getPageSize());
        ff.setQuantity(pageable.getPageSize());
        List<T> results = (List<T>) itemService.getResults(ff);
        return new PageImpl<>(results, pageable, results.size());
    }

    private Map<String, Object> sortToFacetFilterOrder(Sort sort) {
        Map<String, Object> sortMap = new HashMap<>();
        for (Sort.Order o : sort.get().collect(Collectors.toList())) {
            Map<String, Object> order = new HashMap<>();
            order.put("order", o.getDirection().isAscending() ? "ASC" : "DESC");
            sortMap.put(o.getProperty(), order);
        }
        return sortMap;
    }
}
