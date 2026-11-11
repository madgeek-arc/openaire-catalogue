package eu.openaire.catalogue.extension;

import eu.openaire.catalogue.config.OpenAIREProperties;
import gr.uoa.di.madgik.catalogue.domain.Model;
import gr.uoa.di.madgik.catalogue.domain.Section;
import gr.uoa.di.madgik.catalogue.domain.UiField;
import gr.uoa.di.madgik.catalogue.service.ModelResourceTypeMapper;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Stores the OpenAIRE provider, service and datasource models and creates or rebuilds the Registry resource types
 * they map to, so that the index fields follow the models. Models that are already stored and unchanged are left as
 * is. Also creates the {@code vocabulary} resource type, which has no model, when it is missing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class OpenAIREModelLoader implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIREModelLoader.class);
    private static final String MODELS_PATTERN = "classpath:openaire/models/*.json";
    private static final String VOCABULARY_TYPE = "classpath:openaire/resourceTypes/vocabulary.json";
    private static final Set<String> SERVER_ASSIGNED_FIELDS = Set.of(
            "creationDate", "modificationDate", "createdBy", "modifiedBy");

    private final ModelService modelService;
    private final ModelResourceTypeMapper typeMapper;
    private final ResourceTypeService resourceTypeService;
    private final ObjectMapper mapper;
    private final OpenAIREProperties properties;

    public OpenAIREModelLoader(ModelService modelService, ModelResourceTypeMapper typeMapper,
                               ResourceTypeService resourceTypeService, ObjectMapper mapper,
                               OpenAIREProperties properties) {
        this.modelService = modelService;
        this.typeMapper = typeMapper;
        this.resourceTypeService = resourceTypeService;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!properties.isLoadModels()) {
            return;
        }
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (Resource file : resolver.getResources(MODELS_PATTERN)) {
            try (InputStream in = file.getInputStream()) {
                load(mapper.readValue(in, Model.class));
            }
        }
        ResourceType vocabulary;
        try (InputStream in = resolver.getResource(VOCABULARY_TYPE).getInputStream()) {
            vocabulary = mapper.readValue(in, ResourceType.class);
        }
        ResourceType stored = resourceTypeService.getResourceType(vocabulary.getName());
        if (stored == null) {
            resourceTypeService.addResourceType(vocabulary);
            logger.info("Resource type [vocabulary] added");
        } else {
            alignPayloadClass(stored, vocabulary.getProperty("class"));
        }
    }

    /**
     * Sets the payload class of a stored resource type, keeping the rest of its definition, so that its records are
     * read as that class. The update goes through a detached copy of the stored type.
     */
    private void alignPayloadClass(ResourceType stored, String payloadClass) {
        if (payloadClass.equals(stored.getProperty("class"))) {
            return;
        }
        ResourceType update = mapper.convertValue(stored, ResourceType.class);
        update.getProperties().put("class", payloadClass);
        resourceTypeService.updateResourceType(update);
        logger.info("Resource type [{}] payload class set to {} (was {})",
                stored.getName(), payloadClass, stored.getProperty("class"));
    }

    private void load(Model model) {
        deriveParents(model);
        Model existing = find(model.getId());
        if (existing == null) {
            modelService.add(model);
            logger.info("Model [{}] added", model.getId());
        } else if (!content(existing).equals(content(model))) {
            modelService.update(model.getId(), model);
            logger.info("Model [{}] updated", model.getId());
        } else {
            logger.debug("Model [{}] is up to date", model.getId());
        }
        // Adding a model does not change a resource type that already exists, and updating one refreshes the
        // resource type asynchronously, so the mapping is applied explicitly.
        if (resourceTypeService.getResourceType(model.getResourceType()) == null) {
            resourceTypeService.addResourceType(typeMapper.map(model, new ResourceType()));
            logger.info("Resource type [{}] added", model.getResourceType());
        } else {
            resourceTypeService.updateResourceType(modelService.generateResourceType(model));
        }
    }

    private Model find(String id) {
        try {
            return modelService.get(id);
        } catch (ResourceNotFoundException | NoSuchElementException e) {
            return null;
        }
    }

    private JsonNode content(Model model) {
        JsonNode node = mapper.valueToTree(model);
        if (node instanceof ObjectNode object) {
            object.remove(SERVER_ASSIGNED_FIELDS);
        }
        return node;
    }

    private void deriveParents(Model model) {
        if (model.getSections() != null) {
            model.getSections().forEach(this::deriveParents);
        }
    }

    private void deriveParents(Section section) {
        if (section.getSubSections() != null) {
            section.getSubSections().forEach(this::deriveParents);
        }
        if (section.getFields() != null) {
            section.getFields().forEach(this::deriveParents);
        }
    }

    private void deriveParents(UiField parent) {
        if (parent.getSubFields() != null) {
            for (UiField field : parent.getSubFields()) {
                field.setParentId(parent.getId());
                field.setParent(parent.getName());
                deriveParents(field);
            }
        }
    }
}
