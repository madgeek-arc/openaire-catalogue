package eu.openaire.catalogue.api;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The APIs a service is listed on, stored in the {@code channels} property of its bundle. A service without channels
 * is listed nowhere. Channels are separate from {@code active}, which is the lifecycle state of the service.
 */
public final class ServiceChannels {

    public static final String PROPERTY = "channels";
    public static final String OA = "oa";
    public static final String NODE = "node";

    private static final Set<String> KNOWN = Set.of(OA, NODE);

    private ServiceChannels() {
    }

    /**
     * Limits a listing to the active services of a channel, overriding any value the client sent.
     */
    public static FacetFilter restrict(FacetFilter filter, String channel) {
        filter.addFilter("active", true);
        filter.addFilter(PROPERTY, channel);
        return filter;
    }

    /**
     * @return the given bundle
     * @throws ResourceNotFoundException if the bundle is not listed on the channel
     */
    public static <T extends Bundle> T require(T bundle, String channel) {
        if (!of(bundle).contains(channel)) {
            throw new ResourceNotFoundException(bundle.getId(), "service");
        }
        return bundle;
    }

    public static List<String> of(Bundle bundle) {
        if (bundle.getProperties().get(PROPERTY) instanceof List<?> channels) {
            return channels.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    /**
     * @return the channels of the bundle after adding or removing one
     * @throws ResourceException if the channel is unknown
     */
    public static List<String> change(Bundle bundle, String channel, boolean enabled) {
        if (!KNOWN.contains(channel)) {
            throw new ResourceException("Unknown channel '" + channel + "'", HttpStatus.BAD_REQUEST);
        }
        List<String> channels = new ArrayList<>(of(bundle));
        channels.remove(channel);
        if (enabled) {
            channels.add(channel);
        }
        return channels;
    }
}
