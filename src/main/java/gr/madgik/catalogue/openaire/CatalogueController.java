package gr.madgik.catalogue.openaire;

import gr.uoa.di.madgik.resourcecatalogue.domain.Catalogue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("catalogues")
public class CatalogueController extends AbstractResourceController<Catalogue> {

    CatalogueController(ResourceOperations<Catalogue, String> service) {
        super(service);
    }
}
