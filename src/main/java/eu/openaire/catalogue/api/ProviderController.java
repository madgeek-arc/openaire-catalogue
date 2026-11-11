package eu.openaire.catalogue.api;

import eu.openaire.catalogue.config.OpenAIREProperties;
import eu.openaire.catalogue.invitations.InvitationService;
import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.Bundles;
import eu.openaire.catalogue.resource.ResourceKind;
import eu.openaire.catalogue.security.AccessRules;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider routes of one channel.
 */
public abstract class ProviderController extends RecordController {

    private final InvitationService invitations;
    private final OpenAIREProperties properties;

    protected ProviderController(String channel, BundleService bundles, AccessRules access,
                                 InvitationService invitations, OpenAIREProperties properties) {
        super(ResourceKind.PROVIDER, channel, bundles, access);
        this.invitations = invitations;
        this.properties = properties;
    }

    @Operation(summary = "Returns the providers the signed-in user manages, whatever their status or channels.")
    @GetMapping("my")
    @PreAuthorize("@access.isSignedIn(authentication)")
    public List<Map<String, Object>> getMy(@Parameter(hidden = true) Authentication auth) {
        String email = UserInfo.of(auth).email();
        return bundles.all(kind, Map.of()).stream()
                .filter(bundle -> Bundles.userEmails(Bundles.payload(bundle, kind)).contains(email))
                .map(bundle -> Bundles.payload(bundle, kind))
                .toList();
    }

    /**
     * Lists providers by name.
     */
    @Override
    protected void defaultOrder(FacetFilter filter) {
        filter.addOrderBy("name", "asc");
    }

    /**
     * Leaves out the provider users, whose emails are personal data.
     */
    @Override
    protected Map<String, Object> publicView(Map<String, Object> payload) {
        Map<String, Object> view = new LinkedHashMap<>(payload);
        view.remove("users");
        return view;
    }

    /**
     * Users outside the reviewers need a valid invitation, sent for their email, to register a provider.
     */
    @Override
    protected void beforeAdd(Map<String, Object> payload, MultiValueMap<String, String> params, Authentication auth) {
        if (!properties.getInvitations().isRequired() || access.isReviewer(auth)) {
            return;
        }
        String token = params.getFirst(properties.getInvitations().getParameter());
        if (token == null || token.isBlank()) {
            throw new ResourceException("An invitation is required to register a provider", HttpStatus.FORBIDDEN);
        }
        invitations.verify(token, UserInfo.of(auth).email());
    }
}
