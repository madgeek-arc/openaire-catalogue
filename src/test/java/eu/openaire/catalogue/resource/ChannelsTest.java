package eu.openaire.catalogue.resource;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChannelsTest {

    private static final List<String> KNOWN = List.of(Channels.OA, Channels.NODE);

    private static Map<String, Object> bundle(String... channels) {
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("id", "openaire.argos");
        bundle.put("properties", new LinkedHashMap<>(Map.of(Channels.PROPERTY, List.of(channels))));
        return bundle;
    }

    @Test
    void restrictOverridesTheClientValue() {
        FacetFilter filter = new FacetFilter();
        filter.addFilter(Channels.PROPERTY, Channels.OA);
        filter.addFilter("active", false);

        Channels.restrict(filter, Channels.NODE);

        assertEquals(Channels.NODE, filter.getFilter().get(Channels.PROPERTY));
        assertEquals(true, filter.getFilter().get("active"));
    }

    @Test
    void requireReturnsABundleOnTheChannel() {
        Map<String, Object> bundle = bundle(Channels.OA);
        assertSame(bundle, Channels.require(bundle, ResourceKind.SERVICE, Channels.OA));
    }

    @Test
    void requireHidesABundleOffTheChannel() {
        Map<String, Object> bundle = bundle(Channels.OA);
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> Channels.require(bundle, ResourceKind.DATASOURCE, Channels.NODE));
        assertEquals(true, e.getMessage().contains("datasource"));
    }

    @Test
    void aBundleWithoutChannelsIsListedNowhere() {
        Map<String, Object> bundle = new LinkedHashMap<>(Map.of("id", "openaire.argos"));
        assertEquals(List.of(), Channels.of(bundle));
        assertThrows(ResourceNotFoundException.class,
                () -> Channels.require(bundle, ResourceKind.PROVIDER, Channels.OA));
    }

    @Test
    void changeAddsAndRemovesOneChannel() {
        assertEquals(List.of("oa", "node"), Channels.change(bundle("oa"), "node", true, KNOWN));
        assertEquals(List.of("node"), Channels.change(bundle("oa", "node"), "oa", false, KNOWN));
        assertEquals(List.of(), Channels.change(bundle("oa"), "oa", false, KNOWN));
    }

    @Test
    void changeIsIdempotent() {
        assertEquals(List.of("oa"), Channels.change(bundle("oa"), "oa", true, KNOWN));
        assertEquals(List.of("oa"), Channels.change(bundle("oa"), "node", false, KNOWN));
    }

    @Test
    void changeRejectsAnUnknownChannel() {
        assertThrows(ResourceException.class, () -> Channels.change(bundle("oa"), "eosc", true, KNOWN));
        assertEquals(List.of("oa", "eosc"),
                Channels.change(bundle("oa"), "eosc", true, List.of("oa", "node", "eosc")));
    }

    @Test
    void theConfiguredChannelsMustCoverTheApisAndTheEoscChannel() {
        assertDoesNotThrow(() -> Channels.validate(List.of("oa", "node", "eosc"), "eosc"));
        assertThrows(IllegalStateException.class, () -> Channels.validate(List.of("oa"), "oa"));
        assertThrows(IllegalStateException.class, () -> Channels.validate(List.of("oa", "node"), "eosc"));
    }
}
