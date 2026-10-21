package eu.openaire.catalogue.security;

import gr.uoa.di.madgik.resourcecatalogue.config.security.InMemoryAuthoritiesMapper;
import gr.uoa.di.madgik.resourcecatalogue.service.AuthoritiesMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Resolves authorities of administrators ({@code catalogue.admins}) and the onboarding team
 * ({@code catalogue.onboarding-team}). Besides the {@code ROLE_ADMIN} and {@code ROLE_EPOT} authorities,
 * users receive the plain {@code ADMIN} and {@code ONBOARDING_TEAM} authorities that OpenAIRE clients
 * check for.
 */
@Primary
@Component
public class OpenAIREAuthoritiesMapper implements AuthoritiesMapper {

    static final String ADMIN = "ADMIN";
    static final String ONBOARDING_TEAM = "ONBOARDING_TEAM";

    private final InMemoryAuthoritiesMapper delegate;

    public OpenAIREAuthoritiesMapper(InMemoryAuthoritiesMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isAdmin(String email) {
        return delegate.isAdmin(email);
    }

    @Override
    public boolean isEPOT(String email) {
        return delegate.isEPOT(email);
    }

    @Override
    public Set<GrantedAuthority> getAuthorities(String email) {
        Set<GrantedAuthority> authorities = new HashSet<>(delegate.getAuthorities(email));
        if (delegate.isAdmin(email)) {
            authorities.add(new SimpleGrantedAuthority(ADMIN));
        }
        if (delegate.isEPOT(email)) {
            authorities.add(new SimpleGrantedAuthority(ONBOARDING_TEAM));
        }
        return authorities;
    }

    @Override
    public void updateAuthorities() {
        delegate.updateAuthorities();
    }
}
