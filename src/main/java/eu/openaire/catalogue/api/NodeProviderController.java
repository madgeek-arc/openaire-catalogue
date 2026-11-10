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
@RequestMapping(path = {"node/providers", "node/organisations"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "providers (Node)", description = "Providers on the node channel")
public class NodeProviderController extends ProviderController {

    public NodeProviderController(BundleService bundles, AccessRules access, InvitationService invitations,
                                  OpenAIREProperties properties) {
        super(Channels.NODE, bundles, access, invitations, properties);
    }
}
