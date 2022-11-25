package gr.madgik.catalogue.openaire.domain;

import eu.einfracentral.domain.*;
import org.json.simple.JSONObject;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URL;
import java.util.Date;
import java.util.List;

@XmlType(name = "service")
@XmlRootElement(namespace = "http://openaire.eu")
public class Service extends eu.einfracentral.domain.Service {

    @XmlElement
    private JSONObject extras;

    public Service() {
    }

    public Service(String id, String abbreviation, String name, String resourceOrganisation, List<String> resourceProviders, URL webpage, String description, String tagline, URL logo, List<MultimediaPair> multimedia, List<UseCasesPair> useCases, List<ServiceProviderDomain> scientificDomains, List<ServiceCategory> categories, List<String> targetUsers, List<String> accessTypes, List<String> accessModes, List<String> tags, List<String> geographicalAvailabilities, List<String> languageAvailabilities, List<String> resourceGeographicLocations, ServiceMainContact mainContact, List<ServicePublicContact> publicContacts, String helpdeskEmail, String securityContactEmail, String trl, String lifeCycleStatus, List<String> certifications, List<String> standards, List<String> openSourceTechnologies, String version, Date lastUpdate, List<String> changeLog, List<String> requiredResources, List<String> relatedResources, List<String> relatedPlatforms, String catalogueId, List<String> fundingBody, List<String> fundingPrograms, List<String> grantProjectNames, URL helpdeskPage, URL userManual, URL termsOfUse, URL privacyPolicy, URL accessPolicy, URL resourceLevel, URL trainingInformation, URL statusMonitoring, URL maintenance, String orderType, URL order, URL paymentModel, URL pricing, JSONObject extras) {
        super(id, abbreviation, name, resourceOrganisation, resourceProviders, webpage, description, tagline, logo, multimedia, useCases, scientificDomains, categories, targetUsers, accessTypes, accessModes, tags, geographicalAvailabilities, languageAvailabilities, resourceGeographicLocations, mainContact, publicContacts, helpdeskEmail, securityContactEmail, trl, lifeCycleStatus, certifications, standards, openSourceTechnologies, version, lastUpdate, changeLog, requiredResources, relatedResources, relatedPlatforms, catalogueId, fundingBody, fundingPrograms, grantProjectNames, helpdeskPage, userManual, termsOfUse, privacyPolicy, accessPolicy, resourceLevel, trainingInformation, statusMonitoring, maintenance, orderType, order, paymentModel, pricing);
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
        if (!(o instanceof Service that)) return false;
        if (!super.equals(o)) return false;

        return getExtras() != null ? getExtras().equals(that.getExtras()) : that.getExtras() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getExtras() != null ? getExtras().hashCode() : 0);
        return result;
    }
}
