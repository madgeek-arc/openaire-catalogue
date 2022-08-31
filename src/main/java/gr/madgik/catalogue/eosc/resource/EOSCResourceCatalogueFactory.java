package gr.madgik.catalogue.eosc.resource;

import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceBundle;
import gr.madgik.catalogue.ActionHandler;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.Context;
import gr.madgik.catalogue.eosc.resource.repository.MongoServiceRepository;
import gr.madgik.catalogue.eosc.resource.repository.ServiceRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class EOSCResourceCatalogueFactory {

    private static final Logger logger = LoggerFactory.getLogger(EOSCResourceCatalogueFactory.class);
    private final ServiceRepository resourceRepository;

    public EOSCResourceCatalogueFactory(ServiceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Bean
    public Catalogue<ServiceBundle, String> getResourceCatalogue() {
        Catalogue<ServiceBundle, String> catalogue = new Catalogue<>(resourceRepository);

        catalogue.registerHandler(Catalogue.Action.REGISTER, new ActionHandler<>() {
            @Override
            public void preHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service registration preHandle");
                serviceBundle.setId(createId(serviceBundle.getService()));
            }

            @Override
            public void postHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service registration postHandle");
            }

            @Override
            public void handleError(ServiceBundle serviceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Service registration handleError");
            }
        });

        catalogue.registerHandler(Catalogue.Action.UPDATE, new ActionHandler<>() {
            @Override
            public void preHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service update preHandle");
            }

            @Override
            public void postHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service update postHandle");
            }

            @Override
            public void handleError(ServiceBundle serviceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Service update handleError");
            }
        });

        return catalogue;
    }

    private String createId(Service service) {
        if (service.getResourceOrganisation() == null || service.getResourceOrganisation().equals("")) {
//            throw new ValidationException("Resource must have a Resource Organisation.");
            throw new RuntimeException("Resource must have a Resource Organisation.");
        }
        if (service.getName() == null || service.getName().equals("")) {
//            throw new ValidationException("Resource must have a Name.");
            throw new RuntimeException("Resource must have a Name.");
        }
        String provider = service.getResourceOrganisation();
        return String.format("%s.%s", provider, StringUtils
                .stripAccents(service.getName())
                .replaceAll("[\n\t\\s]+", " ")
                .replaceAll("\\s+$", "")
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_")
                .toLowerCase());
    }
}
