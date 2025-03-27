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

package gr.madgik.catalogue.openaire.vocabulary.service;

import gr.madgik.catalogue.openaire.AbstractResourceOperations;
import gr.madgik.catalogue.openaire.domain.Vocabulary;
import gr.madgik.catalogue.openaire.dto.VocabularyTree;
import gr.madgik.catalogue.openaire.vocabulary.VocabularyRepository;
import gr.uoa.di.madgik.catalogue.utils.SortUtils;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VocabularyService extends AbstractResourceOperations<Vocabulary> implements VocabularyOperations {

    public static final Logger logger = LoggerFactory.getLogger(VocabularyService.class);

    public final VocabularyRepository vocabularyRepository;

    public VocabularyService(VocabularyRepository vocabularyRepository) {
        super(vocabularyRepository);
        this.vocabularyRepository = vocabularyRepository;
    }

    @Override
    public boolean validate(Vocabulary vocabulary) {
        return false;
    }

    @Override
    public List<Vocabulary> getByType(String type) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("type", type);
        List<Vocabulary> vocList = get(ff).getResults();
        return vocList.stream().sorted(Comparator.comparing(Vocabulary::getId)).collect(Collectors.toList());
    }

    @Override
//    @Cacheable(value = CACHE_VOCABULARIES)
    public Map<String, List<Vocabulary>> getByType() {
        Map<String, List<Vocabulary>> allVocabularies = new HashMap<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<Vocabulary> all = get(ff).getResults();
        for (Vocabulary entry : all) {
            allVocabularies.putIfAbsent(entry.getType(), new ArrayList<>());
            allVocabularies.get(entry.getType()).add(entry);
        }
        return allVocabularies;
    }

    @Override
    public List<String> getTypes() {
        Set<String> types;
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        types = get(filter).getResults().stream().map(Vocabulary::getType).collect(Collectors.toSet());
        return SortUtils.sort(new ArrayList<>(types));
    }

    //    @Cacheable(value = CACHE_VOCABULARY_TREE)
    @Override
    public VocabularyTree getVocabulariesTree(String type) { // TODO: refactor method
        VocabularyTree root = new VocabularyTree();
        root.setVocabulary(null);
        Map<String, List<Vocabulary>> vocabularies = getByType().entrySet().stream().collect(Collectors.toMap(item -> item.getValue().get(0).getParentId(), Map.Entry::getValue));
        List<VocabularyTree> superTreeList = new ArrayList<>();
        List<Vocabulary> superVocabularies = getByType(type);
        if (superVocabularies != null) {
            for (Vocabulary superVocabulary : superVocabularies) {
                VocabularyTree superTree = new VocabularyTree();
                superTree.setVocabulary(superVocabulary);
                List<VocabularyTree> treeList = new ArrayList<>();
                List<Vocabulary> vocs = vocabularies.get(superVocabulary.getId());
                if (vocs != null) {
                    for (Vocabulary voc : vocs) {
                        VocabularyTree tree = new VocabularyTree();
                        tree.setVocabulary(voc);
                        List<VocabularyTree> subTreeList = new ArrayList<>();
                        List<Vocabulary> subVocabularies = vocabularies.get(voc.getId());
                        if (subVocabularies != null) {
                            for (Vocabulary subVocabulary : subVocabularies) {
                                VocabularyTree subTree = new VocabularyTree();
                                subTree.setVocabulary(subVocabulary);
//                    subTree.setChildren(null);
                                subTreeList.add(subTree);
                            }
                        }
                        tree.setChildren(subTreeList);
                        treeList.add(tree);
                    }
                }
                superTree.setChildren(treeList);
                superTreeList.add(superTree);
            }
        }
        root.setChildren(superTreeList);
        return root;
    }

    @Override
    public Vocabulary getParent(String childId) {
        return get(get(childId).getParentId());
    }
}
