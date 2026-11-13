package eu.openaire.catalogue.extension;

import gr.uoa.di.madgik.catalogue.domain.Model;
import gr.uoa.di.madgik.catalogue.domain.Section;
import gr.uoa.di.madgik.catalogue.domain.UiField;
import gr.uoa.di.madgik.resourcecatalogue.domain.Datasource;
import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;
import gr.uoa.di.madgik.resourcecatalogue.domain.Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every field of the lot1 domain class of a resource type, nested fields included, has a field in its OpenAIRE model,
 * so that records in lot1 form pass validation. Fields that OpenAIRE deliberately leaves out of its models are listed
 * in {@link #OMITTED}.
 */
class OpenAIREModelLot1CoverageTest {

    private static final String DOMAIN_PACKAGE = Service.class.getPackageName();
    private static final Map<String, Class<?>> CLASSES = Map.of(
            "service", Service.class, "provider", Provider.class, "datasource", Datasource.class);
    // lot1 fields left out of each model, with their nested fields.
    private static final Map<String, Set<String>> OMITTED = Map.of(
            "service", Set.of("classTier", "horizontalService", "serviceCategories", "marketplaceLocations",
                    "categories.subcategory"),
            "provider", Set.of(), "datasource", Set.of());

    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {"service", "provider", "datasource"})
    void modelCoversEveryLot1Field(String type) throws IOException {
        Set<String> modelFields = modelFields(type);
        List<String> missing = new ArrayList<>();
        for (String path : lot1Fields(CLASSES.get(type))) {
            if (!modelFields.contains(path) && !omitted(type, path)) {
                missing.add(path);
            }
        }
        assertEquals(List.of(), missing);
    }

    private static boolean omitted(String type, String path) {
        return OMITTED.get(type).stream().anyMatch(field -> path.equals(field) || path.startsWith(field + "."));
    }

    @Test
    void readsNestedLot1Fields() {
        Set<String> fields = lot1Fields(Service.class);
        assertTrue(fields.contains("resourceOrganisation"));
        assertTrue(fields.contains("mainContact.email"));
        assertTrue(fields.contains("multimedia.multimediaURL"));
    }

    /**
     * @return the instance fields of the class, with the fields of nested lot1 domain classes as dotted paths
     */
    static Set<String> lot1Fields(Class<?> type) {
        Set<String> paths = new LinkedHashSet<>();
        collect(type, "", paths, new HashSet<>());
        return paths;
    }

    private static void collect(Class<?> type, String prefix, Set<String> paths, Set<Class<?>> visiting) {
        if (!visiting.add(type)) {
            return;
        }
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                String path = prefix + field.getName();
                paths.add(path);
                Class<?> nested = domainClass(field.getGenericType());
                if (nested != null) {
                    collect(nested, path + ".", paths, visiting);
                }
            }
        }
        visiting.remove(type);
    }

    /**
     * @return the lot1 domain class of a field, or of the elements of a collection field; null for other types
     */
    private static Class<?> domainClass(Type type) {
        if (type instanceof ParameterizedType parameterized
                && parameterized.getRawType() instanceof Class<?> raw && Collection.class.isAssignableFrom(raw)) {
            return domainClass(parameterized.getActualTypeArguments()[0]);
        }
        if (type instanceof Class<?> clazz && !clazz.isEnum() && clazz.getPackageName().equals(DOMAIN_PACKAGE)) {
            return clazz;
        }
        return null;
    }

    private Set<String> modelFields(String type) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/openaire/models/m-oa-" + type + ".json")) {
            Set<String> paths = new HashSet<>();
            for (Section section : mapper.readValue(in, Model.class).getSections()) {
                collect(section, paths);
            }
            return paths;
        }
    }

    private void collect(Section section, Set<String> paths) {
        if (section.getSubSections() != null) {
            section.getSubSections().forEach(subSection -> collect(subSection, paths));
        }
        if (section.getFields() != null) {
            section.getFields().forEach(field -> collect(field, "", paths));
        }
    }

    private void collect(UiField field, String prefix, Set<String> paths) {
        String path = prefix + field.getName();
        paths.add(path);
        if (field.getSubFields() != null) {
            field.getSubFields().forEach(subField -> collect(subField, path + ".", paths));
        }
    }
}
