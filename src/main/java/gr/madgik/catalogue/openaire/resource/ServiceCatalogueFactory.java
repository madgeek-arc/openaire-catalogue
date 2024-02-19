package gr.madgik.catalogue.openaire.resource;

import eu.einfracentral.domain.LoggingInfo;
import eu.einfracentral.domain.Metadata;
import gr.madgik.catalogue.domain.User;
import eu.openminted.registry.core.service.ServiceException;
import gr.madgik.catalogue.ActionHandler;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.Context;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.resource.repository.ServiceRepository;
import gr.madgik.catalogue.openaire.utils.ProviderResourcesCommonMethods;
import gr.madgik.catalogue.openaire.utils.SimpleIdCreator;
import gr.madgik.catalogue.openaire.validation.FieldValidator;
import gr.madgik.catalogue.service.sync.ServiceSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ServiceCatalogueFactory {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCatalogueFactory.class);
    private final ServiceRepository resourceRepository;
    private final ServiceSync serviceSync;
    private final FieldValidator fieldValidator;
    private final ProviderResourcesCommonMethods commonMethods;
    private final SimpleIdCreator idCreator;
    @Value("${project.catalogue.name}")
    private String catalogueName;

    public ServiceCatalogueFactory(ServiceRepository resourceRepository,
                                   ServiceSync serviceSync,
                                   FieldValidator fieldValidator,
                                   ProviderResourcesCommonMethods commonMethods,
                                   SimpleIdCreator idCreator) {
        this.resourceRepository = resourceRepository;
        this.serviceSync = serviceSync;
        this.fieldValidator = fieldValidator;
        this.commonMethods = commonMethods;
        this.idCreator = idCreator;
    }

    @Bean
    public Catalogue<ServiceBundle, String> getServiceCatalogue() {
        Catalogue<ServiceBundle, String> catalogue = new Catalogue<>(resourceRepository);

        catalogue.registerHandler(Catalogue.Action.REGISTER, new ActionHandler<>() {
            @Override
            public ServiceBundle preHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service registration preHandle");
                User user = User.of(SecurityContextHolder.getContext().getAuthentication());
                commonMethods.onboard(serviceBundle, user);
                serviceBundle.setId(idCreator.createServiceId(serviceBundle.getService()));
                serviceBundle.setMetadata(Metadata.createMetadata(user.getFullname(), user.getEmail()));

                // validate
                fieldValidator.validate(serviceBundle);

                return serviceBundle;
            }

            @Override
            public void postHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service registration postHandle");
//                serviceSync.syncAdd(serviceBundle.getService());
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
                ServiceBundle existingService = resourceRepository.get(serviceBundle.getService().getId(),
                        serviceBundle.getService().getCatalogueId());
                serviceBundle.getService().setCatalogueId(catalogueName);

                // validate
                commonMethods.prohibitCatalogueIdChange(serviceBundle.getService().getCatalogueId());
                fieldValidator.validate(serviceBundle);

                User user = User.of(SecurityContextHolder.getContext().getAuthentication());

                serviceBundle.setMetadata(Metadata.updateMetadata(serviceBundle.getMetadata(), user.getFullname(),
                        user.getEmail()));
                serviceBundle.setResourceExtras(existingService.getResourceExtras());
                serviceBundle.setIdentifiers(existingService.getIdentifiers());
                serviceBundle.setMigrationStatus(existingService.getMigrationStatus());

                List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingService, user);

                // update VS version update
                LoggingInfo loggingInfo;
                if (((serviceBundle.getService().getVersion() == null) && (existingService.getService().getVersion() == null)) ||
                        (serviceBundle.getService().getVersion().equals(existingService.getService().getVersion()))) {
                    loggingInfo = commonMethods.createLoggingInfo(user, LoggingInfo.Types.UPDATE.getKey(),
                            LoggingInfo.ActionType.UPDATED.getKey());
                } else {
                    loggingInfo = commonMethods.createLoggingInfo(user, LoggingInfo.Types.UPDATE.getKey(),
                            LoggingInfo.ActionType.UPDATED_VERSION.getKey());
                }
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
                serviceBundle.setLoggingInfo(loggingInfoList);

                // latestUpdateInfo
                serviceBundle.setLatestUpdateInfo(loggingInfo);
                serviceBundle.setActive(existingService.isActive());
                serviceBundle.setStatus(existingService.getStatus());

                // TODO: TBD
                // if Resource's status = "rejected resource", update to "pending resource"
                if (existingService.getStatus().equals("rejected resource")) {
                    serviceBundle.setStatus("pending resource");
                    serviceBundle.setActive(false);
                }

                // if a user updates a service with version to a service with null version then while searching for the service
                // you get a "Service already exists" error.
                if (existingService.getService().getVersion() != null && serviceBundle.getService().getVersion() == null) {
                    throw new ServiceException("You cannot update a Service registered with version to a Service with null version");
                }

                return serviceBundle;
            }

            @Override
            public void postHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service update postHandle");
//                serviceSync.syncUpdate(serviceBundle.getService());
            }

            @Override
            public void handleError(ServiceBundle serviceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Service update handleError");
                throw new gr.athenarc.catalogue.exception.ResourceException(throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });

        catalogue.registerHandler(Catalogue.Action.DELETE, new ActionHandler<>() {
            @Override
            public ServiceBundle preHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service delete preHandle");
                return serviceBundle;
            }

            @Override
            public void postHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service delete postHandle");
//                serviceSync.syncDelete(serviceBundle.getService());
            }

            @Override
            public void handleError(ServiceBundle serviceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Service delete handleError");
            }
        });

        return catalogue;
    }

}
