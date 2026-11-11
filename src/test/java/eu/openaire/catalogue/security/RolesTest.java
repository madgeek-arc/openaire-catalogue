package eu.openaire.catalogue.security;

import eu.openaire.catalogue.config.OpenAIREProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RolesTest {

    private final OpenAIREProperties openaire = new OpenAIREProperties();
    private final OpenAIREProperties.RoleSettings settings = openaire.getRoles();

    @Test
    void grantsRolesByEmail() {
        settings.getAdmin().setEmails(List.of("Admin@example.org"));
        settings.getOnboardingTeam().setEmails(List.of("onboarding@example.org"));
        settings.setChannels(Map.of("node", scope(List.of("node@example.org"), List.of())));
        Roles roles = new Roles(openaire);

        assertEquals(Set.of("ROLE_USER", "ROLE_ADMIN"), names(roles.authorities(" admin@EXAMPLE.org")));
        assertEquals(Set.of("ROLE_USER", "ROLE_ONBOARDING_TEAM"), names(roles.authorities("onboarding@example.org")));
        assertEquals(Set.of("ROLE_USER", "ROLE_ADMIN_NODE"), names(roles.authorities("node@example.org")));
        assertEquals(Set.of("ROLE_USER"), names(roles.authorities("someone@example.org")));
    }

    @Test
    void grantsRolesFromIdentityProviderClaims() {
        settings.setIdpRoleClaims(List.of("realm_access.roles", "resource_access.{client}.roles", "missing.path"));
        settings.setChannels(Map.of("oa", scope(List.of(), List.of("catalogue-oa-admin"))));
        Roles roles = new Roles(openaire);
        Map<String, Object> claims = Map.of(
                "realm_access", Map.of("roles", List.of("offline_access", "Super Administrator")),
                "resource_access", Map.of(
                        "catalogue.client", Map.of("roles", List.of("catalogue-oa-admin")),
                        "other", Map.of("roles", List.of("ignored"))));

        Set<String> idpRoles = roles.idpRoles(claims, "catalogue.client");

        assertEquals(Set.of("offline_access", "Super Administrator", "catalogue-oa-admin"), idpRoles);
        assertEquals(Set.of("ROLE_USER", "ROLE_ADMIN", "ROLE_ADMIN_OA"),
                names(roles.authorities("someone@example.org", idpRoles)));
        assertEquals(Set.of(), roles.idpRoles(claims, null).stream()
                .filter("catalogue-oa-admin"::equals).collect(Collectors.toSet()));
    }

    @Test
    void grantsConfiguredIdentityProviderRolesInsteadOfTheDefault() {
        settings.getAdmin().setIdpRoles(List.of("Catalogue Admins"));
        settings.getOnboardingTeam().setIdpRoles(List.of("Onboarding"));
        Roles roles = new Roles(openaire);

        assertEquals(Set.of("ROLE_USER"), names(roles.authorities("someone@example.org", Set.of("Super Administrator"))));
        assertEquals(Set.of("ROLE_USER", "ROLE_ADMIN"),
                names(roles.authorities("someone@example.org", Set.of("Catalogue Admins"))));
        assertEquals(Set.of("ROLE_USER", "ROLE_ONBOARDING_TEAM"),
                names(roles.authorities("someone@example.org", Set.of("Onboarding"))));
    }

    @Test
    void readsOpenAIREEntitlementsAsGroupRoles() {
        settings.setIdpRoleClaims(List.of("eduperson_entitlement"));
        settings.setChannels(Map.of("oa", scope(List.of(), List.of("Catalogue Curators"))));
        Roles roles = new Roles(openaire);
        String superAdministrator = "urn:geant:openaire.eu:group:Super+Administrator:role=member#aai.openaire.eu";
        String curators = "urn:geant:openaire.eu:group:Catalogue%20Curators:role=member#aai.openaire.eu";
        String subgroup = "urn:geant:openaire.eu:group:Catalogue:oa:role=member#aai.openaire.eu";
        String manager = "urn:geant:openaire.eu:group:Other:role=manager#aai.openaire.eu";
        String foreign = "urn:geant:example.org:group:Other:role=member#aai.example.org";

        Set<String> idpRoles = roles.idpRoles(Map.of("eduperson_entitlement",
                List.of(superAdministrator, curators, subgroup, manager, foreign)), null);

        assertEquals(Set.of(superAdministrator, "Super Administrator", curators, "Catalogue Curators", subgroup,
                manager, foreign), idpRoles);
        assertEquals(Set.of("ROLE_USER", "ROLE_ADMIN", "ROLE_ADMIN_OA"),
                names(roles.authorities("someone@example.org", idpRoles)));
    }

    @Test
    void refusesChannelRolesForUnknownChannels() {
        settings.setChannels(Map.of("global", scope(List.of("x@example.org"), List.of())));
        assertThrows(IllegalStateException.class, () -> new Roles(openaire));
    }

    private static OpenAIREProperties.RoleScope scope(List<String> emails, List<String> idpRoles) {
        OpenAIREProperties.RoleScope scope = new OpenAIREProperties.RoleScope();
        scope.setEmails(emails);
        scope.setIdpRoles(idpRoles);
        return scope;
    }

    private static Set<String> names(Set<GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }
}
