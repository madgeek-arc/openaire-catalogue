package gr.madgik.catalogue.openaire.utils;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.LoggingInfo;
import eu.einfracentral.domain.ProviderBundle;
import gr.madgik.catalogue.domain.User;
import gr.madgik.catalogue.exception.ValidationException;
import gr.madgik.catalogue.openaire.domain.DatasourceBundle;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ProviderResourcesCommonMethods {

    @Value("${project.catalogue.name}")
    private String catalogueName;

    public ProviderResourcesCommonMethods() {
    }

    public void onboard(Bundle<?> bundle, User user) {
        bundle.setActive(false);

        if (bundle instanceof ProviderBundle providerBundle) {
            providerBundle.getProvider().setCatalogueId(catalogueName);
            providerBundle.setStatus("pending provider");
            providerBundle.setTemplateStatus("no template status");
        } else if (bundle instanceof ServiceBundle serviceBundle) {
            serviceBundle.getService().setCatalogueId(catalogueName);
            serviceBundle.setStatus("pending resource");
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
        List<LoggingInfo> loggingInfoList = returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, user);
        bundle.setLoggingInfo(loggingInfoList);
        bundle.setLatestOnboardingInfo(loggingInfoList.get(0));
    }

    public void logVerificationAndActivation(Bundle<?> bundle, String status, Boolean active) {
        User user = User.of(SecurityContextHolder.getContext().getAuthentication());
        List<LoggingInfo> loggingInfoList = returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, user);
        LoggingInfo loggingInfo = null;
        if (status != null) {
            switch (status) {
                case "approved provider":
                case "approved resource":
                case "approved datasource":
                    loggingInfo = createLoggingInfo(user, LoggingInfo.Types.ONBOARD.getKey(),
                            LoggingInfo.ActionType.APPROVED.getKey());
                    break;
                case "rejected provider":
                case "rejected resource":
                case "rejected datasource":
                    loggingInfo = createLoggingInfo(user, LoggingInfo.Types.ONBOARD.getKey(),
                            LoggingInfo.ActionType.REJECTED.getKey());
                    break;
                default:
                    break;
            }
        } else {
            if (active) {
                loggingInfo = createLoggingInfo(user, LoggingInfo.Types.UPDATE.getKey(),
                        LoggingInfo.ActionType.ACTIVATED.getKey());
            } else {
                loggingInfo = createLoggingInfo(user, LoggingInfo.Types.UPDATE.getKey(),
                        LoggingInfo.ActionType.DEACTIVATED.getKey());
            }
        }
        loggingInfoList.add(loggingInfo);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate).reversed());
        bundle.setLoggingInfo(loggingInfoList);

        // latestOnboardingInfo
        bundle.setLatestOnboardingInfo(loggingInfo);
    }

    public List<LoggingInfo> returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(Bundle<?> bundle, User user) {
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        if (bundle.getLoggingInfo() != null && !bundle.getLoggingInfo().isEmpty()) {
            loggingInfoList = bundle.getLoggingInfo();
        } else {
            loggingInfoList.add(createLoggingInfo(user, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.REGISTERED.getKey()));
        }
        return loggingInfoList;
    }

    public LoggingInfo createLoggingInfo(User user, String type, String actionType) {
        LoggingInfo ret = new LoggingInfo();
        ret.setDate(String.valueOf(System.currentTimeMillis()));
        ret.setType(type);
        ret.setActionType(actionType);
        ret.setUserEmail(user.getEmail());
        ret.setUserFullName(user.getFullname());
        ret.setUserRole(String.join(",", user.getRoles()));
        return ret;
    }

    public void prohibitCatalogueIdChange(String catalogueId) {
        if (!catalogueId.equals(catalogueName)) {
            throw new ValidationException("You cannot change catalogueId");
        }
    }
}
