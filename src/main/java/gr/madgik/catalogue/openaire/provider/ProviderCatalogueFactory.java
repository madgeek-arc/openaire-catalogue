package gr.madgik.catalogue.openaire.provider;

import eu.einfracentral.domain.LoggingInfo;
import eu.einfracentral.domain.Metadata;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.Provider;
import gr.madgik.catalogue.domain.User;
import gr.madgik.catalogue.ActionHandler;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.Context;
import gr.madgik.catalogue.openaire.provider.repository.ProviderRepository;
import gr.madgik.catalogue.openaire.utils.ProviderResourcesCommonMethods;
import gr.madgik.catalogue.openaire.utils.SimpleIdCreator;
import gr.madgik.catalogue.service.sync.ProviderSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ProviderCatalogueFactory {

    private static final Logger logger = LoggerFactory.getLogger(ProviderCatalogueFactory.class);
    private final ProviderRepository providerRepository;
    private final ProviderService providerService;
    private final ProviderSync providerSync;
    private final ProviderResourcesCommonMethods commonMethods;
    private final SimpleIdCreator idCreator;
    @Value("${project.catalogue.name}")
    private String catalogueName;

    public ProviderCatalogueFactory(ProviderRepository providerRepository,
                                    ProviderSync providerSync,
                                    @Lazy ProviderService providerService,
                                    ProviderResourcesCommonMethods commonMethods,
                                    SimpleIdCreator idCreator) {
        this.providerRepository = providerRepository;
        this.providerSync = providerSync;
        this.providerService = providerService;
        this.commonMethods = commonMethods;
        this.idCreator = idCreator;
    }

    @Bean
    public Catalogue<ProviderBundle, String> getProviderCatalogue() {
        Catalogue<ProviderBundle, String> catalogue = new Catalogue<>(providerRepository);

        catalogue.registerHandler(Catalogue.Action.REGISTER, new ActionHandler<>() {
            @Override
            public ProviderBundle preHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider registration preHandle");
                User user = User.of(SecurityContextHolder.getContext().getAuthentication());
                commonMethods.onboard(providerBundle, user);
                providerBundle.setId(idCreator.createProviderId(providerBundle.getProvider()));
                addAuthenticatedUser(providerBundle.getProvider());
                providerBundle.setMetadata(Metadata.createMetadata(user.getFullname(), user.getEmail()));

                // validate
                providerService.validate(providerBundle);

                return providerBundle;
            }

            @Override
            public void postHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider registration postHandle");
//                serviceSync.syncAdd(providerBundle.getProvider());
            }

            @Override
            public void handleError(ProviderBundle providerBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Provider registration handleError");
            }
        });

        catalogue.registerHandler(Catalogue.Action.UPDATE, new ActionHandler<>() {
            @Override
            public ProviderBundle preHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider update preHandle");

                ProviderBundle existing = providerRepository.get(providerBundle.getId());
                existing.setProvider(providerBundle.getProvider());
                providerBundle = existing;
                providerBundle.getProvider().setCatalogueId(catalogueName);

                // validate
                commonMethods.prohibitCatalogueIdChange(providerBundle.getProvider().getCatalogueId());
                providerService.validate(providerBundle);

                User user = User.of(SecurityContextHolder.getContext().getAuthentication());

                providerBundle.setMetadata(Metadata.updateMetadata(providerBundle.getMetadata(), user.getFullname(),
                        user.getEmail()));

                List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(providerBundle, user);
                LoggingInfo loggingInfo = commonMethods.createLoggingInfo(user, LoggingInfo.Types.UPDATE.getKey(),
                        LoggingInfo.ActionType.UPDATED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
                providerBundle.setLoggingInfo(loggingInfoList);
                providerBundle.setLatestUpdateInfo(loggingInfo);

                return providerBundle;
            }

            @Override
            public void postHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider update postHandle");
//                serviceSync.syncUpdate(providerBundle.getProvider());
            }

            @Override
            public void handleError(ProviderBundle providerBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Provider update handleError");
            }
        });

        catalogue.registerHandler(Catalogue.Action.DELETE, new ActionHandler<>() {
            @Override
            public ProviderBundle preHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider delete preHandle");
                return providerBundle;
            }

            @Override
            public void postHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider delete postHandle");
//                serviceSync.syncDelete(providerBundle.getProvider());
            }

            @Override
            public void handleError(ProviderBundle providerBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Provider delete handleError");
            }
        });


        return catalogue;
    }

    private void addAuthenticatedUser(Provider provider) {
        List<eu.einfracentral.domain.User> users = provider.getUsers();
        User authUser = User.of(SecurityContextHolder.getContext().getAuthentication());
        if (users == null) {
            users = new ArrayList<>();
        }
        if (users.stream().noneMatch(u -> u.getEmail().equalsIgnoreCase(authUser.getEmail()))) {
            users.add(transformUser(authUser));
            provider.setUsers(users);
        }
    }

    private eu.einfracentral.domain.User transformUser(User user) {
        eu.einfracentral.domain.User eicUser = new eu.einfracentral.domain.User();
        eicUser.setId(user.getSub());
        eicUser.setName(user.getName());
        eicUser.setSurname(user.getSurname());
        eicUser.setEmail(user.getEmail());
        return eicUser;
    }
}
