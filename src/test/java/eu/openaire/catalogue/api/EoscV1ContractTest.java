package eu.openaire.catalogue.api;

import eu.openaire.catalogue.config.OpenAIREProperties;
import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.ResourceKind;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Pins the fields of version 1 of the EOSC API and their types, which come from the lot1 classes: a new lot1 version
 * that adds, removes, renames or retypes a field fails here. After deciding the change is acceptable for version 1, regenerate the files with
 * {@code mvn test -Dtest=EoscV1ContractTest -Deosc.contract.update=true}.
 */
class EoscV1ContractTest {

    private static final Path FIXTURES = Path.of("src/test/resources/eosc/v1");

    private final EoscController controller = new EoscController(mock(BundleService.class),
            mock(ResourceTypeService.class), JsonMapper.builder().build(), new OpenAIREProperties());

    @ParameterizedTest
    @EnumSource(ResourceKind.class)
    void theFieldsOfVersion1AreUnchanged(ResourceKind kind) throws IOException {
        Path file = FIXTURES.resolve(kind.name().toLowerCase(Locale.ROOT) + "-fields.txt");
        List<String> actual = controller.lot1Fields(kind).entrySet().stream()
                .map(field -> field.getKey() + ": " + field.getValue())
                .sorted()
                .toList();
        if (Boolean.getBoolean("eosc.contract.update")) {
            Files.createDirectories(FIXTURES);
            Files.write(file, actual);
        }
        List<String> expected = Files.exists(file) ? Files.readAllLines(file) : List.of();

        List<String> added = new ArrayList<>(actual);
        added.removeAll(expected);
        List<String> removed = new ArrayList<>(expected);
        removed.removeAll(actual);
        assertTrue(added.isEmpty() && removed.isEmpty(), "The " + kind + " fields of /eosc/v1 changed. Added: "
                + added + ". Removed: " + removed + ". See the class comment to accept the change.");
    }
}
