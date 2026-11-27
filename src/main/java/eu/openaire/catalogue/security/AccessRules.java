package eu.openaire.catalogue.security;

import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.Bundles;
import eu.openaire.catalogue.resource.Channels;
import eu.openaire.catalogue.resource.ResourceKind;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Who may read and change records. Global reviewers (administrators and the onboarding team) may do anything. A channel
 * administrator reviews and manages the records listed on at least one of their channels. A provider is also managed by
 * its users, a service by the users of its provider, and a datasource by those who manage its service. The users of a
 * provider listed among the resource providers of a service view its bundle without managing it. Only a user with an
 * email is signed in, since access is granted by email.
 */
@Component("access")
public class AccessRules {

    /**
     * The service field listing the providers that contribute to a service, besides the one that owns it.
     */
    static final String RESOURCE_PROVIDERS = "resourceProviders";

    private final BundleService bundles;
    private final Roles roles;

    public AccessRules(BundleService bundles, Roles roles) {
        this.bundles = bundles;
        this.roles = roles;
    }

    public boolean isSignedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && hasEmail(auth);
    }

    private static boolean hasEmail(Authentication auth) {
        try {
            String email = User.of(auth).getEmail();
            return email != null && !email.isBlank();
        } catch (AuthenticationException e) {
            return false;
        }
    }

    /**
     * Whether the user reviews records on at least one channel.
     */
    public boolean isReviewer(Authentication auth) {
        return isGlobalReviewer(auth) || (isSignedIn(auth) && !roles.adminChannels(auth).isEmpty());
    }

    /**
     * Whether the user reviews every record, whatever its channels.
     */
    public boolean isGlobalReviewer(Authentication auth) {
        return isSignedIn(auth) && Roles.isGlobalReviewer(auth);
    }

    /**
     * The channels whose records a channel administrator reviews. Empty for global reviewers, who review all.
     */
    public Set<String> adminChannels(Authentication auth) {
        return isSignedIn(auth) ? roles.adminChannels(auth) : Collections.emptySet();
    }

    /**
     * Whether the user may review the record with the given id: a global reviewer, or an administrator of one of the
     * channels it is listed on. False if the record does not exist.
     */
    public boolean reviews(Authentication auth, ResourceKind kind, Object id) {
        if (isGlobalReviewer(auth)) {
            return true;
        }
        if (id == null || !isSignedIn(auth)) {
            return false;
        }
        Set<String> adminChannels = roles.adminChannels(auth);
        if (adminChannels.isEmpty()) {
            return false;
        }
        try {
            return Channels.of(bundles.get(kind, id.toString())).stream().anyMatch(adminChannels::contains);
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    /**
     * Whether the user may change the record with the given id. False if the record does not exist.
     */
    public boolean canManage(Authentication auth, ResourceKind kind, Object id) {
        if (!isSignedIn(auth)) {
            return false;
        }
        if (reviews(auth, kind, id)) {
            return true;
        }
        if (id == null) {
            return false;
        }
        return isManager(auth, kind, id.toString());
    }

    /**
     * Whether the user manages the record with the given id as one of its users, not as a reviewer: reviewing a record
     * on one channel does not reveal what belongs to it on other channels.
     */
    public boolean managesAsUser(Authentication auth, ResourceKind kind, Object id) {
        return isSignedIn(auth) && id != null && isManager(auth, kind, id.toString());
    }

    /**
     * Whether the user may view the record with the given id, whatever its status or channels. False if the record
     * does not exist.
     *
     * @see #canViewBundle
     */
    public boolean canView(Authentication auth, ResourceKind kind, Object id) {
        if (id == null || !isSignedIn(auth)) {
            return false;
        }
        try {
            return canViewBundle(auth, kind, bundles.get(kind, id.toString()));
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    /**
     * Whether the user may view the record of the given bundle, whatever its status or channels: those who manage it,
     * and the users of a provider listed among the resource providers of a service. Viewing grants no change.
     */
    public boolean canViewBundle(Authentication auth, ResourceKind kind, Map<String, Object> bundle) {
        if (!isSignedIn(auth)) {
            return false;
        }
        if (isGlobalReviewer(auth)) {
            return true;
        }
        Set<String> adminChannels = roles.adminChannels(auth);
        if (Channels.of(bundle).stream().anyMatch(adminChannels::contains)) {
            return true;
        }
        Map<String, Object> payload = Bundles.payload(bundle, kind);
        if (kind == ResourceKind.PROVIDER) {
            return Bundles.userEmails(payload).contains(UserInfo.of(auth).email());
        }
        Object owner = payload.get(BundleService.ownerField(kind));
        if (owner != null && isManager(auth, ownerKind(kind), owner.toString())) {
            return true;
        }
        return kind == ResourceKind.SERVICE && values(payload.get(RESOURCE_PROVIDERS)).stream()
                .anyMatch(provider -> isProviderUser(auth, provider));
    }

    private static List<String> values(Object value) {
        if (value instanceof Collection<?> values) {
            return values.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        }
        return value != null ? List.of(value.toString()) : List.of();
    }

    /**
     * Whether an update may move the record to the owner named in the payload: global reviewers may move any record,
     * channel administrators only under an owner they review.
     */
    public boolean canMove(Authentication auth, ResourceKind kind, Map<String, Object> payload) {
        if (isGlobalReviewer(auth)) {
            return true;
        }
        ResourceKind ownerKind = ownerKind(kind);
        Object owner = payload != null && ownerKind != null ? payload.get(BundleService.ownerField(kind)) : null;
        return owner != null && reviews(auth, ownerKind, owner);
    }

    /**
     * Whether the user may add the record to a channel, or remove it from one. Global reviewers may change any channel.
     * A channel administrator changes only their own channels, on records they review, and cannot remove the last
     * channel of a record.
     */
    public boolean canChangeChannel(Authentication auth, ResourceKind kind, Object id, String channel, boolean enabled) {
        if (isGlobalReviewer(auth)) {
            return true;
        }
        if (id == null || !adminChannels(auth).contains(channel) || !reviews(auth, kind, id)) {
            return false;
        }
        if (enabled) {
            return true;
        }
        List<String> channels = Channels.of(bundles.get(kind, id.toString()));
        return !channels.equals(List.of(channel));
    }

    private boolean isManager(Authentication auth, ResourceKind kind, String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return switch (kind) {
            case PROVIDER -> isProviderUser(auth, id);
            case SERVICE -> isProviderUser(auth, owner(kind, id));
            case DATASOURCE -> isManager(auth, ResourceKind.SERVICE, owner(kind, id));
        };
    }

    /**
     * Whether the user may add the given payload: any signed-in user may register a provider, and a service or
     * datasource may be added by those who manage the record it belongs to.
     */
    public boolean canAdd(Authentication auth, ResourceKind kind, Map<String, Object> payload) {
        if (!isSignedIn(auth)) {
            return false;
        }
        if (kind == ResourceKind.PROVIDER || isGlobalReviewer(auth)) {
            return true;
        }
        Object owner = payload != null ? payload.get(BundleService.ownerField(kind)) : null;
        if (owner == null) {
            return false;
        }
        if (reviews(auth, ownerKind(kind), owner)) {
            return true;
        }
        return kind == ResourceKind.SERVICE
                ? isProviderUser(auth, owner.toString())
                : isManager(auth, ResourceKind.SERVICE, owner.toString());
    }

    /**
     * Whether the user may read the record: an active record is public, others need {@link #canViewBundle}.
     */
    public boolean canRead(Authentication auth, ResourceKind kind, Map<String, Object> bundle) {
        return Bundles.active(bundle) || canViewBundle(auth, kind, bundle);
    }

    /**
     * The type of the record a record belongs to, or {@code null} for a provider.
     */
    private static ResourceKind ownerKind(ResourceKind kind) {
        return switch (kind) {
            case PROVIDER -> null;
            case SERVICE -> ResourceKind.PROVIDER;
            case DATASOURCE -> ResourceKind.SERVICE;
        };
    }

    private boolean isProviderUser(Authentication auth, String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return false;
        }
        try {
            Map<String, Object> provider = Bundles.payload(bundles.get(ResourceKind.PROVIDER, providerId),
                    ResourceKind.PROVIDER);
            return Bundles.userEmails(provider).contains(UserInfo.of(auth).email());
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    private String owner(ResourceKind kind, String id) {
        try {
            Object owner = Bundles.payload(bundles.get(kind, id), kind).get(BundleService.ownerField(kind));
            return owner != null ? owner.toString() : null;
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }
}
