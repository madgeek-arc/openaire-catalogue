package gr.madgik.catalogue.openaire.dto;

import eu.einfracentral.domain.Vocabulary;

public class UiVocabulary {

    private String id;
    private String name;

    public UiVocabulary() {}

    public UiVocabulary of(Vocabulary vocabulary) {
        UiVocabulary uiVocabulary = new UiVocabulary();
        uiVocabulary.setId(vocabulary.getId());
        uiVocabulary.setName(vocabulary.getName());
        return uiVocabulary;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
