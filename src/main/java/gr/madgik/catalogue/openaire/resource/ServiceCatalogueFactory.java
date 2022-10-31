package gr.madgik.catalogue.openaire.resource;

import eu.einfracentral.domain.Service;
import gr.madgik.catalogue.ActionHandler;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.Context;
import gr.madgik.catalogue.openaire.OpenAIREServiceBundle;
import gr.madgik.catalogue.openaire.resource.repository.ServiceRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ServiceCatalogueFactory {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCatalogueFactory.class);
    private final ServiceRepository resourceRepository;

    public ServiceCatalogueFactory(ServiceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Bean
    public Catalogue<OpenAIREServiceBundle, String> getServiceCatalogue() {
        Catalogue<OpenAIREServiceBundle, String> catalogue = new Catalogue<>(resourceRepository);

        catalogue.registerHandler(Catalogue.Action.REGISTER, new ActionHandler<>() {
            @Override
            public void preHandle(OpenAIREServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service registration preHandle");
                if (serviceBundle.getService().getResourceOrganisation() == null) {
                    serviceBundle.getService().setResourceOrganisation("openaire");
                }
                serviceBundle.setId(createId(serviceBundle.getPayload()));
            }

            @Override
            public void postHandle(OpenAIREServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service registration postHandle");
            }

            @Override
            public void handleError(OpenAIREServiceBundle serviceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Service registration handleError");
            }
        });

        catalogue.registerHandler(Catalogue.Action.UPDATE, new ActionHandler<>() {
            @Override
            public void preHandle(OpenAIREServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service update preHandle");
            }

            @Override
            public void postHandle(OpenAIREServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service update postHandle");
            }

            @Override
            public void handleError(OpenAIREServiceBundle serviceBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Service update handleError");
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
}
