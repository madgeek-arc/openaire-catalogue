package eu.openaire.catalogue.api;

import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.Channels;
import eu.openaire.catalogue.security.AccessRules;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "node/datasources", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "datasources (Node)", description = "Datasources on the node channel")
public class NodeDatasourceController extends DatasourceController {

    public NodeDatasourceController(BundleService bundles, AccessRules access) {
        super(Channels.NODE, bundles, access);
    }
}
