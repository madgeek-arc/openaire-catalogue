package gr.madgik.catalogue.openaire.domain;

import java.net.URL;

public class EOSCIFGuidelines {

    private String pid;

    private String label;

    private URL url;

    private String semanticRelationship;

    public EOSCIFGuidelines() {
    }

    public EOSCIFGuidelines(String pid, String label, URL url, String semanticRelationship) {
        this.pid = pid;
        this.label = label;
        this.url = url;
        this.semanticRelationship = semanticRelationship;
    }

    @Override
    public String toString() {
        return "EOSCIFGuidelines{" +
                "pid='" + pid + '\'' +
                ", label='" + label + '\'' +
                ", url=" + url +
                ", semanticRelationship='" + semanticRelationship + '\'' +
                '}';
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getSemanticRelationship() {
        return semanticRelationship;
    }

    public void setSemanticRelationship(String semanticRelationship) {
        this.semanticRelationship = semanticRelationship;
    }
}
