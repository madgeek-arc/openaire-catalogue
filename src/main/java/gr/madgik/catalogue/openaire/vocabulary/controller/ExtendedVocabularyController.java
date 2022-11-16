package gr.madgik.catalogue.openaire.vocabulary.controller;

import gr.madgik.catalogue.dto.Value;
import gr.madgik.catalogue.openaire.vocabulary.service.ExtendedVocabularyOperations;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ExtendedVocabularyController {

    private final ExtendedVocabularyOperations vocabularyService;

    public ExtendedVocabularyController(ExtendedVocabularyOperations vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    @GetMapping(value = "vocabularies/mappings", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<Value>> getControlValuesByType() {
        return vocabularyService.getControlValuesMap();
    }

}
