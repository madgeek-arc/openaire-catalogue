package gr.madgik.catalogue.openaire;

import eu.einfracentral.domain.Metadata;
import eu.einfracentral.domain.ResourceBundle;

import javax.xml.bind.annotation.XmlElement;

public class OpenAIREServiceBundle extends ResourceBundle<OpenAIREService> {

    public OpenAIREServiceBundle() {
        // No arg constructor
    }

    public OpenAIREServiceBundle(OpenAIREService service) {
        this.setService(service);
        this.setMetadata(null);
    }

    public OpenAIREServiceBundle(OpenAIREService service, Metadata metadata) {
        this.setService(service);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "service")
    public OpenAIREService getService() {
        return this.getPayload();
    }

    public void setService(OpenAIREService service) {
        this.setPayload(service);
    }

    //    @Id
    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    @Override
    public String toString() {
        return "OpenAIREServiceBundle{} " + super.toString();
    }
}
