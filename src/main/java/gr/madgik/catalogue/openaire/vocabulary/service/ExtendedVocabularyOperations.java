package gr.madgik.catalogue.openaire.vocabulary.service;


import gr.madgik.catalogue.openaire.dto.Value;

import java.util.List;
import java.util.Map;

public interface ExtendedVocabularyOperations extends VocabularyOperations {

    Map<String, List<Value>> getControlValuesMap();
}
