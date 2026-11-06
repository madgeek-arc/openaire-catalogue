package eu.openaire.catalogue.api;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceChannelsTest {

    private static ServiceBundle bundle(String... channels) {
        ServiceBundle bundle = new ServiceBundle();
        bundle.setId("21.T15999/abc");
        bundle.getProperties().put(ServiceChannels.PROPERTY, List.of(channels));
        return bundle;
    }

    @Test
    void restrictOverridesTheClientValue() {
        FacetFilter filter = new FacetFilter();
        filter.addFilter(ServiceChannels.PROPERTY, ServiceChannels.OA);
        filter.addFilter("active", false);

        ServiceChannels.restrict(filter, ServiceChannels.NODE);

        assertEquals(ServiceChannels.NODE, filter.getFilter().get(ServiceChannels.PROPERTY));
        assertEquals(true, filter.getFilter().get("active"));
    }

    @Test
    void requireReturnsABundleOnTheChannel() {
        ServiceBundle bundle = bundle(ServiceChannels.OA);
        assertSame(bundle, ServiceChannels.require(bundle, ServiceChannels.OA));
    }

    @Test
    void requireHidesABundleOffTheChannel() {
        ServiceBundle bundle = bundle(ServiceChannels.OA);
        assertThrows(ResourceNotFoundException.class, () -> ServiceChannels.require(bundle, ServiceChannels.NODE));
    }

    @Test
    void aBundleWithoutChannelsIsListedNowhere() {
        ServiceBundle bundle = new ServiceBundle();
        bundle.setId("21.T15999/abc");
        assertEquals(List.of(), ServiceChannels.of(bundle));
        assertThrows(ResourceNotFoundException.class, () -> ServiceChannels.require(bundle, ServiceChannels.OA));
    }

    @Test
    void changeAddsAndRemovesOneChannel() {
        assertEquals(List.of("oa", "node"), ServiceChannels.change(bundle("oa"), "node", true));
        assertEquals(List.of("node"), ServiceChannels.change(bundle("oa", "node"), "oa", false));
        assertEquals(List.of(), ServiceChannels.change(bundle("oa"), "oa", false));
    }

    @Test
    void changeIsIdempotent() {
        assertEquals(List.of("oa"), ServiceChannels.change(bundle("oa"), "oa", true));
        assertEquals(List.of("oa"), ServiceChannels.change(bundle("oa"), "node", false));
    }

    @Test
    void changeRejectsAnUnknownChannel() {
        assertThrows(ResourceException.class, () -> ServiceChannels.change(bundle("oa"), "other", true));
    }
}
