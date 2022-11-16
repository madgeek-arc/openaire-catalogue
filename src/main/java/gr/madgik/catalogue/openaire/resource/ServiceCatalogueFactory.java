package gr.madgik.catalogue.openaire.resource;

import eu.einfracentral.domain.Service;
import gr.madgik.catalogue.ActionHandler;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.Context;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.resource.repository.ServiceRegistryRepository;
import gr.madgik.catalogue.service.sync.ServiceSync;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ServiceCatalogueFactory {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCatalogueFactory.class);
    private final ServiceRegistryRepository resourceRepository;
    private final ServiceSync serviceSync;

    public ServiceCatalogueFactory(ServiceRegistryRepository resourceRepository, ServiceSync serviceSync) {
        this.resourceRepository = resourceRepository;
        this.serviceSync = serviceSync;
    }

    @Bean
    public Catalogue<ServiceBundle, String> getServiceCatalogue() {
        Catalogue<ServiceBundle, String> catalogue = new Catalogue<>(resourceRepository);

        catalogue.registerHandler(Catalogue.Action.REGISTER, new ActionHandler<>() {
            @Override
            public void preHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service registration preHandle");
                if (serviceBundle.getService().getResourceOrganisation() == null) {
                    serviceBundle.getService().setResourceOrganisation("openaire");
                }
                serviceBundle.setId(createId(serviceBundle.getPayload()));
                serviceBundle.setStatus("approved resource");
                serviceBundle.setActive(true);

                // TODO: create logging info

                // TODO: create metadata
            }

            @Override
            public void postHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service registration postHandle");
                serviceSync.syncAdd(serviceBundle.getService());
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
//                resourceRepository.update(serviceBundle.getId(), serviceBundle);
            }

            @Override
            public void postHandle(ServiceBundle serviceBundle, Context ctx) {
                logger.info("Inside Service update postHandle");
                serviceSync.syncUpdate(serviceBundle.getService());
            }

            @Override
            public void handleError(ServiceBundle serviceBundle, Throwable throwable, Context ctx) {
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
