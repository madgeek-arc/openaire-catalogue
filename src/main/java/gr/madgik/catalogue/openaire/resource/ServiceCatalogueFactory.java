package gr.madgik.catalogue.openaire.resource;

import gr.madgik.catalogue.openaire.ActionHandler;
import gr.madgik.catalogue.openaire.Catalogue;
import gr.madgik.catalogue.openaire.Context;
import gr.madgik.catalogue.openaire.domain.LoggingInfo;
import gr.madgik.catalogue.openaire.domain.Metadata;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.domain.User;
import gr.madgik.catalogue.openaire.resource.repository.ServiceRepository;
import gr.madgik.catalogue.openaire.utils.ProviderResourcesCommonMethods;
import gr.madgik.catalogue.openaire.utils.SimpleIdCreator;
import gr.uoa.di.madgik.registry.service.ServiceException;
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
    private final ProviderResourcesCommonMethods commonMethods;
    private final SimpleIdCreator idCreator;
    @Value("${project.catalogue.name}")
    private String catalogueName;

    public ServiceCatalogueFactory(ServiceRepository resourceRepository,
                                   ProviderResourcesCommonMethods commonMethods,
                                   SimpleIdCreator idCreator) {
        this.resourceRepository = resourceRepository;
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
//                fieldValidator.validate(serviceBundle); // TODO: replace with catalogue-lib validation
                logger.error("Validation removed. Replace with catalogue form validation.");

                return serviceBundle;
            }

            @Override
            public void postHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service registration postHandle");
            }

            @Override
            public void handleError(ServiceBundle serviceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Service registration handleError");
                throw new gr.uoa.di.madgik.registry.exception.ResourceException(throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
//                fieldValidator.validate(serviceBundle); // TODO: replace with catalogue-lib validation
                logger.error("Validation removed. Replace with catalogue form validation.");

                User user = User.of(SecurityContextHolder.getContext().getAuthentication());

                serviceBundle.setMetadata(Metadata.updateMetadata(existingService.getMetadata(), user.getFullname(),
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
            }

            @Override
            public void handleError(ServiceBundle serviceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Service update handleError");
                throw new gr.uoa.di.madgik.registry.exception.ResourceException(throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
            }

            @Override
            public void handleError(ServiceBundle serviceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Service delete handleError");
            }
        });

        return catalogue;
    }

}
