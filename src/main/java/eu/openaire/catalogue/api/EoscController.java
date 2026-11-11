package eu.openaire.catalogue.api;

import eu.openaire.catalogue.config.OpenAIREProperties;
import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.Bundles;
import eu.openaire.catalogue.resource.Channels;
import eu.openaire.catalogue.resource.ResourceKind;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Datasource;
import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;
import gr.uoa.di.madgik.resourcecatalogue.domain.Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.introspect.BeanPropertyDefinition;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read-only routes in the lot1 format, for harvesting: each payload is read into the lot1 {@link Provider},
 * {@link Service} or {@link Datasource} and written from it, so fields outside those classes are left out. Only the
 * active records on the channel set by {@code openaire.eosc-channel} are shown. Providers are shown without their
 * users, whose emails are personal data. Search facets are limited to the index fields whose path is a lot1 field.
 * The output follows the lot1 classes on the classpath, so a new lot1 version changes version 1 of this API unless it
 * is pinned.
 */
@RestController
@RequestMapping(path = "eosc/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "EOSC", description = "Active providers, services and datasources in the lot1 format, version 1")
public class EoscController {

    private static final String LOT1_PACKAGE = Service.class.getPackageName();

    private final BundleService bundles;
    private final ResourceTypeService resourceTypes;
    private final ObjectMapper mapper;
    private final String channel;
    private final ObjectReader services;
    private final ObjectReader providers;
    private final ObjectReader datasources;
    private final Map<ResourceKind, Map<String, String>> lot1Fields = new EnumMap<>(ResourceKind.class);

    public EoscController(BundleService bundles, ResourceTypeService resourceTypes, ObjectMapper mapper,
                          OpenAIREProperties properties) {
        this.bundles = bundles;
        this.resourceTypes = resourceTypes;
        this.mapper = mapper;
        this.channel = properties.getEoscChannel();
        this.services = reader(Service.class);
        this.providers = reader(Provider.class);
        this.datasources = reader(Datasource.class);
        Map<String, String> providerFields = fields(Provider.class);
        providerFields.keySet().removeIf(path -> path.equals("users") || path.startsWith("users."));
        lot1Fields.put(ResourceKind.PROVIDER, providerFields);
        lot1Fields.put(ResourceKind.SERVICE, fields(Service.class));
        lot1Fields.put(ResourceKind.DATASOURCE, fields(Datasource.class));
    }

    @Operation(summary = "Returns the provider with the given id.")
    @GetMapping({"providers/{id}", "organisations/{id}"})
    public Provider getProvider(@PathVariable String id) {
        return toProvider(published(ResourceKind.PROVIDER, id));
    }

    @Operation(summary = "Returns the providers, based on a set of filters.")
    @BrowseParameters
    @GetMapping({"providers", "organisations"})
    public Paging<Provider> getProviders(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        return search(ResourceKind.PROVIDER, params).map(this::toProvider);
    }

    @Operation(summary = "Returns the service with the given id.")
    @GetMapping("services/{id}")
    public Service getService(@PathVariable String id) {
        return toService(published(ResourceKind.SERVICE, id));
    }

    @Operation(summary = "Returns the services, based on a set of filters.")
    @BrowseParameters
    @GetMapping("services")
    public Paging<Service> getServices(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        return search(ResourceKind.SERVICE, params).map(this::toService);
    }

    @Operation(summary = "Returns the datasource with the given id.")
    @GetMapping("datasources/{id}")
    public Datasource getDatasource(@PathVariable String id) {
        return toDatasource(published(ResourceKind.DATASOURCE, id));
    }

    @Operation(summary = "Returns the datasources, based on a set of filters.")
    @BrowseParameters
    @GetMapping("datasources")
    public Paging<Datasource> getDatasources(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        return search(ResourceKind.DATASOURCE, params).map(this::toDatasource);
    }

    Provider toProvider(Map<String, Object> bundle) {
        Provider provider = read(providers, bundle, ResourceKind.PROVIDER);
        provider.setUsers(null);
        return provider;
    }

    Service toService(Map<String, Object> bundle) {
        return read(services, bundle, ResourceKind.SERVICE);
    }

    Datasource toDatasource(Map<String, Object> bundle) {
        return read(datasources, bundle, ResourceKind.DATASOURCE);
    }

    /**
     * @throws ResourceNotFoundException if the record is inactive or outside the EOSC channel
     */
    private Map<String, Object> published(ResourceKind kind, String id) {
        Map<String, Object> bundle = bundles.get(kind, id);
        if (!Bundles.active(bundle)) {
            throw new ResourceNotFoundException(id, kind.type());
        }
        return Channels.require(bundle, kind, channel);
    }

    private Paging<Map<String, Object>> search(ResourceKind kind, MultiValueMap<String, Object> params) {
        Paging<Map<String, Object>> page = bundles.search(kind, Channels.restrict(FacetFilter.from(params), channel));
        if (page.getFacets() != null) {
            Set<String> fields = lot1IndexFields(kind);
            page.setFacets(page.getFacets().stream().filter(facet -> fields.contains(facet.getField())).toList());
        }
        return page;
    }

    /**
     * @return the fields a record of {@code kind} can have in the output: dotted path to Java type
     */
    Map<String, String> lot1Fields(ResourceKind kind) {
        return lot1Fields.get(kind);
    }

    /**
     * @return the names of the index fields of {@code kind} whose path, inside the payload, is a lot1 field
     */
    Set<String> lot1IndexFields(ResourceKind kind) {
        ResourceType type = resourceTypes.getResourceType(kind.type());
        if (type == null || type.getIndexFields() == null) {
            return Set.of();
        }
        Set<String> paths = lot1Fields.get(kind).keySet();
        String prefix = "$." + kind.type() + ".";
        return type.getIndexFields().stream()
                .filter(field -> field.getPath() != null && field.getPath().startsWith(prefix))
                .filter(field -> paths.contains(field.getPath().substring(prefix.length()).replace("[*]", "")))
                .map(IndexField::getName)
                .collect(Collectors.toSet());
    }

    /**
     * @return the dotted paths of every property of {@code type}, descending into collections and lot1 classes
     */
    private Map<String, String> fields(Class<?> type) {
        Map<String, String> paths = new LinkedHashMap<>();
        collectPaths(mapper.constructType(type), "", paths, new HashSet<>());
        return paths;
    }

    private void collectPaths(JavaType type, String prefix, Map<String, String> paths, Set<Class<?>> visiting) {
        if (!visiting.add(type.getRawClass())) {
            return;
        }
        for (BeanPropertyDefinition property : mapper._serializationContext().introspectBeanDescription(type)
                .findProperties()) {
            String path = prefix + property.getName();
            JavaType propertyType = property.getPrimaryType();
            paths.put(path, propertyType.toCanonical());
            while (propertyType != null && propertyType.isContainerType() && !propertyType.isMapLikeType()) {
                propertyType = propertyType.getContentType();
            }
            if (isLot1Bean(propertyType)) {
                collectPaths(propertyType, path + ".", paths, visiting);
            }
        }
        visiting.remove(type.getRawClass());
    }

    private static boolean isLot1Bean(JavaType type) {
        return type != null && !type.isEnumType() && type.getRawClass().getPackageName().startsWith(LOT1_PACKAGE);
    }

    private <T> T read(ObjectReader reader, Map<String, Object> bundle, ResourceKind kind) {
        return reader.readValue(mapper.<JsonNode>valueToTree(Bundles.payload(bundle, kind)));
    }

    private ObjectReader reader(Class<?> type) {
        return mapper.readerFor(type).without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
