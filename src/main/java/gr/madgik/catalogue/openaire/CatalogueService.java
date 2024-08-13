package gr.madgik.catalogue.openaire;

import gr.madgik.catalogue.openaire.repository.CatalogueRepository;
import gr.uoa.di.madgik.resourcecatalogue.domain.Catalogue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CatalogueService extends AbstractResourceOperations<Catalogue> {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueService.class);

    private final CatalogueRepository catalogueRepository;

    public CatalogueService(CatalogueRepository catalogueRepository) {
        super(catalogueRepository);
        this.catalogueRepository = catalogueRepository;
    }

    @Override
    public boolean validate(Catalogue catalogue) {
        return false;
    }
}
