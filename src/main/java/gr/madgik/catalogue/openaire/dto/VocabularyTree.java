package gr.madgik.catalogue.openaire.dto;

import gr.madgik.catalogue.openaire.domain.Vocabulary;
import jakarta.xml.bind.annotation.XmlTransient;

import java.util.List;

@XmlTransient
public class VocabularyTree {

    private Vocabulary vocabulary = null;
    private List<VocabularyTree> children = null;

    public VocabularyTree() {
    }

    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(Vocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }

    public List<VocabularyTree> getChildren() {
        return children;
    }

    public void setChildren(List<VocabularyTree> children) {
        this.children = children;
    }
}
