package gr.madgik.catalogue.openaire.security;

import gr.uoa.di.madgik.resourcecatalogue.extension.EntitlementAuthorityMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Authorities are derived from the email lists only; entitlement claims grant nothing.
 */
@Component
public class OpenAIREEntitlementAuthorityMapper implements EntitlementAuthorityMapper {

    @Override
    public Set<GrantedAuthority> map(Map<String, Object> attributes) {
        return Set.of();
    }
}
