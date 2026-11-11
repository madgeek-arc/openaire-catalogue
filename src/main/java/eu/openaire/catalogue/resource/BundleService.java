package eu.openaire.catalogue.resource;

import eu.openaire.catalogue.config.OpenAIREProperties;
import eu.openaire.catalogue.resource.BundleEvent.Action;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Stores providers, services and datasources as lot1 bundles kept as JSON maps, so that payload fields outside the
 * lot1 classes, such as service {@code extras}, are kept.
 *
 * <p>Every new record is pending and inactive. A reviewer approves it, which activates it, or rejects it. Updates keep
 * the status and activity, except that an updated rejected record is pending again, for a new review. Every write is
 * validated against the model of its resource type, and each change is logged in the bundle and published as a
 * {@link BundleEvent}. Logging entries, metadata and provider users are built with the lot1 classes and stored as
 * maps.</p>
 */
@Service
public class BundleService {

    private static final Logger logger = LoggerFactory.getLogger(BundleService.class);
    private static final int MAX_QUANTITY = 10_000;
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private final GenericResourceService resources;
    private final ApplicationEventPublisher events;
    private final OpenAIREProperties properties;
    private final ObjectMapper mapper;

    public BundleService(GenericResourceService resources, ApplicationEventPublisher events,
                         OpenAIREProperties properties, ObjectMapper mapper) {
        this.resources = resources;
        this.events = events;
        this.properties = properties;
        this.mapper = mapper;
    }

    /**
     * @throws IllegalStateException if the configured channels are inconsistent
     */
    @PostConstruct
    void validateChannels() {
        Channels.validate(properties.getChannels(), properties.getEoscChannel());
    }

    /**
     * @throws ResourceNotFoundException if no record has the id
     */
    public Map<String, Object> get(ResourceKind kind, String id) {
        Map<String, Object> bundle = resources.get(kind.type(), id);
        if (bundle == null) {
            throw new ResourceNotFoundException(id, kind.type());
        }
        return bundle;
    }

