package eu.openaire.catalogue.api;

import eu.openaire.catalogue.config.OpenAIREProperties;
import eu.openaire.catalogue.invitations.InvitationService;
import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.Channels;
import eu.openaire.catalogue.security.AccessRules;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"oa/providers", "oa/organisations"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "providers (OpenAIRE)", description = "Providers on the oa channel")
public class OaProviderController extends ProviderController {

    public OaProviderController(BundleService bundles, AccessRules access, InvitationService invitations,
                                OpenAIREProperties properties) {
        super(Channels.OA, bundles, access, invitations, properties);
    }
}
