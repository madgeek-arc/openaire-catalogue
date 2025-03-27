/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.madgik.catalogue.openaire.vocabulary.controller;

import gr.madgik.catalogue.openaire.dto.Value;
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
