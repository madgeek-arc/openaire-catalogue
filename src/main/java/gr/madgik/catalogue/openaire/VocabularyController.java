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

package gr.madgik.catalogue.openaire;

import gr.madgik.catalogue.openaire.domain.Vocabulary;
import gr.madgik.catalogue.openaire.dto.BulkOperation;
import gr.madgik.catalogue.openaire.dto.VocabularyTree;
import gr.madgik.catalogue.openaire.vocabulary.service.VocabularyOperations;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "vocabularies", produces = MediaType.APPLICATION_JSON_VALUE)
public class VocabularyController extends AbstractResourceController<Vocabulary> {

    private final VocabularyOperations vocabularyService;

    public VocabularyController(VocabularyOperations vocabularyService) {
        super(vocabularyService);
        this.vocabularyService = vocabularyService;
    }

    @PostMapping("bulk")
    ResponseEntity<BulkOperation<Vocabulary>> addMany(@RequestBody List<Vocabulary> vocabularyList) {
        return new ResponseEntity<>(vocabularyService.bulkAdd(vocabularyList), HttpStatus.OK);
    }

    @Operation(summary = "Returns a list of Vocabulary types")
    @GetMapping(path = "types", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<String>> getVocabularyTypes() {
        return new ResponseEntity<>(vocabularyService.getTypes(), HttpStatus.OK);
    }

    @GetMapping(path = "vocabularyTree/{type}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<VocabularyTree> getVocabularyTree(@PathVariable("type") String type) {
        return new ResponseEntity<>(vocabularyService.getVocabulariesTree(type), HttpStatus.OK);
    }

    @Operation(summary = "Get a Map of vocabulary types and their respective entries")
    @GetMapping(path = "/byType", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, List<Vocabulary>>> getByType() {
        return new ResponseEntity<>(vocabularyService.getByType(), HttpStatus.OK);
    }

    @Operation(summary = "Get vocabularies by type")
    @GetMapping(path = "/byType/{type}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Vocabulary>> getByType(@PathVariable(value = "type") String type) {
        return new ResponseEntity<>(vocabularyService.getByType(type), HttpStatus.OK);
    }
}
