package eu.openaire.catalogue.api;

import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.Channels;
import eu.openaire.catalogue.security.AccessRules;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "oa/services", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "services (OpenAIRE)", description = "Services on the oa channel")
public class OaServiceController extends ServiceController {

    public OaServiceController(BundleService bundles, AccessRules access) {
        super(Channels.OA, bundles, access);
    }
}
