package gr.madgik.catalogue.openaire.extension;

import gr.madgik.catalogue.openaire.config.OpenAIREProperties;
import gr.madgik.catalogue.openaire.invitations.InvitationService;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.extension.BundleCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * OpenAIRE rules applied around bundle operations:
 * <ul>
 *     <li>organisations can be registered by non-privileged users only with a valid invitation;</li>
 *     <li>services and datasources get a default resource owner, and the {@code resourceOrganisation} and
 *     {@code resourceOwner} properties are kept consistent;</li>
 *     <li>an empty {@code extras} object is dropped;</li>
 *     <li>new services and datasources remain active while their onboarding status is pending.</li>
 * </ul>
 */
@Component
@Order(100)
public class OpenAIREBundleCustomizer implements BundleCustomizer {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIREBundleCustomizer.class);
    private static final String ORGANISATION = "organisation";

    private final OpenAIREProperties properties;
    private final InvitationService invitationService;
    private final GenericResourceService genericResourceService;

    public OpenAIREBundleCustomizer(OpenAIREProperties properties,
                                    InvitationService invitationService,
                                    @Lazy GenericResourceService genericResourceService) {
        this.properties = properties;
        this.invitationService = invitationService;
        this.genericResourceService = genericResourceService;
    }

    @Override
    public boolean supports(String resourceType) {
        return "service".equals(resourceType) || "datasource".equals(resourceType) || ORGANISATION.equals(resourceType);
    }

    @Override
    public void beforeAdd(String resourceType, Bundle bundle, Authentication auth) {
        if (ORGANISATION.equals(resourceType)) {
            requireInvitation(auth);
            return;
        }
        applyResourceDefaults(bundle.getPayload());
        dropEmptyExtras(bundle.getPayload());
    }

    @Override
    public void afterAdd(String resourceType, Bundle bundle, Authentication auth) {
        if (!ORGANISATION.equals(resourceType) && properties.isPendingResourcesActive() && "pending".equals(bundle.getStatus()) && !bundle.isActive()) {
            bundle.setActive(true);
            genericResourceService.update(resourceType, bundle, false);
            logger.info("Activated pending {} '{}'", resourceType, bundle.getId());
        }
    }

    @Override
    public void beforeUpdate(String resourceType, Bundle bundle, Bundle existing, Authentication auth) {
        if (ORGANISATION.equals(resourceType)) {
            return;
        }
        applyResourceDefaults(bundle.getPayload());
        dropEmptyExtras(bundle.getPayload());
    }

    private void applyResourceDefaults(Map<String, Object> payload) {
        String owner = text(payload.get("resourceOwner"));
        String organisation = text(payload.get("resourceOrganisation"));
        if (owner == null) {
            owner = organisation != null ? organisation : properties.getDefaultResourceOwner();
            payload.put("resourceOwner", owner);
        }
        if (organisation == null) {
            payload.put("resourceOrganisation", owner);
        }
    }

    private void dropEmptyExtras(Map<String, Object> payload) {
        if (payload.get("extras") instanceof Map<?, ?> extras && extras.isEmpty()) {
            payload.remove("extras");
        }
    }

    private void requireInvitation(Authentication auth) {
        if (!properties.getInvitations().isRequired() || isPrivileged(auth)) {
            return;
        }
        invitationService.verify(invitationToken(), UserInfo.of(auth).email());
    }

    private String invitationToken() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servlet) {
            String token = servlet.getRequest().getParameter(properties.getInvitations().getParameter());
            if (token != null && !token.isBlank()) {
                return token;
            }
        }
        throw new gr.uoa.di.madgik.registry.exception.ResourceException(
                "A valid invitation is required to register a provider.", HttpStatus.FORBIDDEN);
    }

    private static boolean isPrivileged(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_EPOT".equals(a.getAuthority()));
    }

    private static String text(Object value) {
        return value instanceof String s && !s.isBlank() ? s : null;
    }
}
