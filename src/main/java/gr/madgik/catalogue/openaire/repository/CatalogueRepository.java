package gr.madgik.catalogue.openaire.repository;

import gr.uoa.di.madgik.resourcecatalogue.domain.Catalogue;
import gr.athenarc.catalogue.service.GenericItemService;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogueRepository extends RegistryCoreRepository<Catalogue, String> {

    public CatalogueRepository(GenericItemService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "catalogue";
    }
}
