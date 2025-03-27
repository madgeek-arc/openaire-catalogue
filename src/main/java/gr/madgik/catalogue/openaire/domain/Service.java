/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.madgik.catalogue.openaire.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import org.json.simple.JSONObject;

import java.net.URL;
import java.util.Date;
import java.util.List;

public class Service implements Identifiable {

    // Basic Information
    /**
     * A persistent identifier, a unique reference to the Resource in the context of the EOSC Portal.
     */
    @Schema(example = "(required on PUT only)")
    private String id;

    /**
     * An abbreviation of the Resource Name as assigned by the Provider
     */
    @Schema
    private String abbreviation;

    /**
     * Resource Full Name as assigned by the Provider.
     */
    @Schema
    private String name;

    /**
     * The name (or abbreviation) of the organisation that manages or delivers the resource, or that coordinates resource delivery in a federated scenario.
     */
    @Schema
    private String resourceOrganisation;

    /**
     * The name(s) (or abbreviation(s)) of Provider(s) that manage or deliver the Resource in federated scenarios.
     */
    @Schema
    private List<String> resourceProviders;

    /**
     * Webpage with information about the Resource usually hosted and maintained by the Provider.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    private URL webpage;

    /**
     * Other types of Identifiers for the specific Service (eg. PID)
     */
    @Schema
    private List<AlternativeIdentifier> alternativeIdentifiers;


    // Marketing Information
    /**
     * A high-level description in fairly non-technical terms of a) what the Resource does, functionality it provides and Resources it enables to access,
     * b) the benefit to a user/customer delivered by a Resource; benefits are usually related to alleviating pains
     * (e.g., eliminate undesired outcomes, obstacles or risks) or producing gains (e.g. increased performance, social gains, positive emotions or cost saving),
     * c) list of customers, communities, users, etc. using the Resource.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    /**
     * Short catch-phrase for marketing and advertising purposes. It will be usually displayed close to the Resource name and should refer to the main value or purpose of the Resource.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String tagline;

    /**
     * Link to the logo/visual identity of the Resource. The logo will be visible at the Portal. If there is no specific logo for the Resource the logo of the Provider may be used.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    private URL logo;

    /**
     * Link to video, slideshow, photos, screenshots with details of the Provider.
     */
    @Schema
    private List<MultimediaPair> multimedia;

    /**
     * Link to use cases supported by this Resource.
     */
    @Schema
    private List<UseCasesPair> useCases;


    // Classification Information
    /**
     * The branch of science, scientific discipline that is related to the Resource.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ServiceProviderDomain> scientificDomains;

    /**
     * A named group of Resources that offer access to the same type of Resources.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ServiceCategory> categories;

    /**
     * Type of users/customers that commissions a Provider to deliver a Resource.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> targetUsers;

    /**
     * The way a user can access the service/resource (Remote, Physical, Virtual, etc.).
     */
    @Schema
    private List<String> accessTypes;

    /**
     * Eligibility/criteria for granting access to users (excellence-based, free-conditionally, free etc.).
     */
    @Schema
    private List<String> accessModes;

    /**
     * Keywords associated to the Resource to simplify search by relevant keywords.
     */
    @Schema
    private List<String> tags;

    /**
     * Does Service consist a generic service or resource bringing significant value to two or more research
     * infrastructures.
     */
    @Schema
    private Boolean horizontalService;

    /**
     * A named group of Resources that offer access to the same type of Resources.
     */
    @Schema
    private List<String> serviceCategories;

    /**
     * Placement of the Service in the different sections of the EOSC Marketplace.
     */
    @Schema
    private List<String> marketplaceLocations;


    // Geographical and Language Availability Information
    /**
     * Locations where the Resource is offered.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> geographicalAvailabilities;

    /**
     * Languages of the (user interface of the) Resource.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> languageAvailabilities;


    // Resource Location Information
    /**
     * List of geographic locations where data, samples, etc. are stored and processed.
     */
    @Schema
    private List<String> resourceGeographicLocations;


    // Contact Information
    /**
     * Service's Main Contact/Resource Owner info.
     */
    @Schema
    private ServiceMainContact mainContact;

