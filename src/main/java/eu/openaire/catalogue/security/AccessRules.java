package eu.openaire.catalogue.security;

import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.Bundles;
import eu.openaire.catalogue.resource.ResourceKind;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Who may read and change records. Reviewers (administrators and the onboarding team) may do anything. A provider is
 * managed by its users, a service by the users of its provider, and a datasource by those who manage its service.
 * Only a user with an email is signed in, since access is granted by email.
 */
@Component("access")
public class AccessRules {

    private final BundleService bundles;

    public AccessRules(BundleService bundles) {
        this.bundles = bundles;
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

    public boolean isReviewer(Authentication auth) {
        return isSignedIn(auth) && Roles.isReviewer(auth);
    }

    /**
     * Whether the user may change the record with the given id. False if the record does not exist.
     */
    public boolean canManage(Authentication auth, ResourceKind kind, Object id) {
        if (!isSignedIn(auth)) {
            return false;
        }
        if (Roles.isReviewer(auth)) {
            return true;
        }
        if (id == null) {
            return false;
        }
        return isManager(auth, kind, id.toString());
    }

    private boolean isManager(Authentication auth, ResourceKind kind, String id) {
        if (id == null) {
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
        if (kind == ResourceKind.PROVIDER || Roles.isReviewer(auth)) {
            return true;
        }
        Object owner = payload != null ? payload.get(BundleService.ownerField(kind)) : null;
        if (owner == null) {
            return false;
        }
        return kind == ResourceKind.SERVICE
                ? isProviderUser(auth, owner.toString())
                : isManager(auth, ResourceKind.SERVICE, owner.toString());
    }

    /**
     * Whether the user may read the record: an active record is public, others need {@link #canManage}.
     */
    public boolean canRead(Authentication auth, ResourceKind kind, Map<String, Object> bundle) {
        return Bundles.active(bundle) || canManage(auth, kind, Bundles.id(bundle));
    }

    private boolean isProviderUser(Authentication auth, String providerId) {
        if (providerId == null) {
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
