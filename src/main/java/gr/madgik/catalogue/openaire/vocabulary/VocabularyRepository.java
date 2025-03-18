package gr.madgik.catalogue.openaire.vocabulary;

import gr.madgik.catalogue.openaire.domain.Vocabulary;
import gr.madgik.catalogue.openaire.repository.RegistryCoreRepository;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import org.springframework.stereotype.Repository;

@Repository
public class VocabularyRepository extends RegistryCoreRepository<Vocabulary, String> {

    public VocabularyRepository(GenericResourceService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "vocabulary";
    }
}
