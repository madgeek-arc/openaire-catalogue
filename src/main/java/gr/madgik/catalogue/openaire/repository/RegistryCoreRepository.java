package gr.madgik.catalogue.openaire.repository;

import gr.athenarc.catalogue.exception.ResourceAlreadyExistsException;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.athenarc.catalogue.utils.ReflectUtils;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class RegistryCoreRepository<T, ID extends String> implements Repository<T, ID> {

    private static final Logger logger = LoggerFactory.getLogger(RegistryCoreRepository.class);
    protected final GenericItemService itemService;

    public abstract String getResourceTypeName();

    public RegistryCoreRepository(GenericItemService itemService) {
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
//    public Page<T> get(Pageable pageable) {
//        FacetFilter ff = new FacetFilter();
//        ff.setResourceType(getResourceTypeName());
//        // pageable to ff
//        Paging<T> paging = itemService.getResults(ff);
//        return new PageImpl<>(paging.getResults()); // FIXME: set total size and page
//    }

    //    @Override
    public Paging<T> get(FacetFilter filter) {
        filter.setResourceType(getResourceTypeName());
        Paging<T> paging = itemService.getResults(filter);
        return paging;
    }


    @Override
    public <S extends T> S save(S entity) {
        S resource;
        try {
            String id = ReflectUtils.getId(entity.getClass(), entity);
            resource = itemService.update(getResourceTypeName(), id, entity);
        } catch (RuntimeException e) {
            resource = itemService.add(getResourceTypeName(), entity);
        } catch (NoSuchFieldException e) {
            logger.error("Could not find ID field..", e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return resource;
    }

    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        List<S> resources = new ArrayList<>();
        for (S entity : entities) {
            resources.add(save(entity));
        }
        return resources;
    }

    @Override
    public Optional<T> findById(String s) {
        return Optional.of(itemService.get(getResourceTypeName(), s));
    }

    @Override
    public boolean existsById(String s) {
        boolean exists = true;
        try {
            itemService.get(getResourceTypeName(), s);
        } catch (ResourceNotFoundException e) {
            exists = false;
        }
        return exists;
    }

    @Override
    public Iterable<T> findAll() {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getResourceTypeName());
        ff.setQuantity(10000); // FIXME
        return (Iterable<T>) itemService.getResults(ff).getResults();
    }

    @Override
    public Iterable<T> findAllById(Iterable<ID> ids) {
        List<T> resources = new ArrayList<>();
        for (String id : ids) {
            resources.add(itemService.get(getResourceTypeName(), id));
        }
        return resources;
    }

    @Override
    public long count() {
        int counter = 0;
        for (Object i : findAll()) {
            counter++;
        }
        return counter;
    }

    @Override
    public void delete(T entity) {
        try {
            String id = ReflectUtils.getId(entity.getClass(), entity);
            itemService.delete(getResourceTypeName(), id);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {
        for (String id : ids) {
            itemService.delete(getResourceTypeName(), id);
        }
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        for (T entity : entities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        for (T entity : findAll()) {
            delete(entity);
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