    /**
     * List of the Service's Public Contacts info.
     */
    @Schema
    private List<ServicePublicContact> publicContacts;

    /**
     * The email to ask more information from the Provider about this Resource.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String helpdeskEmail;

    /**
     * The email to contact the Provider for critical security issues about this Resource.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String securityContactEmail;


    // Maturity Information
    /**
     * The Technology Readiness Level of the Resource (to be further updated in the context of the EOSC).
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String trl;

    /**
     * Phase of the Resource life-cycle.
     */
    @Schema
    private String lifeCycleStatus;

    /**
     * List of certifications obtained for the Resource (including the certification body).
     */
    @Schema
    private List<String> certifications;

    /**
     * List of standards supported by the Resource.
     */
    @Schema
    private List<String> standards;

    /**
     * List of open source technologies supported by the Resource.
     */
    @Schema
    private List<String> openSourceTechnologies;

    /**
     * Version of the Resource that is in force.
     */
    @Schema
    private String version;

    /**
     * Date of the latest update of the Resource.
     */
    @Schema(example = "2020-01-01")
    private Date lastUpdate;

    /**
     * Summary of the Resource features updated from the previous version.
     */
    @Schema
    private List<String> changeLog;


    // Dependencies Information
    /**
     * List of other Resources required to use this Resource.
     */
    @Schema
    private List<String> requiredResources;

    /**
     * List of other Resources that are commonly used with this Resource.
     */
    @Schema
    private List<String> relatedResources;

    /**
     * List of suites or thematic platforms in which the Resource is engaged or Providers (Provider groups) contributing to this Resource.
     */
    @Schema
    private List<String> relatedPlatforms;

    /**
     * The Catalogue this Resource is originally registered at.
     */
    @Schema
    private String catalogueId;


    // Attribution Information
    /**
     * Name of the funding body that supported the development and/or operation of the Resource.
     */
    @Schema
    private List<String> fundingBody;

    /**
     * Name of the funding program that supported the development and/or operation of the Resource.
     */
    @Schema
    private List<String> fundingPrograms;

    /**
     * Name of the project that supported the development and/or operation of the Resource.
     */
    @Schema
    private List<String> grantProjectNames;


    // Management Information
    /**
     * The URL to a webpage to ask more information from the Provider about this Resource.
     */
    @Schema(example = "https://example.com")
    private URL helpdeskPage;

    /**
     * Link to the Resource user manual and documentation.
     */
    @Schema(example = "https://example.com")
    private URL userManual;

    /**
     * Webpage describing the rules, Resource conditions and usage policy which one must agree to abide by in order to use the Resource.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    private URL termsOfUse;

    /**
     * Link to the privacy policy applicable to the Resource.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com")
    private URL privacyPolicy;

    /**
     * Information about the access policies that apply.
     */
    @Schema(example = "https://example.com")
    private URL accessPolicy;

    /**
     * Webpage with the information about the levels of performance that a Provider is expected to deliver.
     */
    @Schema(example = "https://example.com")
    private URL resourceLevel;

    /**
     * Webpage to training information on the Resource.
     */
    @Schema(example = "https://example.com")
    private URL trainingInformation;

    /**
     * Webpage with monitoring information about this Resource.
     */
    @Schema(example = "https://example.com")
    private URL statusMonitoring;

    /**
     * Webpage with information about planned maintenance windows for this Resource.
     */
    @Schema(example = "https://example.com")
    private URL maintenance;


    // Access & Order Information
    /**
     * Information on the order type (requires an ordering procedure, or no ordering and if fully open or requires authentication).
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderType;

    /**
     * Webpage through which an order for the Resource can be placed.
     */
    @Schema(example = "https://example.com")
    private URL order;


    // Financial Information
    /**
     * Webpage with the supported payment models and restrictions that apply to each of them.
     */
    @Schema(example = "https://example.com")
    private URL paymentModel;

    /**
     * Webpage with the information on the price scheme for this Resource in case the customer is charged for.
     */
    @Schema(example = "https://example.com")
    private URL pricing;


    private JSONObject extras;

