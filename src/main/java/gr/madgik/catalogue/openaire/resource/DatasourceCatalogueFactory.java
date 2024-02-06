package gr.madgik.catalogue.openaire.resource;

import gr.madgik.catalogue.ActionHandler;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.Context;
import gr.madgik.catalogue.openaire.domain.Datasource;
import gr.madgik.catalogue.openaire.domain.DatasourceBundle;
import gr.madgik.catalogue.openaire.resource.repository.DatasourceRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class DatasourceCatalogueFactory {

    private static final Logger logger = LoggerFactory.getLogger(DatasourceCatalogueFactory.class);
    private final DatasourceRepository resourceRepository;

    public DatasourceCatalogueFactory(DatasourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Bean
    public Catalogue<DatasourceBundle, String> getDatasourceCatalogue() {
        Catalogue<DatasourceBundle, String> catalogue = new Catalogue<>(resourceRepository);

        catalogue.registerHandler(Catalogue.Action.REGISTER, new ActionHandler<>() {
            @Override
            public DatasourceBundle preHandle(DatasourceBundle datasourceBundle, Context ctx) {
                logger.info("Inside Datasource registration preHandle");
                datasourceBundle.setId(createId(datasourceBundle.getDatasource()));
                datasourceBundle.setActive(false);
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

        return catalogue;
    }

    private String createId(Datasource resource) {
        return resource.getServiceId();
    }
}
