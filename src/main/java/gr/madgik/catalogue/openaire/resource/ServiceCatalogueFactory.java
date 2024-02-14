package gr.madgik.catalogue.openaire.service;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.LoggingInfo;
import eu.einfracentral.domain.Metadata;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.catalogue.utils.SortUtils;
import gr.madgik.catalogue.ActionHandler;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.Context;
import gr.madgik.catalogue.MailerService;
import gr.madgik.catalogue.domain.User;
import gr.madgik.catalogue.exception.ValidationException;
import gr.madgik.catalogue.openaire.domain.Service;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.service.repository.ServiceRepository;
import gr.madgik.catalogue.openaire.utils.ProviderResourcesCommonMethods;
import gr.madgik.catalogue.openaire.validation.FieldValidator;
import gr.madgik.catalogue.service.sync.ServiceSync;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ServiceCatalogueFactory {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCatalogueFactory.class);
    private final ServiceRepository resourceRepository;
    private final ServiceSync serviceSync;
    private final FieldValidator fieldValidator;
    private final MailerService mailerService;
    private final ProviderResourcesCommonMethods commonMethods;

    public ServiceCatalogueFactory(ServiceRepository resourceRepository, ServiceSync serviceSync,
                                   FieldValidator fieldValidator, MailerService mailerService,
                                   ProviderResourcesCommonMethods commonMethods) {
        this.resourceRepository = resourceRepository;
        this.serviceSync = serviceSync;
        this.fieldValidator = fieldValidator;
        this.mailerService = mailerService;
        this.commonMethods = commonMethods;
    }

    @Bean
    public Catalogue<ServiceBundle, String> getServiceCatalogue() {
        Catalogue<ServiceBundle, String> catalogue = new Catalogue<>(resourceRepository);

        catalogue.registerHandler(Catalogue.Action.REGISTER, new ActionHandler<>() {
            @Override
            public ServiceBundle preHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service registration preHandle");
                if (serviceBundle.getService().getResourceOrganisation() == null || serviceBundle.getService().getResourceOrganisation().equals("")) {
                    serviceBundle.getService().setResourceOrganisation("openaire");
                }
                if (serviceBundle.getService().getCatalogueId() == null || serviceBundle.getService().getCatalogueId().equals("")) {
                    serviceBundle.getService().setCatalogueId("eosc");
                }
                serviceBundle.setId(createId(serviceBundle.getPayload()));

                // validate
                fieldValidator.validate(serviceBundle);

                // FIXME: imported code from eosc project - needs refactoring
                serviceBundle.setStatus("pending resource");
                serviceBundle.setActive(false);
                sortFields(serviceBundle);

                // create logging info
                createLoggingInfo(serviceBundle);

                // create Metadata
                serviceBundle.setMetadata(Metadata.createMetadata(User.of(SecurityContextHolder.getContext().getAuthentication()).getFullname()));

                return serviceBundle;
            }

            @Override
            public void postHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service registration postHandle");
                serviceSync.syncAdd(serviceBundle.getService());
            }

            @Override
            public void handleError(ServiceBundle serviceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Service registration handleError");
                throw new gr.athenarc.catalogue.exception.ResourceException(throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });

        catalogue.registerHandler(Catalogue.Action.UPDATE, new ActionHandler<>() {
            @Override
            public ServiceBundle preHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service update preHandle");
                fieldValidator.validate(serviceBundle);

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                User user = User.of(auth);
                // FIXME: imported code from eosc project - needs refactoring
                ServiceBundle existingService = resourceRepository.get(serviceBundle.getService().getId(), serviceBundle.getService().getCatalogueId());
                serviceBundle.setMetadata(Metadata.updateMetadata(existingService.getMetadata(), user.getFullname()));
                serviceBundle.setResourceExtras(existingService.getResourceExtras());
                serviceBundle.setIdentifiers(existingService.getIdentifiers());
                serviceBundle.setMigrationStatus(existingService.getMigrationStatus());

                ///////////////
                LoggingInfo loggingInfo;
                List<LoggingInfo> loggingInfoList = new ArrayList<>();

                // update VS version update
                if (((serviceBundle.getService().getVersion() == null) && (existingService.getService().getVersion() == null)) ||
                        (serviceBundle.getService().getVersion().equals(existingService.getService().getVersion()))) {
                    loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                            LoggingInfo.ActionType.UPDATED.getKey());
                } else {
                    loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                            LoggingInfo.ActionType.UPDATED_VERSION.getKey());
                }
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                serviceBundle.setLoggingInfo(loggingInfoList);

                // latestUpdateInfo
                serviceBundle.setLatestUpdateInfo(loggingInfo);
                serviceBundle.setActive(existingService.isActive());
                sortFields(serviceBundle);

                // set status
                serviceBundle.setStatus(existingService.getStatus());

                // if Resource's status = "rejected resource", update to "pending resource" & Provider templateStatus to "pending template"
                if (existingService.getStatus().equals("rejected resource")) {
                    serviceBundle.setStatus("pending resource");
                    serviceBundle.setActive(false);
                    // TODO: update provider template status
//                    providerBundle.setTemplateStatus(vocabularyService.get("pending template").getId());
//                    providerService.update(providerBundle, serviceBundle.getService().getCatalogueId(), auth);
                }

                // if a user updates a service with version to a service with null version then while searching for the service
                // you get a "Service already exists" error.
                if (existingService.getService().getVersion() != null && serviceBundle.getService().getVersion() == null) {
                    throw new ServiceException("You cannot update a Service registered with version to a Service with null version");
                }

                // block catalogueId updates from Provider Admins
                if (!user.getRoles().contains("ROLE_ADMIN")) {
                    if (!existingService.getService().getCatalogueId().equals(serviceBundle.getService().getCatalogueId())) {
                        throw new ValidationException("You cannot change catalogueId");
                    }
                }

                return serviceBundle;
            }

            @Override
            public void postHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service update postHandle");
                serviceSync.syncUpdate(serviceBundle.getService());

                // FIXME: imported code from eosc project - needs refactoring
                // send notification emails to Portal Admins
                if (serviceBundle.getLatestAuditInfo() != null && serviceBundle.getLatestUpdateInfo() != null) {
                    Long latestAudit = Long.parseLong(serviceBundle.getLatestAuditInfo().getDate());
                    Long latestUpdate = Long.parseLong(serviceBundle.getLatestUpdateInfo().getDate());
                    if (latestAudit < latestUpdate && serviceBundle.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                        // TODO: enable mails
//                        mailerService.notifyPortalAdminsForInvalidResourceUpdate(serviceBundle);
                    }
                }
            }

            @Override
            public void handleError(ServiceBundle serviceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Service update handleError");
                throw new gr.athenarc.catalogue.exception.ResourceException(throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });

        return catalogue;
    }

    private String createId(Service resource) {
        if (resource.getResourceOrganisation() == null || resource.getResourceOrganisation().equals("")) {
//            throw new ValidationException("Resource must have a Resource Organisation.");
            throw new RuntimeException("Resource must have a Resource Organisation.");
        }
        if (resource.getName() == null || resource.getName().equals("")) {
//            throw new ValidationException("Resource must have a Name.");
            throw new RuntimeException("Resource must have a Name.");
        }
        String provider = resource.getResourceOrganisation();
        return String.format("%s.%s", provider, StringUtils
                .stripAccents(resource.getName())
                .replaceAll("[\n\t\\s]+", " ")
                .replaceAll("\\s+$", "")
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_")
                .toLowerCase());
    }

    public static LoggingInfo createLoggingInfoEntry(User user, String type, String actionType) {
        LoggingInfo ret = new LoggingInfo();
        ret.setDate(String.valueOf(System.currentTimeMillis()));
        ret.setType(type);
        ret.setActionType(actionType);
        ret.setUserEmail(user.getEmail());
        ret.setUserFullName(user.getFullname());
        ret.setUserRole(String.join(",", user.getRoles()));
        return ret;
    }

    private void createLoggingInfo(ServiceBundle serviceBundle) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = User.of(auth);
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(auth, User.of(auth).getFullname(), String.join(",", user.getRoles()),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);

        // latestOnboardingInfo
        serviceBundle.setLatestOnboardingInfo(loggingInfo);

        // LoggingInfo
        serviceBundle.setLoggingInfo(loggingInfoList);
    }

    public <T extends Bundle<? extends Service>> void sortFields(T resourceBundle) {
        resourceBundle.getPayload().setGeographicalAvailabilities(SortUtils.sort(resourceBundle.getPayload().getGeographicalAvailabilities()));
        resourceBundle.getPayload().setResourceGeographicLocations(SortUtils.sort(resourceBundle.getPayload().getResourceGeographicLocations()));
    }
}
