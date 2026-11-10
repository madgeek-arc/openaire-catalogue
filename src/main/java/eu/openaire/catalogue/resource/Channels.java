package eu.openaire.catalogue.resource;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The APIs a record is listed on, stored in the {@code channels} property of its bundle. A record without channels
 * is listed nowhere. Channels are separate from {@code active}, which is the lifecycle state of the record.
 */
public final class Channels {

    public static final String PROPERTY = "channels";
    public static final String OA = "oa";
    public static final String NODE = "node";

    private Channels() {
    }

    /**
     * @param configured  the channels a record may be listed on
     * @param eoscChannel the channel whose records the EOSC API returns
     * @throws IllegalStateException if {@code configured} lacks {@link #OA} or {@link #NODE}, which have their own APIs,
     *                               or does not contain {@code eoscChannel}
     */
    public static void validate(Collection<String> configured, String eoscChannel) {
        for (String required : List.of(OA, NODE)) {
            if (!configured.contains(required)) {
                throw new IllegalStateException("openaire.channels must contain '" + required + "', which has its own API");
            }
        }
        if (!configured.contains(eoscChannel)) {
            throw new IllegalStateException("openaire.eosc-channel '" + eoscChannel + "' is not one of openaire.channels "
                    + configured);
        }
    }

    /**
     * Limits a listing to the active records of a channel, overriding any value the client sent.
     */
    public static FacetFilter restrict(FacetFilter filter, String channel) {
        filter.addFilter(Bundles.ACTIVE, true);
        filter.addFilter(PROPERTY, channel);
        return filter;
    }

    /**
     * @return the given bundle
     * @throws ResourceNotFoundException if the bundle is not listed on the channel
     */
    public static Map<String, Object> require(Map<String, Object> bundle, ResourceKind kind, String channel) {
        if (!of(bundle).contains(channel)) {
            throw new ResourceNotFoundException(Bundles.id(bundle), kind.type());
        }
        return bundle;
    }

    public static List<String> of(Map<String, Object> bundle) {
        if (Bundles.properties(bundle).get(PROPERTY) instanceof List<?> channels) {
            return channels.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    /**
     * @param known the channels a record may be listed on
     * @return the channels of the bundle after adding or removing one
     * @throws ResourceException if the channel is not one of the known channels
     */
    public static List<String> change(Map<String, Object> bundle, String channel, boolean enabled,
                                      Collection<String> known) {
        if (!known.contains(channel)) {
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
