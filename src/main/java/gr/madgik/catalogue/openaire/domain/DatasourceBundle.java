package gr.madgik.catalogue.openaire.domain;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.Metadata;
import eu.einfracentral.domain.ResourceExtras;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://openaire.eu")
public class DatasourceBundle extends Bundle<Datasource> {

    @XmlElement
    private String status;

    @XmlElement
    @FieldValidation(nullable = true)
    private ResourceExtras resourceExtras;

    public DatasourceBundle() {
        // No arg constructor
    }

    public DatasourceBundle(Datasource datasource) {
        this.setDatasource(datasource);
        this.setMetadata(null);
    }

    public DatasourceBundle(Datasource datasource, Metadata metadata) {
        this.setDatasource(datasource);
        this.setMetadata(metadata);
    }

    @XmlElement(name = "datasource")
    public Datasource getDatasource() {
        return this.getPayload();
    }

    public void setDatasource(Datasource datasource) {
        this.setPayload(datasource);
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ResourceExtras getResourceExtras() {
        return resourceExtras;
    }

    public void setResourceExtras(ResourceExtras resourceExtras) {
        this.resourceExtras = resourceExtras;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
