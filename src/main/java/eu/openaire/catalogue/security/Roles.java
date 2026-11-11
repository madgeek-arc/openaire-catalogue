package eu.openaire.catalogue.security;

import eu.openaire.catalogue.config.OpenAIREProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Grants roles by email and by identity provider role. Administrators get {@code ROLE_ADMIN}, administrators of one
 * channel {@code ROLE_ADMIN_<CHANNEL>}, the onboarding team {@code ROLE_ONBOARDING_TEAM}, and every signed-in user
 * {@code ROLE_USER}. Who gets each role is set by email or identity provider role in {@code openaire.roles.admin},
 * {@code openaire.roles.onboarding-team} and {@code openaire.roles.channels.<channel>}.
 * Authorities are fixed when the user signs in, so a role change applies from the next sign-in.
 */
@Component
public class Roles {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_ONBOARDING_TEAM = "ROLE_ONBOARDING_TEAM";
    public static final String ROLE_USER = "ROLE_USER";

    private static final String CLIENT_SEGMENT = "{client}";

    /**
     * An OpenAIRE AAI entitlement of a group member; the group name is URL-encoded.
     */
    // TODO: map subgroup entitlements once their form is known
    private static final Pattern ENTITLEMENT =
            Pattern.compile("urn:geant:openaire[.]eu:group:([^:]+):role=member#aai[.]openaire[.]eu");

    private final OpenAIREProperties openaire;

    /**
     * @throws IllegalStateException if a key of {@code openaire.roles.channels} is not a channel
     */
    public Roles(OpenAIREProperties openaire) {
        this.openaire = openaire;
        for (String channel : openaire.getRoles().getChannels().keySet()) {
            if (!openaire.getChannels().contains(channel)) {
                throw new IllegalStateException("openaire.roles.channels." + channel
                        + " is not one of openaire.channels " + openaire.getChannels());
            }
        }
    }

    /**
     * The authority of the administrators of a channel.
     */
    public static String channelAdmin(String channel) {
        return "ROLE_ADMIN_" + channel.toUpperCase(Locale.ROOT);
    }

    public Set<GrantedAuthority> authorities(String email) {
        return authorities(email, Set.of());
    }

    /**
     * @param idpRoles the roles the user has at the identity provider
     */
    public Set<GrantedAuthority> authorities(String email, Collection<String> idpRoles) {
        OpenAIREProperties.RoleSettings settings = openaire.getRoles();
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority(ROLE_USER));
        if (inScope(settings.getAdmin(), email, idpRoles)) {
            authorities.add(new SimpleGrantedAuthority(ROLE_ADMIN));
        }
        if (inScope(settings.getOnboardingTeam(), email, idpRoles)) {
            authorities.add(new SimpleGrantedAuthority(ROLE_ONBOARDING_TEAM));
        }
        for (String channel : openaire.getChannels()) {
            if (inScope(settings.getChannels().get(channel), email, idpRoles)) {
                authorities.add(new SimpleGrantedAuthority(channelAdmin(channel)));
            }
        }
        return authorities;
    }

    /**
     * The roles found at the configured claim paths ({@code openaire.roles.idp-role-claims}) of the given claims. An
     * OpenAIRE AAI entitlement of a group member, such as
     * {@code urn:geant:openaire.eu:group:Super+Administrator:role=member#aai.openaire.eu}, also gives the decoded group
     * name as a role ({@code Super Administrator}).
     *
     * @param clientId the value of a {@code {client}} path segment
     */
    public Set<String> idpRoles(Map<String, Object> claims, String clientId) {
        Set<String> roles = new LinkedHashSet<>();
        for (String path : openaire.getRoles().getIdpRoleClaims()) {
            Object value = claims;
            for (String segment : path.split("\\.")) {
                String key = CLIENT_SEGMENT.equals(segment) ? clientId : segment;
                value = value instanceof Map<?, ?> map && key != null ? map.get(key) : null;
            }
            if (value instanceof Collection<?> values) {
                values.stream().filter(v -> v != null).map(String::valueOf).forEach(role -> add(roles, role));
            } else if (value instanceof String role) {
                add(roles, role);
            }
        }
        return roles;
    }

    private static void add(Set<String> roles, String role) {
        roles.add(role);
        Matcher entitlement = ENTITLEMENT.matcher(role);
        if (entitlement.matches()) {
            roles.add(URLDecoder.decode(entitlement.group(1), StandardCharsets.UTF_8));
        }
    }

    /**
     * Whether the user reviews every record: an administrator or a member of the onboarding team.
     */
    public static boolean isGlobalReviewer(Authentication auth) {
        return has(auth, ROLE_ADMIN) || has(auth, ROLE_ONBOARDING_TEAM);
    }

    /**
     * The channels whose records the user administers, without the administrators of everything.
     */
    public Set<String> adminChannels(Authentication auth) {
        Set<String> channels = new LinkedHashSet<>();
        for (String channel : openaire.getChannels()) {
            if (has(auth, channelAdmin(channel))) {
                channels.add(channel);
            }
        }
        return channels;
    }

    public static boolean has(Authentication auth, String authority) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private static boolean inScope(OpenAIREProperties.RoleScope scope, String email, Collection<String> idpRoles) {
        if (scope == null) {
            return false;
        }
        return contains(scope.getEmails(), email) || scope.getIdpRoles().stream().anyMatch(idpRoles::contains);
    }

    private static boolean contains(List<String> emails, String email) {
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
