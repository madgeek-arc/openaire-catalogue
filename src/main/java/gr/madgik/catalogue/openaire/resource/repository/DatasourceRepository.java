package gr.madgik.catalogue.openaire.resource.repository;

import gr.madgik.catalogue.openaire.domain.DatasourceBundle;
import gr.madgik.catalogue.openaire.repository.RegistryCoreRepository;
import gr.madgik.catalogue.openaire.repository.Repository;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import org.springframework.stereotype.Component;

@Component
public class DatasourceRepository extends RegistryCoreRepository<DatasourceBundle, String> implements Repository<DatasourceBundle, String> {


    public DatasourceRepository(GenericResourceService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "datasource";
    }
}
