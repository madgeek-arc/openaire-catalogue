package gr.madgik.catalogue.openaire.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.*;
import org.json.simple.JSONObject;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.Date;
import java.util.List;

@XmlType(name = "service")
@XmlRootElement(namespace = "http://openaire.eu")
public class Datasource extends eu.einfracentral.domain.Datasource {

    @XmlElement
    private JSONObject extras;

    public Datasource() {
    }

    public Datasource(String id, String serviceId, String catalogueId, URL submissionPolicyURL, URL preservationPolicyURL, Boolean versionControl, List<PersistentIdentitySystem> persistentIdentitySystems, String jurisdiction, String datasourceClassification, List<String> researchEntityTypes, Boolean thematic, List<ResearchProductLicensing> researchProductLicensings, List<String> researchProductAccessPolicies, ResearchProductMetadataLicensing researchProductMetadataLicensing, List<String> researchProductMetadataAccessPolicies, Boolean harvestable, JSONObject extras) {
        super(id, serviceId, catalogueId, submissionPolicyURL, preservationPolicyURL, versionControl, persistentIdentitySystems, jurisdiction, datasourceClassification, researchEntityTypes, thematic, researchProductLicensings, researchProductAccessPolicies, researchProductMetadataLicensing, researchProductMetadataAccessPolicies, harvestable);
        this.extras = extras;
    }

    public JSONObject getExtras() {
        return extras;
    }

    public void setExtras(JSONObject extras) {
        this.extras = extras;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Datasource)) return false;
        if (!super.equals(o)) return false;

        Datasource that = (Datasource) o;

        return getExtras() != null ? getExtras().equals(that.getExtras()) : that.getExtras() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getExtras() != null ? getExtras().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
