package gr.madgik.catalogue.openaire.resource;

import gr.madgik.catalogue.openaire.ActionHandler;
import gr.madgik.catalogue.openaire.Catalogue;
import gr.madgik.catalogue.openaire.Context;
import gr.madgik.catalogue.openaire.domain.DatasourceBundle;
import gr.madgik.catalogue.openaire.domain.LoggingInfo;
import gr.madgik.catalogue.openaire.domain.Metadata;
import gr.madgik.catalogue.openaire.domain.User;
import gr.madgik.catalogue.openaire.resource.repository.DatasourceRepository;
import gr.madgik.catalogue.openaire.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class DatasourceCatalogueFactory {

    private static final Logger logger = LoggerFactory.getLogger(DatasourceCatalogueFactory.class);
    private final DatasourceRepository resourceRepository;
    private final ProviderResourcesCommonMethods commonMethods;
    @Value("${project.catalogue.name}")
    private String catalogueName;

    public DatasourceCatalogueFactory(DatasourceRepository resourceRepository,
                                      ProviderResourcesCommonMethods commonMethods) {
        this.resourceRepository = resourceRepository;
        this.commonMethods = commonMethods;
    }

    @Bean
    public Catalogue<DatasourceBundle, String> getDatasourceCatalogue() {
        Catalogue<DatasourceBundle, String> catalogue = new Catalogue<>(resourceRepository);

        catalogue.registerHandler(Catalogue.Action.REGISTER, new ActionHandler<>() {
            @Override
            public DatasourceBundle preHandle(DatasourceBundle datasourceBundle, Context ctx) {
                logger.info("Inside Datasource registration preHandle");
                User user = User.of(SecurityContextHolder.getContext().getAuthentication());
                commonMethods.onboard(datasourceBundle, user);
                datasourceBundle.setId(datasourceBundle.getDatasource().getServiceId());
                datasourceBundle.setMetadata(Metadata.createMetadata(user.getFullname(), user.getEmail()));

                // validate
//                fieldValidator.validate(datasourceBundle); // TODO: replace with catalogue-lib validation
                logger.error("Validation removed. Replace with catalogue form validation.");

                return datasourceBundle;
            }

            @Override
            public void postHandle(DatasourceBundle datasourceBundle, Context ctx) {
                logger.info("Inside Datasource registration postHandle");
            }

            @Override
            public void handleError(DatasourceBundle datasourceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Datasource registration handleError");
            }
        });

        catalogue.registerHandler(Catalogue.Action.UPDATE, new ActionHandler<>() {
            @Override
            public DatasourceBundle preHandle(DatasourceBundle datasourceBundle, Context ctx) {
                logger.info("Inside Datasource update preHandle");

                DatasourceBundle existing = resourceRepository.get(datasourceBundle.getId());
                existing.setDatasource(datasourceBundle.getDatasource());
                datasourceBundle = existing;
                datasourceBundle.getDatasource().setCatalogueId(catalogueName);

                // validate
                commonMethods.prohibitCatalogueIdChange(datasourceBundle.getDatasource().getCatalogueId());
//                fieldValidator.validate(datasourceBundle); // TODO: replace with catalogue-lib validation
                logger.error("Validation removed. Replace with catalogue form validation.");

                User user = User.of(SecurityContextHolder.getContext().getAuthentication());

                datasourceBundle.setMetadata(Metadata.updateMetadata(datasourceBundle.getMetadata(), user.getFullname(),
                        user.getEmail()));

                List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(datasourceBundle, user);
                LoggingInfo loggingInfo = commonMethods.createLoggingInfo(user, LoggingInfo.Types.UPDATE.getKey(),
                        LoggingInfo.ActionType.UPDATED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
                datasourceBundle.setLoggingInfo(loggingInfoList);
                datasourceBundle.setLatestUpdateInfo(loggingInfo);

                return datasourceBundle;
            }

            @Override
            public void postHandle(DatasourceBundle datasourceBundle, Context ctx) {
                logger.info("Inside Datasource update postHandle");
            }

            @Override
            public void handleError(DatasourceBundle datasourceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Datasource update handleError");
            }
        });

        catalogue.registerHandler(Catalogue.Action.DELETE, new ActionHandler<>() {
            @Override
            public DatasourceBundle preHandle(DatasourceBundle datasourceBundle, Context ctx) {
                logger.info("Inside Datasource delete preHandle");
                return datasourceBundle;
            }

            @Override
            public void postHandle(DatasourceBundle datasourceBundle, Context ctx) {
                logger.info("Inside Datasource delete postHandle");
            }

            @Override
            public void handleError(DatasourceBundle datasourceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Datasource delete handleError");
            }
        });

        return catalogue;
    }
}
