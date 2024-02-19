package gr.madgik.catalogue.openaire.resource;

import eu.einfracentral.domain.LoggingInfo;
import eu.einfracentral.domain.Metadata;
import gr.madgik.catalogue.ActionHandler;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.Context;
import gr.madgik.catalogue.domain.User;
import gr.madgik.catalogue.openaire.domain.DatasourceBundle;
import gr.madgik.catalogue.openaire.resource.repository.DatasourceRepository;
import gr.madgik.catalogue.openaire.utils.ProviderResourcesCommonMethods;
import gr.madgik.catalogue.openaire.validation.FieldValidator;
import gr.madgik.catalogue.service.sync.DatasourceSync;
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
    private final DatasourceSync datasourceSync;
    private final FieldValidator fieldValidator;
    private final ProviderResourcesCommonMethods commonMethods;
    @Value("${project.catalogue.name}")
    private String catalogueName;

    public DatasourceCatalogueFactory(DatasourceRepository resourceRepository,
                                      DatasourceSync dataSourceSync,
                                      FieldValidator fieldValidator,
                                      ProviderResourcesCommonMethods commonMethods) {
        this.resourceRepository = resourceRepository;
        this.datasourceSync = dataSourceSync;
        this.fieldValidator = fieldValidator;
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
                fieldValidator.validate(datasourceBundle);

                return datasourceBundle;
            }

            @Override
            public void postHandle(DatasourceBundle datasourceBundle, Context ctx) {
                logger.info("Inside Datasource registration postHandle");
//                datasourceSync.syncAdd(datasourceBundle.getDatasource());
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
                fieldValidator.validate(datasourceBundle);

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
//                datasourceSync.syncUpdate(datasourceBundle.getDatasource());
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
//                datasourceSync.syncDelete(datasourceBundle.getDatasource());
            }

            @Override
            public void handleError(DatasourceBundle datasourceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Datasource delete handleError");
            }
        });

        return catalogue;
    }
}
