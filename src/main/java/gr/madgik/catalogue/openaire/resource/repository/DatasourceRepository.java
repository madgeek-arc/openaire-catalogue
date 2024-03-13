package gr.madgik.catalogue.openaire.resource.repository;

import eu.einfracentral.domain.DatasourceBundle;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.madgik.catalogue.repository.RegistryCoreRepository;
import gr.madgik.catalogue.repository.Repository;
import org.springframework.stereotype.Component;

@Component
public class DatasourceRepository extends RegistryCoreRepository<DatasourceBundle, String> implements Repository<DatasourceBundle, String> {


    public DatasourceRepository(GenericItemService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "datasource";
    }
}
