package eu.openaire.catalogue.extension;

import eu.openaire.catalogue.config.OpenAIREProperties;
import gr.uoa.di.madgik.catalogue.domain.Model;
import gr.uoa.di.madgik.catalogue.domain.Section;
import gr.uoa.di.madgik.catalogue.domain.UiField;
import gr.uoa.di.madgik.catalogue.service.ModelService;
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
 * Stores the OpenAIRE service and datasource models and rebuilds the Registry resource types they map to,
 * so that the index fields follow the models. Models that are already stored and unchanged are left as is.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class OpenAIREModelLoader implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIREModelLoader.class);
    private static final String MODELS_PATTERN = "classpath:openaire/models/*.json";
    private static final Set<String> SERVER_ASSIGNED_FIELDS = Set.of(
            "creationDate", "modificationDate", "createdBy", "modifiedBy");

    private final ModelService modelService;
    private final ResourceTypeService resourceTypeService;
    private final ObjectMapper mapper;
    private final OpenAIREProperties properties;

    public OpenAIREModelLoader(ModelService modelService, ResourceTypeService resourceTypeService,
                               ObjectMapper mapper, OpenAIREProperties properties) {
        this.modelService = modelService;
        this.resourceTypeService = resourceTypeService;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!properties.isLoadModels()) {
            return;
        }
        for (Resource file : new PathMatchingResourcePatternResolver().getResources(MODELS_PATTERN)) {
            try (InputStream in = file.getInputStream()) {
                load(mapper.readValue(in, Model.class));
            }
        }
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
        resourceTypeService.updateResourceType(modelService.generateResourceType(model));
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
