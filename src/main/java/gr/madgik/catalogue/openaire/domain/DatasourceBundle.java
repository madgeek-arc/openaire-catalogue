package gr.madgik.catalogue.openaire.domain;

import eu.einfracentral.domain.Metadata;
import eu.einfracentral.domain.ResourceBundle;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://openaire.eu")
public class DatasourceBundle extends ResourceBundle<Datasource> {

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

    @Override
    public String toString() {
        return "DatasourceBundle{} " + super.toString();
    }
}
