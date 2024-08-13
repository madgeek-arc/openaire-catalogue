package gr.madgik.catalogue.openaire.vocabulary.service;

import gr.madgik.catalogue.openaire.domain.Vocabulary;
import gr.madgik.catalogue.openaire.ResourceOperations;
import gr.madgik.catalogue.openaire.dto.VocabularyTree;

import java.util.List;
import java.util.Map;

public interface VocabularyOperations extends ResourceOperations<Vocabulary, String> {

    List<Vocabulary> getByType(String type);

    Map<String, List<Vocabulary>> getByType();

    List<String> getTypes();

    VocabularyTree getVocabulariesTree(String type);

    Vocabulary getParent(String childId);
}
