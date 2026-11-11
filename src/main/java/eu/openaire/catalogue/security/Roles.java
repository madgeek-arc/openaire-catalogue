package eu.openaire.catalogue.security;

import eu.openaire.catalogue.config.CatalogueProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Grants authorities by email: administrators get {@code ROLE_ADMIN} and {@code ADMIN}, the onboarding team
 * {@code ROLE_EPOT} and {@code ONBOARDING_TEAM}, and every signed-in user {@code ROLE_USER}.
 */
@Component
public class Roles {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_EPOT = "ROLE_EPOT";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ADMIN = "ADMIN";
    public static final String ONBOARDING_TEAM = "ONBOARDING_TEAM";

    private final CatalogueProperties properties;

    public Roles(CatalogueProperties properties) {
        this.properties = properties;
    }

    public Set<GrantedAuthority> authorities(String email) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority(ROLE_USER));
        if (isAdmin(email)) {
            authorities.add(new SimpleGrantedAuthority(ROLE_ADMIN));
            authorities.add(new SimpleGrantedAuthority(ADMIN));
        }
        if (isOnboardingTeam(email)) {
            authorities.add(new SimpleGrantedAuthority(ROLE_EPOT));
            authorities.add(new SimpleGrantedAuthority(ONBOARDING_TEAM));
        }
        return authorities;
    }

    public boolean isAdmin(String email) {
        return contains(properties.getAdmins(), email);
    }

    public boolean isOnboardingTeam(String email) {
        return contains(properties.getOnboardingTeam(), email);
    }

    /**
     * Whether the user may review registrations: an administrator or a member of the onboarding team.
     */
    public static boolean isReviewer(Authentication auth) {
        return has(auth, ROLE_ADMIN) || has(auth, ROLE_EPOT);
    }

    public static boolean has(Authentication auth, String authority) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private static boolean contains(Iterable<String> emails, String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalised = email.trim().toLowerCase(Locale.ROOT);
        for (String candidate : emails) {
            if (candidate != null && normalised.equals(candidate.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
