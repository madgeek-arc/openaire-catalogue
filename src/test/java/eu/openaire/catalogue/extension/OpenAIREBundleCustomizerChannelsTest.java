package eu.openaire.catalogue.extension;

import eu.openaire.catalogue.config.OpenAIREProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OpenAIREBundleCustomizerChannelsTest {

    private final OpenAIREBundleCustomizer customizer = new OpenAIREBundleCustomizer(new OpenAIREProperties(), null, null);

    @Test
    void newServiceGetsTheDefaultChannels() {
        ServiceBundle bundle = new ServiceBundle();
        customizer.beforeAdd("service", bundle, null);
        assertEquals(List.of("oa"), bundle.getProperties().get("channels"));
    }

    @Test
    void finalizedDraftGetsTheDefaultChannels() {
        ServiceBundle bundle = new ServiceBundle();
        customizer.beforeFinalizeDraft("service", bundle, null);
        assertEquals(List.of("oa"), bundle.getProperties().get("channels"));
    }

    @Test
    void existingChannelsAreKept() {
        ServiceBundle bundle = new ServiceBundle();
        bundle.getProperties().put("channels", List.of("node"));
        customizer.beforeAdd("service", bundle, null);
        assertEquals(List.of("node"), bundle.getProperties().get("channels"));
    }

    @Test
    void otherTypesGetNoChannels() {
        ServiceBundle bundle = new ServiceBundle();
        customizer.beforeAdd("datasource", bundle, null);
        assertFalse(bundle.getProperties().containsKey("channels"));
    }
}
