package gr.madgik.catalogue.openaire.utils;

import eu.einfracentral.domain.*;
import gr.madgik.catalogue.SecurityService;
import gr.madgik.catalogue.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProviderResourcesCommonMethods {

    private final SecurityService securityService;
    @Value("${project.catalogue.name}")
    private String catalogueName;

    public ProviderResourcesCommonMethods(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void onboard(Bundle<?> bundle, Authentication auth) {
        bundle.setActive(false);

        if (bundle instanceof ProviderBundle providerBundle) {
            providerBundle.getProvider().setCatalogueId(catalogueName);
            providerBundle.setStatus("pending provider");
            providerBundle.setTemplateStatus("no template status");
        } else if (bundle instanceof ServiceBundle serviceBundle) {
            serviceBundle.getService().setCatalogueId(catalogueName);
            serviceBundle.setStatus("pending service");
            if (serviceBundle.getService().getResourceOrganisation() == null ||
                    serviceBundle.getService().getResourceOrganisation().equals("")) {
                serviceBundle.getService().setResourceOrganisation("openaire");
            }
        } else if (bundle instanceof DatasourceBundle datasourceBundle) {
            datasourceBundle.getDatasource().setCatalogueId(catalogueName);
            datasourceBundle.setStatus("pending datasource");
            if (datasourceBundle.getDatasource().getServiceId() == null ||
                    datasourceBundle.getDatasource().getServiceId().equals("")) {
                throw new ValidationException("Service ID cannot be empty.");
            }
        }

        // loggingInfo
        List<LoggingInfo> loggingInfoList = returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, auth);
        bundle.setLoggingInfo(loggingInfoList);
        bundle.setLatestOnboardingInfo(loggingInfoList.get(0));
    }

    public void prohibitCatalogueIdChange(String catalogueId) {
        if (!catalogueId.equals(catalogueName)) {
            throw new ValidationException("You cannot change catalogueId");
        }
    }

    public List<LoggingInfo> returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(Bundle<?> bundle, Authentication auth) {
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        if (bundle.getLoggingInfo() != null && !bundle.getLoggingInfo().isEmpty()) {
            loggingInfoList = bundle.getLoggingInfo();
        } else {
            loggingInfoList.add(createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.REGISTERED.getKey()));
        }
        return loggingInfoList;
    }

    public LoggingInfo createLoggingInfo(Authentication auth, String type, String actionType) {
        return LoggingInfo.createLoggingInfoEntry(auth, securityService.getRoleName(auth), type, actionType);
    }
}
