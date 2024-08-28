package gr.madgik.catalogue.openaire.vocabulary;

import gr.athenarc.catalogue.service.GenericItemService;
import gr.madgik.catalogue.openaire.domain.Vocabulary;
import gr.madgik.catalogue.openaire.repository.RegistryCoreRepository;
import org.springframework.stereotype.Repository;

@Repository
public class VocabularyRepository extends RegistryCoreRepository<Vocabulary, String> {

    public VocabularyRepository(GenericItemService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "vocabulary";
    }
}
