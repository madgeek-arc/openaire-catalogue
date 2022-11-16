package gr.madgik.catalogue.openaire;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.Identifiable;
import eu.openminted.registry.core.domain.Facet;
import eu.openminted.registry.core.domain.Value;
import gr.madgik.catalogue.BundleResourceOperations;
import gr.madgik.catalogue.dto.FacetedPage;
import gr.madgik.catalogue.openaire.dto.CountFacet;
import gr.madgik.catalogue.openaire.dto.FacetValue;
import gr.madgik.catalogue.openaire.dto.ResourceSnippet;
import gr.madgik.catalogue.openaire.dto.SortByCountFacet;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.query.Criteria.where;

public abstract class AbstractResourceBundleMongoService<T extends Identifiable, B extends Bundle<T>, ID> implements BundleResourceOperations<T, B, ID> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractResourceBundleMongoService.class);
    protected final MongoTemplate mongoTemplate;
    protected final String collectionName;
    private final Class<B> bundleType;

    protected AbstractResourceBundleMongoService(Class<B> bundleType, MongoTemplate mongoTemplate) {
        this.bundleType = bundleType;
        this.mongoTemplate = mongoTemplate;
        this.collectionName = bundleType.getSimpleName().substring(0, 1).toLowerCase() + bundleType.getSimpleName().substring(1);
    }

    @Override
    public B update(ID id, B resource) {
        return null;
    }

//    @Override
    public List<T> search(Map<String, Object> filters) {
        Query dynamicQuery = new Query();
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            dynamicQuery.addCriteria(where(filter.getKey()).is(filter.getValue()));
        }
        List<B> filteredResults = mongoTemplate.find(dynamicQuery, bundleType, collectionName);
//        List<Object> facetedResults = mongoTemplate.aggregate(newAggregation(
//                facet(
//                        match(where("payload.tags").exists(true))
//                ).as("tags")), Object.class, Object.class
//        ).getMappedResults();
        return filteredResults.stream().map(Bundle::getPayload).collect(Collectors.toList());
    }

//    @Override
    public FacetedPage<T> search(Map<String, Object> filters, Pageable pageable) {
        Query dynamicQuery = new Query().with(pageable);
        List<Criteria> criteriaList = new ArrayList<>();
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            criteriaList.add(where(filter.getKey()).is(filter.getValue()));
            dynamicQuery.addCriteria(where(filter.getKey()).is(filter.getValue()));
        }
        Criteria criteria = new Criteria().andOperator(criteriaList);
        List<B> filteredResults = mongoTemplate.find(dynamicQuery, bundleType, collectionName);
//        AggregationOperation unwind = Aggregation.unwind("payload.tags");
//
//        FacetOperation facetUnwind = facet(unwind("payload.tags"), sortByCount("payload.tags")).as("tags");


        Page<B> bundlePage = PageableExecutionUtils.getPage(
                filteredResults,
                pageable,
                () -> mongoTemplate.count(dynamicQuery, bundleType));
//        List<?> facetedResults = mongoTemplate.aggregate(newAggregation(match(where("payload.extras").exists(true)), facetUnwind), ServiceBundle.class, Object.class).getMappedResults();
//        mongoTemplate.aggregate(newAggregation(facet(match(where("payload.extras.portfolios").exists(true)), facetUnwind).as("portfolios")), ServiceBundle.class, Object.class).getUniqueMappedResult();
        FacetedPage<T> page = new FacetedPage<>();
        page.setFacets(createFacets(filters, criteria));
        Page<T> p = bundlePage.map(Bundle::getPayload);
        page.setPage(p);
        return page;
    }

//    @Override
    public FacetedPage<T> searchSnippets(Map<String, Object> filters, String field, String keyword, Pageable pageable, String[] fields) {
        Query dynamicQuery = new Query();
        if (fields != null) {
            dynamicQuery.fields().include(fields);
        }
        List<Criteria> criteriaList = new ArrayList<>();
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            String key = filter.getKey().startsWith("payload.") ? filter.getKey() : "payload." + filter.getKey();
            criteriaList.add(where(key).is(filter.getValue()));
            dynamicQuery.addCriteria(where(key).is(filter.getValue()));
        }

        Criteria criteria = Criteria.where("payload." + field).regex(keyword.toLowerCase(), "i");
        if (!criteriaList.isEmpty()) {
            criteria.andOperator(criteriaList);
        }
        long count = mongoTemplate.count(dynamicQuery, bundleType);
        dynamicQuery.with(pageable);
        List<B> filteredResults = mongoTemplate.find(dynamicQuery, bundleType, collectionName);


        Page<B> bundlePage = PageableExecutionUtils.getPage(
                filteredResults,
                pageable,
                () -> count);

        FacetedPage<T> page = new FacetedPage<>();
        page.setFacets(createFacets(filters, criteria));
        Page<T> p = bundlePage.map(Bundle::getPayload);
        page.setPage(p);
        return page;
    }

//    private List<LinkedHashMap> getFacets(Map<String, Object> filters, Criteria criteria) {
//        List<LinkedHashMap> list = new ArrayList<>();
////        List<Map<String, List<Map<String, Integer>>>> values = new ArrayList<>(); // TODO
//        for (Map.Entry<String, Object> entry : filters.entrySet()) {
//            list.addAll(mongoTemplate.aggregate(newAggregation(/*match(criteria),*/ facet(unwind("payload." + entry.getKey()), sortByCount("payload." + entry.getKey())).as(entry.getKey())), bundleType, LinkedHashMap.class).getMappedResults());
//        }
//        return list;
//    }

    private List<LinkedHashMap> getFacets(Map<String, Object> filters, Criteria criteria) {
        List<LinkedHashMap> list = new ArrayList<>();
//        List<Map<String, List<Map<String, Integer>>>> values = new ArrayList<>(); // TODO
        String[] fields = {"payload.trl","payload.lifeCycleStatus","payload.extras.users","payload.extras.portfolios"};
        for (String field : fields) {
            try {
                list.addAll(mongoTemplate.aggregate(newAggregation(/*match(criteria),*/ facet(unwind(field), sortByCount(field)).as(field)), bundleType, LinkedHashMap.class).getMappedResults());
            } catch (Exception e) {
                logger.error("Error retrieving facets", e);
            }
        }
        return list;
    }

    private List<Facet> createFacets(Map<String, Object> filters, Criteria criteria) {
        List<LinkedHashMap> results = getFacets(filters, criteria);
        List<Facet> facets = new ArrayList<>();
        for (LinkedHashMap obj : results) {
            Facet facet = new Facet();
            for (Map.Entry<String, List> entry : (Set<Map.Entry<String, List>>) obj.entrySet()) {
                facet.setField(entry.getKey());
                facet.setLabel(entry.getKey());
                facet.setValues(new ArrayList<>());
                for (Map o : (List<Map>) entry.getValue()) {
                    JSONObject jsonObject = new JSONObject(o);
                    Value value = new Value();
                    value.setValue((String) jsonObject.get("_id"));
                    value.setCount((Integer) jsonObject.get("count"));
                    facet.getValues().add(value);
                }
                facets.add(facet);
            }
        }
        return facets;
    }
}