    public boolean exists(ResourceKind kind, String id) {
        try {
            get(kind, id);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    public Paging<Map<String, Object>> search(ResourceKind kind, FacetFilter filter) {
        filter.setResourceType(kind.type());
        filter.setQuantity(Math.min(filter.getQuantity(), MAX_QUANTITY));
        return resources.getResults(filter);
    }

    /**
     * @return every record matching the filters, ignoring paging, up to {@value #MAX_QUANTITY}; a warning is logged
     * when more match
     */
    public List<Map<String, Object>> all(ResourceKind kind, Map<String, Object> filters) {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(MAX_QUANTITY);
        filters.forEach(filter::addFilter);
        Paging<Map<String, Object>> results = search(kind, filter);
        if (results.getTotal() > results.getResults().size()) {
            logger.warn("Read {} of {} {} records matching {}", results.getResults().size(), results.getTotal(),
                    kind.type(), filters);
        }
        return results.getResults();
    }

    /**
     * Stores a new record as pending and inactive, listed on one channel.
     *
     * @throws ResourceAlreadyExistsException if a record already has the id built from the payload
     * @throws ResourceException              if a referenced record is missing
     */
    public Map<String, Object> create(ResourceKind kind, Map<String, Object> payload, String channel,
                                      UserInfo user) {
        Map<String, Object> body = new LinkedHashMap<>(payload);
        String id = BundleIds.create(kind, body);
        if (exists(kind, id)) {
            throw new ResourceAlreadyExistsException(id, kind.type());
        }
        checkReferences(kind, body);
        Bundles.dropEmptyExtras(body);
        body.put(Bundles.ID, id);
        body.putIfAbsent(Bundles.CATALOGUE_ID, properties.getCatalogueId());
        if (kind == ResourceKind.PROVIDER) {
            addUser(body, user);
            Bundles.normaliseUserEmails(body);
        }

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put(kind.type(), body);
        bundle.put(Bundles.ID, id);
        bundle.put(Bundles.STATUS, Bundles.PENDING);
        bundle.put(Bundles.ACTIVE, false);
        bundle.put(Bundles.SUSPENDED, false);
        bundle.put(Bundles.METADATA, toMap(Metadata.createMetadata(user.fullName(), user.email())));
        Bundles.properties(bundle).put(Channels.PROPERTY, new ArrayList<>(List.of(channel)));
        log(bundle, user, LoggingInfo.Types.ONBOARD, LoggingInfo.ActionType.REGISTERED, null);

        Map<String, Object> stored = resources.add(kind.type(), bundle, true);
        events.publishEvent(new BundleEvent(kind, Action.CREATED, stored, user));
        return stored;
    }

    /**
     * Replaces the payload of a record, keeping its id, status and activity. A rejected record becomes pending again.
     * An update that changes nothing is not stored or logged. Moving the record to another owner is logged as a move.
     * A datasource cannot move, because its id is the id of its service.
     *
     * @param mayMove         whether the record may change the provider it belongs to
     * @param expectedVersion the {@link Bundles#version version} the client last read, or {@code null} to skip the
     *                        check
     * @throws ResourceException if the record changed since {@code expectedVersion}, the update moves the record
     *                           without permission, or a reference is missing
     */
    public Map<String, Object> update(ResourceKind kind, Map<String, Object> payload, String comment, UserInfo user,
                                      boolean mayMove, String expectedVersion) {
        String id = payload.get(Bundles.ID) != null ? payload.get(Bundles.ID).toString() : null;
        if (id == null || id.isBlank()) {
            throw new ResourceException("Field 'id' is required", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> bundle = get(kind, id);
        if (expectedVersion != null && !expectedVersion.equals(Bundles.version(bundle))) {
            throw new ResourceException("The record was changed since version '" + expectedVersion + "'",
                    HttpStatus.PRECONDITION_FAILED);
        }
        Map<String, Object> existing = Bundles.payload(bundle, kind);
        Map<String, Object> body = new LinkedHashMap<>(payload);
        String owner = ownerField(kind);
        boolean moved = owner != null && !Objects.equals(existing.get(owner), body.get(owner));
        if (moved && (!mayMove || kind == ResourceKind.DATASOURCE)) {
            throw new ResourceException("Field '" + owner + "' cannot be changed", HttpStatus.FORBIDDEN);
        }
        checkReferences(kind, body);
        Bundles.dropEmptyExtras(body);
        if (kind == ResourceKind.PROVIDER) {
            Bundles.normaliseUserEmails(body);
        }
        body.putIfAbsent(Bundles.CATALOGUE_ID, existing.getOrDefault(Bundles.CATALOGUE_ID, properties.getCatalogueId()));
        if (body.equals(existing)) {
            return bundle;
        }

        bundle.put(kind.type(), body);
        boolean resubmitted = Bundles.REJECTED.equals(Bundles.status(bundle));
        if (resubmitted) {
            bundle.put(Bundles.STATUS, Bundles.PENDING);
        }
        touchMetadata(bundle, user);
        if (moved) {
            String move = "Moved from '" + existing.get(owner) + "' to '" + body.get(owner) + "'";
            log(bundle, user, LoggingInfo.Types.UPDATE, LoggingInfo.ActionType.MOVED,
                    comment == null || comment.isBlank() ? move : move + ": " + comment);
        } else {
            log(bundle, user, LoggingInfo.Types.UPDATE, LoggingInfo.ActionType.UPDATED, comment);
        }
        return store(kind, bundle, resubmitted ? Action.RESUBMITTED : Action.UPDATED, user);
    }

    /**
     * Approves a record and activates it. Approving an approved, active record changes nothing.
     *
     * @throws ResourceException if the record belongs to a provider, or service, that is not approved and active
     */
    public Map<String, Object> approve(ResourceKind kind, String id, String comment, UserInfo user) {
        Map<String, Object> bundle = get(kind, id);
        if (Bundles.APPROVED.equals(Bundles.status(bundle)) && Bundles.active(bundle)) {
            return bundle;
        }
        requireApprovedOwner(kind, Bundles.payload(bundle, kind));
        bundle.put(Bundles.STATUS, Bundles.APPROVED);
        bundle.put(Bundles.ACTIVE, true);
        touchMetadata(bundle, user);
        log(bundle, user, LoggingInfo.Types.ONBOARD, LoggingInfo.ActionType.APPROVED, comment);
        return store(kind, bundle, Action.APPROVED, user);
    }

    /**
     * Rejects a record and deactivates it. Rejecting a rejected, inactive record changes nothing.
     */
    public Map<String, Object> reject(ResourceKind kind, String id, String comment, UserInfo user) {
        Map<String, Object> bundle = get(kind, id);
        if (Bundles.REJECTED.equals(Bundles.status(bundle)) && !Bundles.active(bundle)) {
            return bundle;
        }
        bundle.put(Bundles.STATUS, Bundles.REJECTED);
        bundle.put(Bundles.ACTIVE, false);
        touchMetadata(bundle, user);
        log(bundle, user, LoggingInfo.Types.ONBOARD, LoggingInfo.ActionType.REJECTED, comment);
        return store(kind, bundle, Action.REJECTED, user);
    }

    /**
     * Setting the activity a record already has changes nothing.
     *
     * @throws ResourceException if a record that is not approved would be activated
     */
    public Map<String, Object> setActive(ResourceKind kind, String id, boolean active, String comment, UserInfo user) {
        Map<String, Object> bundle = get(kind, id);
        if (active && !Bundles.APPROVED.equals(Bundles.status(bundle))) {
            throw new ResourceException("Only an approved record can be activated", HttpStatus.CONFLICT);
        }
        if (active == Bundles.active(bundle)) {
            return bundle;
        }
        bundle.put(Bundles.ACTIVE, active);
        touchMetadata(bundle, user);
        log(bundle, user, LoggingInfo.Types.UPDATE,
                active ? LoggingInfo.ActionType.ACTIVATED : LoggingInfo.ActionType.DEACTIVATED, comment);
        return store(kind, bundle, active ? Action.ACTIVATED : Action.DEACTIVATED, user);
    }

    /**
     * Adds a record to a channel, or removes it from one.
     *
     * @return the channels of the record
     * @throws ResourceException if the channel is not one of the configured channels
     */
    public List<String> setChannel(ResourceKind kind, String id, String channel, boolean enabled, UserInfo user) {
        Map<String, Object> bundle = get(kind, id);
        List<String> channels = Channels.change(bundle, channel, enabled, properties.getChannels());
        if (channels.equals(Channels.of(bundle))) {
            return channels;
        }
        Bundles.properties(bundle).put(Channels.PROPERTY, channels);
        touchMetadata(bundle, user);
        log(bundle, user, LoggingInfo.Types.UPDATE, LoggingInfo.ActionType.UPDATED,
                (enabled ? "Added to" : "Removed from") + " channel '" + channel + "'");
        return Channels.of(store(kind, bundle, Action.UPDATED, user));
    }

    /**
     * @throws ResourceException if other records still refer to the record
     */
    public Map<String, Object> delete(ResourceKind kind, String id, UserInfo user) {
        Map<String, Object> bundle = get(kind, id);
        if (kind == ResourceKind.PROVIDER && !all(ResourceKind.SERVICE, Map.of("resourceOrganisation", id)).isEmpty()) {
            throw new ResourceException("The provider still has services", HttpStatus.CONFLICT);
        }
        if (kind == ResourceKind.SERVICE && exists(ResourceKind.DATASOURCE, id)) {
            throw new ResourceException("The service still has a datasource", HttpStatus.CONFLICT);
        }
        resources.delete(kind.type(), id);
        events.publishEvent(new BundleEvent(kind, Action.DELETED, bundle, user));
        return bundle;
    }

    /**
     * The payload field naming the record a record belongs to: the provider of a service, the service of a
     * datasource.
     */
    public static String ownerField(ResourceKind kind) {
        return switch (kind) {
            case PROVIDER -> null;
            case SERVICE -> "resourceOrganisation";
            case DATASOURCE -> "serviceId";
        };
    }

    private Map<String, Object> store(ResourceKind kind, Map<String, Object> bundle, Action action, UserInfo user) {
        Map<String, Object> stored = resources.update(kind.type(), bundle, true);
        events.publishEvent(new BundleEvent(kind, action, stored, user));
        return stored;
    }

    private void checkReferences(ResourceKind kind, Map<String, Object> payload) {
        switch (kind) {
            case SERVICE -> requireExisting(ResourceKind.PROVIDER, payload.get("resourceOrganisation"));
            case DATASOURCE -> requireExisting(ResourceKind.SERVICE, payload.get("serviceId"));
            case PROVIDER -> {
            }
        }
    }

    private void requireApprovedOwner(ResourceKind kind, Map<String, Object> payload) {
        ResourceKind ownerKind = switch (kind) {
            case PROVIDER -> null;
            case SERVICE -> ResourceKind.PROVIDER;
            case DATASOURCE -> ResourceKind.SERVICE;
        };
        if (ownerKind == null) {
            return;
        }
        Object ownerId = payload.get(ownerField(kind));
        Map<String, Object> owner = null;
        if (ownerId != null && exists(ownerKind, ownerId.toString())) {
            owner = get(ownerKind, ownerId.toString());
        }
        if (owner == null || !Bundles.APPROVED.equals(Bundles.status(owner)) || !Bundles.active(owner)) {
            throw new ResourceException("The " + ownerKind.type() + " '" + ownerId + "' is not approved and active",
                    HttpStatus.CONFLICT);
        }
    }

    private void requireExisting(ResourceKind kind, Object id) {
        if (id == null || !exists(kind, id.toString())) {
            throw new ResourceException("Unknown " + kind.type() + " '" + id + "'", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Makes the registering user one of the provider users, so they can manage it.
     */
    @SuppressWarnings("unchecked")
    private void addUser(Map<String, Object> provider, UserInfo user) {
        if (user.email() == null || user.email().isBlank() || Bundles.userEmails(provider).contains(user.email())) {
            return;
        }
        List<Object> users = provider.get("users") instanceof List<?> existing
                ? new ArrayList<>((List<Object>) existing)
                : new ArrayList<>();
        users.add(toMap(new User(user.sub(), user.email(), user.name(), user.surname())));
        provider.put("users", users);
    }

    private void log(Map<String, Object> bundle, UserInfo user, LoggingInfo.Types type,
                     LoggingInfo.ActionType action, String comment) {
        LoggingInfo entry = LoggingInfo.createLoggingInfoEntry(user, type.getKey(), action.getKey(), comment);
        Bundles.log(bundle, toMap(entry));
    }

    private void touchMetadata(Map<String, Object> bundle, UserInfo user) {
        Metadata metadata = bundle.get(Bundles.METADATA) instanceof Map<?, ?> stored
                ? mapper.convertValue(stored, Metadata.class)
                : null;
        bundle.put(Bundles.METADATA, toMap(Metadata.updateMetadata(metadata, user.fullName())));
    }

    private Map<String, Object> toMap(Object value) {
        return mapper.convertValue(value, MAP);
    }
}
