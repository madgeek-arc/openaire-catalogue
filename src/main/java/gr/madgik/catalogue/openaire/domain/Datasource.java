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

    public Datasource(URL submissionPolicyURL, URL preservationPolicyURL, boolean versionControl, List<PersistentIdentitySystem> persistentIdentitySystems, String jurisdiction, String datasourceClassification, List<String> researchEntityTypes, boolean thematic, List<ResearchProductLicensing> researchProductLicensings, List<String> researchProductAccessPolicies, ResearchProductMetadataLicensing researchProductMetadataLicensing, List<String> researchProductMetadataAccessPolicies, JSONObject extras) {
        super(submissionPolicyURL, preservationPolicyURL, versionControl, persistentIdentitySystems, jurisdiction, datasourceClassification, researchEntityTypes, thematic, researchProductLicensings, researchProductAccessPolicies, researchProductMetadataLicensing, researchProductMetadataAccessPolicies);
        this.extras = extras;
    }

    public Datasource(String id, String abbreviation, String name, String resourceOrganisation, List<String> resourceProviders, URL webpage, String description, String tagline, URL logo, List<MultimediaPair> multimedia, List<UseCasesPair> useCases, List<ServiceProviderDomain> scientificDomains, List<ServiceCategory> categories, List<String> targetUsers, List<String> accessTypes, List<String> accessModes, List<String> tags, List<String> geographicalAvailabilities, List<String> languageAvailabilities, List<String> resourceGeographicLocations, ServiceMainContact mainContact, List<ServicePublicContact> publicContacts, String helpdeskEmail, String securityContactEmail, String trl, String lifeCycleStatus, List<String> certifications, List<String> standards, List<String> openSourceTechnologies, String version, Date lastUpdate, List<String> changeLog, List<String> requiredResources, List<String> relatedResources, List<String> relatedPlatforms, String catalogueId, List<String> fundingBody, List<String> fundingPrograms, List<String> grantProjectNames, URL helpdeskPage, URL userManual, URL termsOfUse, URL privacyPolicy, URL accessPolicy, URL resourceLevel, URL trainingInformation, URL statusMonitoring, URL maintenance, String orderType, URL order, URL paymentModel, URL pricing, URL submissionPolicyURL, URL preservationPolicyURL, boolean versionControl, List<PersistentIdentitySystem> persistentIdentitySystems, String jurisdiction, String datasourceClassification, List<String> researchEntityTypes, boolean thematic, List<ResearchProductLicensing> researchProductLicensings, List<String> researchProductAccessPolicies, ResearchProductMetadataLicensing researchProductMetadataLicensing, List<String> researchProductMetadataAccessPolicies, JSONObject extras) {
        super(id, abbreviation, name, resourceOrganisation, resourceProviders, webpage, description, tagline, logo, multimedia, useCases, scientificDomains, categories, targetUsers, accessTypes, accessModes, tags, geographicalAvailabilities, languageAvailabilities, resourceGeographicLocations, mainContact, publicContacts, helpdeskEmail, securityContactEmail, trl, lifeCycleStatus, certifications, standards, openSourceTechnologies, version, lastUpdate, changeLog, requiredResources, relatedResources, relatedPlatforms, catalogueId, fundingBody, fundingPrograms, grantProjectNames, helpdeskPage, userManual, termsOfUse, privacyPolicy, accessPolicy, resourceLevel, trainingInformation, statusMonitoring, maintenance, orderType, order, paymentModel, pricing, submissionPolicyURL, preservationPolicyURL, versionControl, persistentIdentitySystems, jurisdiction, datasourceClassification, researchEntityTypes, thematic, researchProductLicensings, researchProductAccessPolicies, researchProductMetadataLicensing, researchProductMetadataAccessPolicies);
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
