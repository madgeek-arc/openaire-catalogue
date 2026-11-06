package eu.openaire.catalogue.eosc;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EoscOrganisationMapperTest {

    @Test
    void mapsOrganisationFields() {
        Map<String, Object> eosc = EoscOrganisationMapper.map(Map.of(
                "id", "21.T15999/org",
                "website", "https://example.org",
                "legalEntity", true,
                "location", Map.of("city", "Meyrin", "country", "CH"),
                "publicContacts", List.of(Map.of("email", "a@example.org"))));

        assertEquals("21.T15999/org", eosc.get("id"));
        assertEquals("https://example.org", eosc.get("website"));
        assertEquals(true, eosc.get("legalEntity"));
        assertEquals("CH", eosc.get("country"));
        assertEquals(List.of("a@example.org"), eosc.get("publicContacts"));
        assertFalse(eosc.containsKey("location"));
    }

    @Test
    void neverIncludesUsers() {
        Map<String, Object> eosc = EoscOrganisationMapper.map(Map.of(
                "name", "Organisation",
                "users", List.of(Map.of("email", "user@example.org"))));

        assertFalse(eosc.containsKey("users"));
    }
}