    public Service() {
        // No arg constructor
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceOrganisation() {
        return resourceOrganisation;
    }

    public void setResourceOrganisation(String resourceOrganisation) {
        this.resourceOrganisation = resourceOrganisation;
    }

    public List<String> getResourceProviders() {
        return resourceProviders;
    }

    public void setResourceProviders(List<String> resourceProviders) {
        this.resourceProviders = resourceProviders;
    }

    public URL getWebpage() {
        return webpage;
    }

    public void setWebpage(URL webpage) {
        this.webpage = webpage;
    }

    public List<AlternativeIdentifier> getAlternativeIdentifiers() {
        return alternativeIdentifiers;
    }

    public void setAlternativeIdentifiers(List<AlternativeIdentifier> alternativeIdentifiers) {
        this.alternativeIdentifiers = alternativeIdentifiers;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public URL getLogo() {
        return logo;
    }

    public void setLogo(URL logo) {
        this.logo = logo;
    }

    public List<MultimediaPair> getMultimedia() {
        return multimedia;
    }

    public void setMultimedia(List<MultimediaPair> multimedia) {
        this.multimedia = multimedia;
    }

    public List<UseCasesPair> getUseCases() {
        return useCases;
    }

    public void setUseCases(List<UseCasesPair> useCases) {
        this.useCases = useCases;
    }

    public List<ServiceProviderDomain> getScientificDomains() {
        return scientificDomains;
    }

    public void setScientificDomains(List<ServiceProviderDomain> scientificDomains) {
        this.scientificDomains = scientificDomains;
    }

    public List<ServiceCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<ServiceCategory> categories) {
        this.categories = categories;
    }

    public List<String> getTargetUsers() {
        return targetUsers;
    }

    public void setTargetUsers(List<String> targetUsers) {
        this.targetUsers = targetUsers;
    }

    public List<String> getAccessTypes() {
        return accessTypes;
    }

    public void setAccessTypes(List<String> accessTypes) {
        this.accessTypes = accessTypes;
    }

    public List<String> getAccessModes() {
        return accessModes;
    }

    public void setAccessModes(List<String> accessModes) {
        this.accessModes = accessModes;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Boolean getHorizontalService() {
        return horizontalService;
    }

    public void setHorizontalService(Boolean horizontalService) {
        this.horizontalService = horizontalService;
    }

    public List<String> getServiceCategories() {
        return serviceCategories;
    }

    public void setServiceCategories(List<String> serviceCategories) {
        this.serviceCategories = serviceCategories;
    }

    public List<String> getMarketplaceLocations() {
        return marketplaceLocations;
    }

    public void setMarketplaceLocations(List<String> marketplaceLocations) {
        this.marketplaceLocations = marketplaceLocations;
    }

    public List<String> getGeographicalAvailabilities() {
        return geographicalAvailabilities;
    }

    public void setGeographicalAvailabilities(List<String> geographicalAvailabilities) {
        this.geographicalAvailabilities = geographicalAvailabilities;
    }

    public List<String> getLanguageAvailabilities() {
        return languageAvailabilities;
    }

    public void setLanguageAvailabilities(List<String> languageAvailabilities) {
        this.languageAvailabilities = languageAvailabilities;
    }

    public List<String> getResourceGeographicLocations() {
        return resourceGeographicLocations;
    }

    public void setResourceGeographicLocations(List<String> resourceGeographicLocations) {
        this.resourceGeographicLocations = resourceGeographicLocations;
    }

    public ServiceMainContact getMainContact() {
        return mainContact;
    }

    public void setMainContact(ServiceMainContact mainContact) {
        this.mainContact = mainContact;
    }

    public List<ServicePublicContact> getPublicContacts() {
        return publicContacts;
    }

    public void setPublicContacts(List<ServicePublicContact> publicContacts) {
        this.publicContacts = publicContacts;
    }

    public String getHelpdeskEmail() {
        return helpdeskEmail;
    }

    public void setHelpdeskEmail(String helpdeskEmail) {
        this.helpdeskEmail = helpdeskEmail;
    }

    public String getSecurityContactEmail() {
        return securityContactEmail;
    }

    public void setSecurityContactEmail(String securityContactEmail) {
        this.securityContactEmail = securityContactEmail;
    }

    public String getTrl() {
        return trl;
    }

    public void setTrl(String trl) {
        this.trl = trl;
    }

    public String getLifeCycleStatus() {
        return lifeCycleStatus;
    }

    public void setLifeCycleStatus(String lifeCycleStatus) {
        this.lifeCycleStatus = lifeCycleStatus;
    }

    public List<String> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<String> certifications) {
        this.certifications = certifications;
    }

    public List<String> getStandards() {
        return standards;
    }

    public void setStandards(List<String> standards) {
        this.standards = standards;
    }

    public List<String> getOpenSourceTechnologies() {
        return openSourceTechnologies;
    }

    public void setOpenSourceTechnologies(List<String> openSourceTechnologies) {
        this.openSourceTechnologies = openSourceTechnologies;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public List<String> getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(List<String> changeLog) {
        this.changeLog = changeLog;
    }

    public List<String> getRequiredResources() {
        return requiredResources;
    }

    public void setRequiredResources(List<String> requiredResources) {
        this.requiredResources = requiredResources;
    }

    public List<String> getRelatedResources() {
        return relatedResources;
    }

    public void setRelatedResources(List<String> relatedResources) {
        this.relatedResources = relatedResources;
    }

    public List<String> getRelatedPlatforms() {
        return relatedPlatforms;
    }

    public void setRelatedPlatforms(List<String> relatedPlatforms) {
        this.relatedPlatforms = relatedPlatforms;
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    public List<String> getFundingBody() {
        return fundingBody;
    }

    public void setFundingBody(List<String> fundingBody) {
        this.fundingBody = fundingBody;
    }

    public List<String> getFundingPrograms() {
        return fundingPrograms;
    }

    public void setFundingPrograms(List<String> fundingPrograms) {
        this.fundingPrograms = fundingPrograms;
    }

    public List<String> getGrantProjectNames() {
        return grantProjectNames;
    }

    public void setGrantProjectNames(List<String> grantProjectNames) {
        this.grantProjectNames = grantProjectNames;
    }

    public URL getHelpdeskPage() {
        return helpdeskPage;
    }

    public void setHelpdeskPage(URL helpdeskPage) {
        this.helpdeskPage = helpdeskPage;
    }

    public URL getUserManual() {
        return userManual;
    }

    public void setUserManual(URL userManual) {
        this.userManual = userManual;
    }

    public URL getTermsOfUse() {
        return termsOfUse;
    }

    public void setTermsOfUse(URL termsOfUse) {
        this.termsOfUse = termsOfUse;
    }

    public URL getPrivacyPolicy() {
        return privacyPolicy;
    }

    public void setPrivacyPolicy(URL privacyPolicy) {
        this.privacyPolicy = privacyPolicy;
    }

    public URL getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(URL accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public URL getResourceLevel() {
        return resourceLevel;
    }

    public void setResourceLevel(URL resourceLevel) {
        this.resourceLevel = resourceLevel;
    }

    public URL getTrainingInformation() {
        return trainingInformation;
    }

    public void setTrainingInformation(URL trainingInformation) {
        this.trainingInformation = trainingInformation;
    }

    public URL getStatusMonitoring() {
        return statusMonitoring;
    }

    public void setStatusMonitoring(URL statusMonitoring) {
        this.statusMonitoring = statusMonitoring;
    }

    public URL getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(URL maintenance) {
        this.maintenance = maintenance;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public URL getOrder() {
        return order;
    }

    public void setOrder(URL order) {
        this.order = order;
    }

    public URL getPaymentModel() {
        return paymentModel;
    }

    public void setPaymentModel(URL paymentModel) {
        this.paymentModel = paymentModel;
    }

    public URL getPricing() {
        return pricing;
    }

    public void setPricing(URL pricing) {
        this.pricing = pricing;
    }

    public JSONObject getExtras() {
        return extras;
    }

    public void setExtras(JSONObject extras) {
        this.extras = extras;
    }
}
