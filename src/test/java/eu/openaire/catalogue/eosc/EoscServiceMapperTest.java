package eu.openaire.catalogue.eosc;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EoscServiceMapperTest {

    @Test
    void mapsRenamedAndReshapedFields() {
        Map<String, Object> stored = new HashMap<>();
        stored.put("id", "21.T15999/abc");
        stored.put("name", "Service");
        stored.put("webpage", "https://example.org");
        stored.put("lastUpdate", "2021-09-13T00:00:00.000+00:00");
        stored.put("resourceOwner", "athena");
        stored.put("resourceProviders", List.of("cnr"));
        stored.put("accessTypes", List.of("access_type-virtual", "access_type-remote"));
        stored.put("alternativeIdentifiers", List.of(Map.of("type", "DOI", "value", "10.1/x")));
        stored.put("publicContacts", List.of(Map.of("email", "a@example.org", "phone", "1"), Map.of("phone", "2")));
        stored.put("mainContact", Map.of("firstName", "A", "lastName", "B", "email", "b@example.org", "phone", "3"));

        Map<String, Object> eosc = EoscServiceMapper.map(stored);

        assertEquals("21.T15999/abc", eosc.get("id"));
        assertEquals("athena", eosc.get("resourceOwner"));
        assertEquals(List.of("cnr"), eosc.get("serviceProviders"));
        assertEquals("access_type-virtual", eosc.get("accessTypes"));
        assertEquals("2021-09-13T00:00:00.000+00:00", eosc.get("publishingDate"));
        assertEquals(List.of("https://example.org"), eosc.get("urls"));
        assertEquals(List.of(Map.of("pid", "10.1/x", "pidSchema", "DOI")), eosc.get("alternativePIDs"));
        assertEquals(List.of("a@example.org"), eosc.get("publicContacts"));
        assertEquals(Map.of("firstName", "A", "lastName", "B", "email", "b@example.org"), eosc.get("mainContact"));
    }

    @Test
    void fallsBackToResourceOrganisationForTheOwner() {
        Map<String, Object> eosc = EoscServiceMapper.map(Map.of("resourceOrganisation", "cern"));

        assertEquals("cern", eosc.get("resourceOwner"));
    }

    @Test
    void leavesOutMissingAndOpenAireOnlyFields() {
        Map<String, Object> stored = new HashMap<>();
        stored.put("tagline", "Only in OpenAIRE");
        stored.put("extras", Map.of("portfolios", List.of()));
        stored.put("logo", null);
        stored.put("accessTypes", List.of());
        stored.put("publicContacts", Arrays.asList((Object) null));

        Map<String, Object> eosc = EoscServiceMapper.map(stored);

        assertFalse(eosc.containsKey("tagline"));
        assertFalse(eosc.containsKey("extras"));
        assertFalse(eosc.containsKey("logo"));
        assertFalse(eosc.containsKey("accessTypes"));
        assertFalse(eosc.containsKey("urls"));
        assertEquals(List.of(), eosc.get("publicContacts"));
    }

    @Test
    void emptyPayloadMapsToEmptyResult() {
        assertTrue(EoscServiceMapper.map(Map.of()).isEmpty());
    }
}
