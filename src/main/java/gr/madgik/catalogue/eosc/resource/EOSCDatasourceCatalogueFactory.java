package gr.madgik.catalogue.eosc.resource;

import eu.einfracentral.domain.Datasource;
import eu.einfracentral.domain.DatasourceBundle;
import gr.madgik.catalogue.ActionHandler;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.Context;
import gr.madgik.catalogue.eosc.resource.repository.DatasourceRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class EOSCDatasourceCatalogueFactory {

    private static final Logger logger = LoggerFactory.getLogger(EOSCDatasourceCatalogueFactory.class);
    private final DatasourceRepository resourceRepository;

    public EOSCDatasourceCatalogueFactory(DatasourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Bean
    public Catalogue<DatasourceBundle, String> getDatasourceCatalogue() {
        Catalogue<DatasourceBundle, String> catalogue = new Catalogue<>(resourceRepository);

        catalogue.registerHandler(Catalogue.Action.REGISTER, new ActionHandler<>() {
            @Override
            public void preHandle(DatasourceBundle serviceBundle, Context ctx) {
                logger.info("Inside Datasource registration preHandle");
                serviceBundle.setId(createId(serviceBundle.getDatasource()));
            }

            @Override
            public void postHandle(DatasourceBundle serviceBundle, Context ctx) {
                logger.info("Inside Datasource registration postHandle");
            }

            @Override
            public void handleError(DatasourceBundle serviceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Datasource registration handleError");
            }
        });

        catalogue.registerHandler(Catalogue.Action.UPDATE, new ActionHandler<>() {
            @Override
            public void preHandle(DatasourceBundle serviceBundle, Context ctx) {
                logger.info("Inside Datasource update preHandle");
            }

            @Override
            public void postHandle(DatasourceBundle serviceBundle, Context ctx) {
                logger.info("Inside Datasource update postHandle");
            }

            @Override
            public void handleError(DatasourceBundle serviceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Datasource update handleError");
            }
        });

        return catalogue;
    }

    private String createId(Datasource resource) {
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
}
