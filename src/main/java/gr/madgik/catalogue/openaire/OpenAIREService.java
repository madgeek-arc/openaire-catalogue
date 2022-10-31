package gr.madgik.catalogue.openaire;

import eu.einfracentral.domain.*;
import org.json.simple.JSONObject;

import java.net.URL;
import java.util.Date;
import java.util.List;


public class OpenAIREService extends Service {

    private JSONObject extras;

    public OpenAIREService() {}

    public OpenAIREService(String id, String abbreviation, String name, String resourceOrganisation, List<String> resourceProviders, URL webpage, String description, String tagline, URL logo, List<MultimediaPair> multimedia, List<UseCasesPair> useCases, List<ServiceProviderDomain> scientificDomains, List<ServiceCategory> categories, List<String> targetUsers, List<String> accessTypes, List<String> accessModes, List<String> tags, List<String> geographicalAvailabilities, List<String> languageAvailabilities, List<String> resourceGeographicLocations, ServiceMainContact mainContact, List<ServicePublicContact> publicContacts, String helpdeskEmail, String securityContactEmail, String trl, String lifeCycleStatus, List<String> certifications, List<String> standards, List<String> openSourceTechnologies, String version, Date lastUpdate, List<String> changeLog, List<String> requiredResources, List<String> relatedResources, List<String> relatedPlatforms, String catalogueId, List<String> fundingBody, List<String> fundingPrograms, List<String> grantProjectNames, URL helpdeskPage, URL userManual, URL termsOfUse, URL privacyPolicy, URL accessPolicy, URL resourceLevel, URL trainingInformation, URL statusMonitoring, URL maintenance, String orderType, URL order, URL paymentModel, URL pricing, JSONObject extras) {
        super(id, abbreviation, name, resourceOrganisation, resourceProviders, webpage, description, tagline, logo, multimedia, useCases, scientificDomains, categories, targetUsers, accessTypes, accessModes, tags, geographicalAvailabilities, languageAvailabilities, resourceGeographicLocations, mainContact, publicContacts, helpdeskEmail, securityContactEmail, trl, lifeCycleStatus, certifications, standards, openSourceTechnologies, version, lastUpdate, changeLog, requiredResources, relatedResources, relatedPlatforms, catalogueId, fundingBody, fundingPrograms, grantProjectNames, helpdeskPage, userManual, termsOfUse, privacyPolicy, accessPolicy, resourceLevel, trainingInformation, statusMonitoring, maintenance, orderType, order, paymentModel, pricing);
        this.extras = extras;
    }



    public JSONObject getExtras() {
        return extras;
    }

    public void setExtras(JSONObject extras) {
        this.extras = extras;
    }
}
