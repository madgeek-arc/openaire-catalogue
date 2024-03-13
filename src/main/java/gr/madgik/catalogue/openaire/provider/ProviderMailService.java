package gr.madgik.catalogue.openaire.provider;

import eu.einfracentral.domain.*;
import eu.openminted.registry.core.domain.FacetFilter;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import gr.madgik.catalogue.Mailer;
import gr.madgik.catalogue.openaire.domain.Service;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.provider.repository.PendingProviderRepository;
import gr.madgik.catalogue.openaire.provider.repository.ProviderRepository;
import gr.madgik.catalogue.openaire.resource.repository.PendingResourceRepository;
import gr.madgik.catalogue.openaire.resource.repository.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@org.springframework.stereotype.Service
// TODO: create application properties component
public class ProviderMailService {
    private static final Logger logger = LoggerFactory.getLogger(ProviderMailService.class);
    private final Mailer mailService;
    private final Configuration cfg;
    private final ProviderRepository providerRepository;
    private final PendingProviderRepository pendingProviderRepository;
    private final ServiceRepository serviceRepository;
    private final PendingResourceRepository pendingResourceRepository;

    @Value("${project.catalogue.name}")
    private String catalogueName;


    @Value("${webapp.homepage}")
    private String endpoint;

    @Value("${project.name:Resource Catalogue}")
    private String projectName;

    @Value("${project.registration.email:registration@catalogue.eu}")
    private String registrationEmail;

    @Value("${project.helpdesk.email}")
    private String helpdeskEmail;

    @Value("${project.helpdesk.cc}")
    private String helpdeskCC;

    @Value("${project.monitoring.email}")
    private String monitoringEmail;

    @Value("${emails.send.admin.notifications}")
    private boolean enableEmailAdminNotifications;

    @Value("${emails.send.provider.notifications}")
    private boolean enableEmailProviderNotifications;

    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;


    public ProviderMailService(Mailer mailService, FreeMarkerConfigurer cfg,
                               ProviderRepository providerRepository,
                               PendingProviderRepository pendingProviderRepository,
                               ServiceRepository serviceRepository,
                               PendingResourceRepository pendingResourceRepository) {
        this.mailService = mailService;
        this.cfg = cfg.getConfiguration();
        this.providerRepository = providerRepository;
        this.pendingProviderRepository = pendingProviderRepository;
        this.serviceRepository = serviceRepository;
        this.pendingResourceRepository = pendingResourceRepository;
    }

    @Async
    public void sendProviderMails(ProviderBundle providerBundle) {
        Map<String, Object> root = new HashMap<>();
        StringWriter out = new StringWriter();
        String providerMail;
        String regTeamMail;

        String providerSubject;
        String regTeamSubject;

        String serviceOrResource = "Resource";
        if (projectName.equalsIgnoreCase("CatRIS")) {
            serviceOrResource = "Service";
        }

        if (providerBundle == null || providerBundle.getProvider() == null) {
            throw new ResourceNotFoundException("Provider is null");
        }

        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        filter.addFilter("resource_organization", providerBundle.getId());
        List<Service> serviceList = serviceRepository.get(filter).getResults()
                .stream()
                .map(ServiceBundle::getService)
                .toList();
        Service serviceTemplate = null;
        if (!serviceList.isEmpty()) {
            root.put("service", serviceList.get(0));
            serviceTemplate = serviceList.get(0);
        } else {
            serviceTemplate = new Service();
            serviceTemplate.setName("");
        }

        providerSubject = getProviderSubject(providerBundle, serviceTemplate);
        regTeamSubject = getRegTeamSubject(providerBundle, serviceTemplate);

        root.put("serviceOrResource", serviceOrResource);
        root.put("providerBundle", providerBundle);
        root.put("endpoint", endpoint);
        root.put("project", projectName);
        root.put("registrationEmail", registrationEmail);
        // get the first user's information for the registration team email
        for (LoggingInfo loggingInfo : providerBundle.getLoggingInfo()) {
            if (loggingInfo.getActionType().equals(LoggingInfo.ActionType.REGISTERED.getKey())) {
                User user = new User();
                if (loggingInfo.getUserEmail() != null && !loggingInfo.getUserEmail().equals("")) {
                    user.setEmail(loggingInfo.getUserEmail());
                }
                if (loggingInfo.getUserFullName() != null && !loggingInfo.getUserFullName().equals("")) {
                    String[] parts = loggingInfo.getUserFullName().split(" ");
                    String name = parts[0];
                    String surname = parts[1];
                    user.setName(name);
                    user.setSurname(surname);
                }
                root.put("user", user);
                break;
            }
        }
        if (!root.containsKey("user")) {
            root.put("user", providerBundle.getProvider().getUsers().get(0));
        }

        try {
            Template temp = cfg.getTemplate("registrationTeamMailTemplate.ftl");
            temp.process(root, out);
            regTeamMail = out.getBuffer().toString();
            mailService.sendMail(registrationEmail, regTeamSubject, regTeamMail);
            logger.info("\nRecipient: {}\nTitle: {}\nMail body: \n{}", registrationEmail,
                    regTeamSubject, regTeamMail);

            temp = cfg.getTemplate("providerMailTemplate.ftl");
            for (User user : providerBundle.getProvider().getUsers()) {
                if (user.getEmail() == null || user.getEmail().equals("")) {
                    continue;
                }
                root.remove("user");
                out.getBuffer().setLength(0);
                root.put("user", user);
                root.put("project", projectName);
                temp.process(root, out);
                providerMail = out.getBuffer().toString();
                mailService.sendMail(user.getEmail(), providerSubject, providerMail);
                logger.info("\nRecipient: {}\nTitle: {}\nMail body: \n{}", user.getEmail(), providerSubject, providerMail);
            }

            out.close();
        } catch (IOException e) {
            logger.error("Error finding mail template", e);
        } catch (TemplateException e) {
            logger.error("ERROR", e);
        } catch (MessagingException e) {
            logger.error("Could not send mail", e);
        }
    }

    @Async
    public void sendCatalogueMails(CatalogueBundle catalogueBundle) {
        Map<String, Object> root = new HashMap<>();
        StringWriter out = new StringWriter();
        String catalogueMail;
        String regTeamMail;

        String catalogueSubject;
        String regTeamSubject;

        if (catalogueBundle == null || catalogueBundle.getCatalogue() == null) {
            throw new ResourceNotFoundException("Cataogue is null");
        }

        catalogueSubject = getCatalogueSubject(catalogueBundle);
        regTeamSubject = getRegTeamCatalogueSubject(catalogueBundle);

        root.put("catalogueBundle", catalogueBundle);
        root.put("endpoint", endpoint);
        root.put("project", projectName);
        root.put("registrationEmail", registrationEmail);
        // get the first user's information for the registration team email
        for (LoggingInfo loggingInfo : catalogueBundle.getLoggingInfo()) {
            if (loggingInfo.getActionType().equals(LoggingInfo.ActionType.REGISTERED.getKey())) {
                User user = new User();
                if (loggingInfo.getUserEmail() != null && !loggingInfo.getUserEmail().equals("")) {
                    user.setEmail(loggingInfo.getUserEmail());
                }
                if (loggingInfo.getUserFullName() != null && !loggingInfo.getUserFullName().equals("")) {
                    String[] parts = loggingInfo.getUserFullName().split(" ");
                    String name = parts[0];
                    String surname = parts[1];
                    user.setName(name);
                    user.setSurname(surname);
                }
                root.put("user", user);
                break;
            }
        }
        if (!root.containsKey("user")) {
            root.put("user", catalogueBundle.getCatalogue().getUsers().get(0));
        }

        try {
            Template temp = cfg.getTemplate("registrationTeamMailCatalogueTemplate.ftl");
            temp.process(root, out);
            regTeamMail = out.getBuffer().toString();
            mailService.sendMail(registrationEmail, regTeamSubject, regTeamMail);
            logger.info("\nRecipient: {}\nTitle: {}\nMail body: \n{}", registrationEmail,
                    regTeamSubject, regTeamMail);

            temp = cfg.getTemplate("catalogueMailTemplate.ftl");
            for (User user : catalogueBundle.getCatalogue().getUsers()) {
                if (user.getEmail() == null || user.getEmail().equals("")) {
                    continue;
                }
                root.remove("user");
                out.getBuffer().setLength(0);
                root.put("user", user);
                root.put("project", projectName);
                temp.process(root, out);
                catalogueMail = out.getBuffer().toString();
                mailService.sendMail(user.getEmail(), catalogueSubject, catalogueMail);
                logger.info("\nRecipient: {}\nTitle: {}\nMail body: \n{}", user.getEmail(), catalogueSubject, catalogueMail);
            }

            out.close();
        } catch (IOException e) {
            logger.error("Error finding mail template", e);
        } catch (TemplateException e) {
            logger.error("ERROR", e);
        } catch (MessagingException e) {
            logger.error("Could not send mail", e);
        }
    }

    //    @Scheduled(cron = "0 0 12 ? * 2/7") // At 12:00:00pm, every 7 days starting on Monday, every month
    public void sendEmailNotificationsToProviders() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ProviderBundle> activeProviders = providerRepository.get(ff).getResults();
        List<ProviderBundle> pendingProviders = pendingProviderRepository.get(ff).getResults();
        List<ProviderBundle> allProviders = Stream.concat(activeProviders.stream(), pendingProviders.stream()).collect(Collectors.toList());

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);

        for (ProviderBundle providerBundle : allProviders) {
            if (providerBundle.getTemplateStatus().equals("no template status")) { //FIXME: we spam even those who don't want to continue to a Resource submission
                if (providerBundle.getProvider().getUsers() == null || providerBundle.getProvider().getUsers().isEmpty()) {
                    continue;
                }
                String subject = String.format("[%s] Friendly reminder for your Provider [%s]", projectName, providerBundle.getProvider().getName());
                root.put("providerBundle", providerBundle);
                for (User user : providerBundle.getProvider().getUsers()) {
                    root.put("user", user);
                    String userRole = "provider";
                    sendMailsFromTemplate("providerOnboarding.ftl", root, subject, user.getEmail(), userRole);
                }
            }
        }
    }

    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId) {
        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        gr.madgik.catalogue.openaire.domain.ServiceBundle infraService = serviceRepository.get(resourceId, catalogueName);
        ProviderBundle providerBundle = providerRepository.get(infraService.getService().getResourceOrganisation());
        if (providerBundle.getProvider().getUsers() == null || providerBundle.getProvider().getUsers().isEmpty()) {
//            throw new ValidationException(String.format("Provider [%s]-[%s] has no Users", providerBundle.getId(), providerBundle.getProvider().getName()));
            throw new RuntimeException(String.format("Provider [%s]-[%s] has no Users", providerBundle.getId(), providerBundle.getProvider().getName()));
        }
        String subject = String.format("[%s] Your Provider [%s] has one or more outdated Resources", projectName, providerBundle.getProvider().getName());
        root.put("providerBundle", providerBundle);
        root.put("infraService", infraService);
        for (User user : providerBundle.getProvider().getUsers()) {
            root.put("user", user);
            String userRole = "provider";
            sendMailsFromTemplate("providerOutdatedResources.ftl", root, subject, user.getEmail(), userRole);
        }
    }

    public void sendEmailsForMovedResources(ProviderBundle oldProvider, ProviderBundle newProvider, ServiceBundle infraService, Authentication auth) {
        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        if (oldProvider.getProvider().getUsers() == null || oldProvider.getProvider().getUsers().isEmpty()) {
//            throw new ValidationException(String.format("Provider [%s]-[%s] has no Users", oldProvider.getId(), oldProvider.getProvider().getName()));
            throw new RuntimeException(String.format("Provider [%s]-[%s] has no Users", oldProvider.getId(), oldProvider.getProvider().getName()));
        }
        if (newProvider.getProvider().getUsers() == null || newProvider.getProvider().getUsers().isEmpty()) {
//            throw new ValidationException(String.format("Provider [%s]-[%s] has no Users", newProvider.getId(), newProvider.getProvider().getName()));
            throw new RuntimeException(String.format("Provider [%s]-[%s] has no Users", newProvider.getId(), newProvider.getProvider().getName()));
        }
        String subject = String.format("[%s] Resource [%s] has been moved from Provider [%s] to Provider [%s]", projectName, infraService.getService().getName(),
                oldProvider.getProvider().getName(), newProvider.getProvider().getName());
        String userRole = "provider";
        root.put("oldProvider", oldProvider);
        root.put("newProvider", newProvider);
        root.put("infraService", infraService);
        root.put("comment", infraService.getLoggingInfo().get(infraService.getLoggingInfo().size() - 1).getComment());

        // emails to old Provider's Users
        for (User user : oldProvider.getProvider().getUsers()) {
            root.put("user", user);
            sendMailsFromTemplate("resourceMovedOldProvider.ftl", root, subject, user.getEmail(), userRole);
        }
//        root.remove("user");

        // emails to new Provider's Users
        for (User user : newProvider.getProvider().getUsers()) {
            root.put("user", user);
            sendMailsFromTemplate("resourceMovedNewProvider.ftl", root, subject, user.getEmail(), userRole);
        }

        // emails to Admins
        userRole = "admin";
        root.put("adminFullName", User.of(auth).getFullName());
        root.put("adminEmail", User.of(auth).getEmail());
        sendMailsFromTemplate("resourceMovedEPOT.ftl", root, subject, registrationEmail, userRole);
    }

    //    @Scheduled(cron = "0 0 12 ? * 2/2") // At 12:00:00pm, every 2 days starting on Monday, every month
    public void sendEmailNotificationsToAdmins() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ProviderBundle> allProviders = providerRepository.get(ff).getResults();

        List<String> providersWaitingForInitialApproval = new ArrayList<>();
        List<String> providersWaitingForSTApproval = new ArrayList<>();
        for (ProviderBundle providerBundle : allProviders) {
            if (providerBundle.getStatus().equals("pending provider")) {
                providersWaitingForInitialApproval.add(providerBundle.getProvider().getName());
            }
            if (providerBundle.getTemplateStatus().equals("pending template")) {
                providersWaitingForSTApproval.add(providerBundle.getProvider().getName());
            }
        }

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("iaProviders", providersWaitingForInitialApproval);
        root.put("stProviders", providersWaitingForSTApproval);

        String subject = String.format("[%s] Some new Providers are pending for your approval", projectName);
        if (!providersWaitingForInitialApproval.isEmpty() || !providersWaitingForSTApproval.isEmpty()) {
            String userRole = "admin";
            sendMailsFromTemplate("adminOnboardingDigest.ftl", root, subject, registrationEmail, userRole);
        }
    }

    //    @Scheduled(cron = "0 0 12 ? * *") // At 12:00:00pm every day
//    @Scheduled(fixedDelay = 1000)
    public void dailyNotificationsToAdmins() {
        // Create timestamps for today and yesterday
        LocalDate today = LocalDate.now();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Timestamp todayTimestamp = Timestamp.valueOf(today.atStartOfDay());
        Timestamp yesterdayTimestamp = Timestamp.valueOf(yesterday.atStartOfDay());

        List<String> newProviders = new ArrayList<>();
        List<String> newServices = new ArrayList<>();
        List<String> updatedProviders = new ArrayList<>();
        List<String> updatedServices = new ArrayList<>();

        // Fetch Active/Pending Services and Active/Pending Providers
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ProviderBundle> activeProviders = providerRepository.get(ff).getResults();
        List<ProviderBundle> pendingProviders = pendingProviderRepository.get(ff).getResults();
        List<ServiceBundle> activeServices = serviceRepository.get(ff).getResults();
        List<ServiceBundle> pendingServices = pendingResourceRepository.get(ff).getResults();
        List<ProviderBundle> allProviders = Stream.concat(activeProviders.stream(), pendingProviders.stream()).collect(Collectors.toList());
        List<ServiceBundle> allServices = Stream.concat(activeServices.stream(), pendingServices.stream()).collect(Collectors.toList());
        List<Bundle> allResources = Stream.concat(allProviders.stream(), allServices.stream()).collect(Collectors.toList());

        // New & Updated Providers, Resources
        for (Bundle bundle : allResources) {
            Timestamp modified;
            Timestamp registered;
            if (bundle.getMetadata() != null) {
                if (bundle.getMetadata().getModifiedAt() == null || !bundle.getMetadata().getModifiedAt().matches("[0-9]+")) {
                    modified = new Timestamp(Long.parseLong("0"));
                } else {
                    modified = new Timestamp(Long.parseLong(bundle.getMetadata().getModifiedAt()));
                }
                if (bundle.getMetadata().getRegisteredAt() == null || !bundle.getMetadata().getRegisteredAt().matches("[0-9]+")) {
                    registered = new Timestamp(Long.parseLong("0"));
                } else {
                    registered = new Timestamp(Long.parseLong(bundle.getMetadata().getRegisteredAt()));
                }
            } else {
                continue;
            }

            if (modified.after(yesterdayTimestamp) && modified.before(todayTimestamp)) {
                if (bundle.getId().contains(".")) {
                    updatedServices.add(bundle.getId());
                } else {
                    updatedProviders.add(bundle.getId());
                }
            }
            if (registered.after(yesterdayTimestamp) && registered.before(todayTimestamp)) {
                if (bundle.getId().contains(".")) {
                    newServices.add(bundle.getId());
                } else {
                    newProviders.add(bundle.getId());
                }
            }
        }

        // Provider & Resource Activities
        Map<String, List<LoggingInfo>> loggingInfoProviderMap = new HashMap<>();
        Map<String, List<LoggingInfo>> loggingInfoServiceMap = new HashMap<>();
        List<LoggingInfo> loggingInfoProviderList = new ArrayList<>();
        List<LoggingInfo> loggingInfoServiceList = new ArrayList<>();
        Timestamp timestamp;
        for (ProviderBundle providerBundle : activeProviders) {
            loggingInfoProviderList = new ArrayList<>();
            boolean providerHasLoggingChanges = false;
            if (providerBundle.getLoggingInfo() != null) {
                List<LoggingInfo> providerLoggingInfo = providerBundle.getLoggingInfo();
                for (int i = providerLoggingInfo.size() - 1; i >= 0; i--) {
                    timestamp = new Timestamp(Long.parseLong(providerLoggingInfo.get(i).getDate()));
                    if (timestamp.after(yesterdayTimestamp) && timestamp.before(todayTimestamp)) {
                        loggingInfoProviderList.add(providerLoggingInfo.get(i));
                        providerHasLoggingChanges = true;
                    } else {
                        break;
                    }
                }
            } else {
                continue;
            }
            if (providerHasLoggingChanges) {
                loggingInfoProviderMap.put(providerBundle.getId(), loggingInfoProviderList);
            }
        }
        for (ServiceBundle infraService : activeServices) {
            loggingInfoServiceList = new ArrayList<>();
            boolean serviceHasLoggingChanges = false;
            if (infraService.getLoggingInfo() != null) {
                List<LoggingInfo> serviceLoggingInfo = infraService.getLoggingInfo();
                for (int i = serviceLoggingInfo.size() - 1; i >= 0; i--) {
                    timestamp = new Timestamp(Long.parseLong(serviceLoggingInfo.get(i).getDate()));
                    if (timestamp.after(yesterdayTimestamp) && timestamp.before(todayTimestamp)) {
                        loggingInfoServiceList.add(serviceLoggingInfo.get(i));
                        serviceHasLoggingChanges = true;
                    }
                    if (!serviceHasLoggingChanges) {
                        break;
                    }
                }
            } else {
                continue;
            }
            if (serviceHasLoggingChanges) {
                loggingInfoServiceMap.put(infraService.getId(), loggingInfoServiceList);
            }
        }

        boolean changes = true;
        if (newProviders.isEmpty() && updatedProviders.isEmpty() && newServices.isEmpty() && updatedServices.isEmpty()
                && loggingInfoProviderList.isEmpty() && loggingInfoServiceList.isEmpty()) {
            changes = false;
        }

        Map<String, Object> root = new HashMap<>();
        root.put("changes", changes);
        root.put("project", projectName);
        root.put("newProviders", newProviders);
        root.put("updatedProviders", updatedProviders);
        root.put("newServices", newServices);
        root.put("updatedServices", updatedServices);
        root.put("loggingInfoProviderMap", loggingInfoProviderMap);
        root.put("loggingInfoServiceMap", loggingInfoServiceMap);

        String subject = String.format("[%s] Daily Notification - Changes to Resources", projectName);
        String userRole = "admin";
        sendMailsFromTemplate("adminDailyDigest.ftl", root, subject, registrationEmail, userRole);
    }

    private void sendMailsFromTemplate(String templateName, Map<String, Object> root, String subject, String email, String userRole) {
        sendMailsFromTemplate(templateName, root, subject, Collections.singletonList(email), userRole);
    }

    private void sendMailsFromTemplate(String templateName, Map<String, Object> root, String subject, String email, List<String> cc, String userRole) {
        sendMailsFromTemplate(templateName, root, subject, Collections.singletonList(email), cc, userRole);
    }

    private void sendMailsFromTemplate(String templateName, Map<String, Object> root, String subject, List<String> emails, String userRole) {
        sendMailsFromTemplate(templateName, root, subject, emails, null, userRole);
    }

    private void sendMailsFromTemplate(String templateName, Map<String, Object> root, String subject, List<String> to, List<String> cc, String userRole) {
        if (to == null || to.isEmpty()) {
            logger.error("emails empty or null");
            return;
        }
        try (StringWriter out = new StringWriter()) {
            Template temp = cfg.getTemplate(templateName);
            temp.process(root, out);
            String mailBody = out.getBuffer().toString();

            if (enableEmailAdminNotifications && userRole.equals("admin")) {
                if (cc != null && !cc.isEmpty()) {
                    mailService.sendMail(to, cc, subject, mailBody);
                } else {
                    mailService.sendMail(to, subject, mailBody);
                }
            }
            if (enableEmailProviderNotifications && userRole.equals("provider")) {
                mailService.sendMail(to, subject, mailBody);
            }
            logger.info("\nRecipients: {}\nCC: {}\nTitle: {}\nMail body: \n{}", String.join(", ", to), cc, subject, mailBody);

        } catch (IOException e) {
            logger.error("Error finding mail template '{}'", templateName, e);
        } catch (TemplateException e) {
            logger.error("ERROR", e);
        } catch (MessagingException e) {
            logger.error("Could not send mail", e);
        }
    }

    private String getProviderSubject(ProviderBundle providerBundle, Service serviceTemplate) {
        if (providerBundle == null || providerBundle.getProvider() == null) {
            logger.error("Provider is null");
            return String.format("[%s]", this.projectName);
        }

        String serviceOrResource = "Resource";
        String subject;
        String providerName = providerBundle.getProvider().getName();

        if (providerBundle.getTemplateStatus().equals("no template status")) {
            switch (providerBundle.getStatus()) {
                case "pending provider":
                    subject = String.format("[%s Portal] Your application for registering [%s] " +
                                    "as a new %s Provider to the %s Portal has been received and is under review",
                            this.projectName, providerName, this.projectName, this.projectName);
                    break;
                case "rejected provider":
                    subject = String.format("[%s Portal] Your application for registering [%s] " +
                                    "as a new %s Provider to the %s Portal has been rejected",
                            this.projectName, providerName, this.projectName, this.projectName);
                    break;
                case "approved provider":
                    subject = String.format("[%s Portal] Your application for registering [%s] " +
                                    "as a new %s Provider to the %s Portal has been approved",
                            this.projectName, providerName, this.projectName, this.projectName);
                    break;
                default:
                    subject = String.format("[%s Portal] Provider Registration", this.projectName);
            }
        } else {
            switch (providerBundle.getTemplateStatus()) {
                case "pending template":
                    assert serviceTemplate != null;
                    subject = String.format("[%s Portal] Your application for registering [%s] " +
                                    "as a new %s to the %s Portal has been received and is under review",
                            this.projectName, serviceTemplate.getName(), serviceOrResource, this.projectName);
                    break;
                case "approved template":
                    if (providerBundle.isActive()) {
                        assert serviceTemplate != null;
                        subject = String.format("[%s Portal] Your application for registering [%s] " +
                                        "as a new %s to the %s Portal has been approved",
                                this.projectName, serviceTemplate.getName(), serviceOrResource, this.projectName);
                        break;
                    } else {
                        assert serviceTemplate != null;
                        subject = String.format("[%s Portal] Your %s Provider [%s] has been set to inactive",
                                projectName, serviceOrResource, providerName);
                        break;
                    }
                case "rejected template":
                    assert serviceTemplate != null;
                    subject = String.format("[%s Portal] Your application for registering [%s] " +
                                    "as a new %s to the %s Portal has been rejected",
                            this.projectName, serviceTemplate.getName(), serviceOrResource, this.projectName);
                    break;
                default:
                    subject = String.format("[%s Portal] Provider Registration", this.projectName);
            }
        }

        return subject;
    }

    private String getCatalogueSubject(CatalogueBundle catalogueBundle) {
        if (catalogueBundle == null || catalogueBundle.getCatalogue() == null) {
            logger.error("Catalogue is null");
            return String.format("[%s]", this.projectName);
        }

        String subject;
        String catalogueName = catalogueBundle.getCatalogue().getName();

        switch (catalogueBundle.getStatus()) {
            case "pending catalogue":
                subject = String.format("[%s Portal] Your application for registering [%s] " +
                                "as a new %s Catalogue to the %s Portal has been received and is under review",
                        this.projectName, catalogueName, this.projectName, this.projectName);
                break;
            case "rejected catalogue":
                subject = String.format("[%s Portal] Your application for registering [%s] " +
                                "as a new %s Catalogue to the %s Portal has been rejected",
                        this.projectName, catalogueName, this.projectName, this.projectName);
                break;
            case "approved catalogue":
                subject = String.format("[%s Portal] Your application for registering [%s] " +
                                "as a new %s Catalogue to the %s Portal has been approved",
                        this.projectName, catalogueName, this.projectName, this.projectName);
                break;
            default:
                subject = String.format("[%s Portal] Catalogue Registration", this.projectName);
        }
        return subject;
    }


    private String getRegTeamSubject(ProviderBundle providerBundle, Service serviceTemplate) {
        if (providerBundle == null || providerBundle.getProvider() == null) {
            logger.error("Provider is null");
            return String.format("[%s]", this.projectName);
        }

        String serviceOrResource = "Resource";
        if (projectName.equalsIgnoreCase("CatRIS")) {
            serviceOrResource = "Service";
        }
        String subject;
        String providerName = providerBundle.getProvider().getName();
        String providerId = providerBundle.getProvider().getId();

        if (providerBundle.getTemplateStatus().equals("no template status")) {
            switch (providerBundle.getStatus()) {
                case "pending provider":
                    subject = String.format("[%s Portal] A new application for registering [%s] - ([%s]) " +
                                    "as a new %s Provider to the %s Portal has been received and should be reviewed",
                            this.projectName, providerName, providerId, this.projectName, this.projectName);
                    break;
                case "approved provider":
                    subject = String.format("[%s Portal] The application of [%s] - ([%s]) for registering " +
                                    "as a new %s Provider has been approved",
                            this.projectName, providerName, providerId, this.projectName);
                    break;
                case "rejected provider":
                    subject = String.format("[%s Portal] The application of [%s] - ([%s]) for registering " +
                                    "as a new %s Provider has been rejected",
                            this.projectName, providerName, providerId, this.projectName);
                    break;
                default:
                    subject = String.format("[%s Portal] Provider Registration", this.projectName);
            }
        } else {
            switch (providerBundle.getTemplateStatus()) {
                case "pending template":
                    assert serviceTemplate != null;
                    subject = String.format("[%s Portal] A new application for registering [%s] " +
                                    "as a new %s to the %s Portal has been received and should be reviewed",
                            this.projectName, serviceTemplate.getId(), serviceOrResource, this.projectName);
                    break;
                case "approved template":
                    if (providerBundle.isActive()) {
                        assert serviceTemplate != null;
                        subject = String.format("[%s Portal] The application of [%s] - ([%s]) " +
                                        "for registering as a new %s has been approved",
                                this.projectName, serviceTemplate.getName(), serviceTemplate.getId(), serviceOrResource);
                        break;
                    } else {
                        assert serviceTemplate != null;
                        subject = String.format("[%s Portal] The %s Provider [%s] has been set to inactive",
                                this.projectName, serviceOrResource, providerName);
                        break;
                    }
                case "rejected template":
                    assert serviceTemplate != null;
                    subject = String.format("[%s Portal] The application of [%s] - ([%s]) " +
                                    "for registering as a %s %s has been rejected",
                            this.projectName, serviceTemplate.getName(), serviceTemplate.getId(), this.projectName, serviceOrResource);
                    break;
                default:
                    subject = String.format("[%s Portal] Provider Registration", this.projectName);
            }
        }

        return subject;
    }

    private String getRegTeamCatalogueSubject(CatalogueBundle catalogueBundle) {
        if (catalogueBundle == null || catalogueBundle.getCatalogue() == null) {
            logger.error("Catalogue is null");
            return String.format("[%s]", this.projectName);
        }

        String subject;
        String catalogueName = catalogueBundle.getCatalogue().getName();
        String catalogueId = catalogueBundle.getCatalogue().getId();

        switch (catalogueBundle.getStatus()) {
            case "pending catalogue":
                subject = String.format("[%s Portal] A new application for registering [%s] - ([%s]) " +
                                "as a new %s Catalogue to the %s Portal has been received and should be reviewed",
                        this.projectName, catalogueName, catalogueId, this.projectName, this.projectName);
                break;
            case "approved catalogue":
                subject = String.format("[%s Portal] The application of [%s] - ([%s]) for registering " +
                                "as a new %s Catalogue has been approved",
                        this.projectName, catalogueName, catalogueId, this.projectName);
                break;
            case "rejected catalogue":
                subject = String.format("[%s Portal] The application of [%s] - ([%s]) for registering " +
                                "as a new %s Catalogue has been rejected",
                        this.projectName, catalogueName, catalogueId, this.projectName);
                break;
            default:
                subject = String.format("[%s Portal] Catalogue Registration", this.projectName);
        }

        return subject;
    }

    public void sendEmailsToNewlyAddedAdmins(ProviderBundle providerBundle, List<String> admins) {

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("providerBundle", providerBundle);

        String subject = String.format("[%s Portal] Your email has been added as an Administrator for the Provider '%s'", projectName, providerBundle.getProvider().getName());

        if (admins == null) {
            for (User user : providerBundle.getProvider().getUsers()) {
                root.put("user", user);
                String userRole = "provider";
                sendMailsFromTemplate("providerAdminAdded.ftl", root, subject, user.getEmail(), userRole);
            }
        } else {
            for (User user : providerBundle.getProvider().getUsers()) {
                if (admins.contains(user.getEmail())) {
                    root.put("user", user);
                    String userRole = "provider";
                    sendMailsFromTemplate("providerAdminAdded.ftl", root, subject, user.getEmail(), userRole);
                }
            }
        }
    }

    public void sendEmailsToNewlyDeletedAdmins(ProviderBundle providerBundle, List<String> admins) {

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("providerBundle", providerBundle);

        String subject = String.format("[%s Portal] Your email has been deleted from the Administration Team of the Provider '%s'", projectName, providerBundle.getProvider().getName());

        for (User user : providerBundle.getProvider().getUsers()) {
            if (admins.contains(user.getEmail())) {
                root.put("user", user);
                String userRole = "provider";
                sendMailsFromTemplate("providerAdminDeleted.ftl", root, subject, user.getEmail(), userRole);
            }
        }
    }

    public void sendEmailsToNewlyAddedCatalogueAdmins(CatalogueBundle catalogueBundle, List<String> admins) {

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("catalogueBundle", catalogueBundle);

        String subject = String.format("[%s Portal] Your email has been added as an Administrator for the Catalogue '%s'", projectName, catalogueBundle.getCatalogue().getName());

        if (admins == null) {
            for (User user : catalogueBundle.getCatalogue().getUsers()) {
                root.put("user", user);
                String userRole = "provider";
                sendMailsFromTemplate("catalogueAdminAdded.ftl", root, subject, user.getEmail(), userRole);
            }
        } else {
            for (User user : catalogueBundle.getCatalogue().getUsers()) {
                if (admins.contains(user.getEmail())) {
                    root.put("user", user);
                    String userRole = "provider";
                    sendMailsFromTemplate("catalogueAdminAdded.ftl", root, subject, user.getEmail(), userRole);
                }
            }
        }
    }

    public void sendEmailsToNewlyDeletedCatalogueAdmins(CatalogueBundle catalogueBundle, List<String> admins) {

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("catalogueBundle", catalogueBundle);

        String subject = String.format("[%s Portal] Your email has been deleted from the Administration Team of the Catalogue '%s'", projectName, catalogueBundle.getCatalogue().getName());

        for (User user : catalogueBundle.getCatalogue().getUsers()) {
            if (admins.contains(user.getEmail())) {
                root.put("user", user);
                String userRole = "provider";
                sendMailsFromTemplate("catalogueAdminDeleted.ftl", root, subject, user.getEmail(), userRole);
            }
        }
    }

    public void informPortalAdminsForProviderDeletion(ProviderBundle provider, User user) {
        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("user", user);
        root.put("providerBundle", provider);

        String subject = String.format("[%s] Provider Deletion Request", projectName);
        String userRole = "admin";
        sendMailsFromTemplate("providerDeletionRequest.ftl", root, subject, registrationEmail, userRole);
    }

    public void notifyProviderAdmins(ProviderBundle provider) {
        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("providerBundle", provider);

        String subject = String.format("[%s] Your Provider [%s]-[%s] has been Deleted", projectName,
                provider.getProvider().getId(), provider.getProvider().getName());
        for (User user : provider.getProvider().getUsers()) {
            root.put("user", user);
            String userRole = "provider";
            sendMailsFromTemplate("providerDeletion.ftl", root, subject, user.getEmail(), userRole);
        }
    }

    public void sendVocabularyCurationEmails(VocabularyCuration vocabularyCuration, String userName) {
        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("vocabularyCuration", vocabularyCuration);
        root.put("userName", userName);
        root.put("userEmail", vocabularyCuration.getVocabularyEntryRequests().get(0).getUserId());

        // send email to User
        String subject = String.format("[%s] Your Vocabulary [%s]-[%s] has been submitted", projectName,
                vocabularyCuration.getVocabulary(), vocabularyCuration.getEntryValueName());
        String userRole = "provider";
        sendMailsFromTemplate("vocabularyCurationUser.ftl", root, subject, vocabularyCuration.getVocabularyEntryRequests().get(0).getUserId(), userRole);

        // send email to Admins
        String adminSubject = String.format("[%s] A new Vocabulary Request [%s]-[%s] has been submitted", projectName,
                vocabularyCuration.getVocabulary(), vocabularyCuration.getEntryValueName());
        userRole = "admin";
        sendMailsFromTemplate("vocabularyCurationAdmin.ftl", root, adminSubject, registrationEmail, userRole);
    }

    public void approveOrRejectVocabularyCurationEmails(VocabularyCuration vocabularyCuration) {
        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("vocabularyCuration", vocabularyCuration);
        root.put("userEmail", vocabularyCuration.getVocabularyEntryRequests().get(0).getUserId());

        if (vocabularyCuration.getStatus().equals(VocabularyCuration.Status.APPROVED.getKey())) {
            // send email of Approval
            String subject = String.format("[%s] Your Vocabulary [%s]-[%s] has been approved", projectName,
                    vocabularyCuration.getVocabulary(), vocabularyCuration.getEntryValueName());
            String userRole = "provider";
            sendMailsFromTemplate("vocabularyCurationApproval.ftl", root, subject, vocabularyCuration.getVocabularyEntryRequests().get(0).getUserId(), userRole);
        } else {
            // send email of Rejection
            String subject = String.format("[%s] Your Vocabulary [%s]-[%s] has been rejected", projectName,
                    vocabularyCuration.getVocabulary(), vocabularyCuration.getEntryValueName());
            String userRole = "provider";
            sendMailsFromTemplate("vocabularyCurationRejection.ftl", root, subject, vocabularyCuration.getVocabularyEntryRequests().get(0).getUserId(), userRole);
        }
    }

    public void notifyProviderAdminsForProviderAuditing(ProviderBundle providerBundle) {

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("providerBundle", providerBundle);

        String subject = String.format("[%s Portal] Your Provider '%s' has been audited by the EPOT team", projectName, providerBundle.getProvider().getName());

        for (User user : providerBundle.getProvider().getUsers()) {
            root.put("user", user);
            String userRole = "provider";
            sendMailsFromTemplate("providerAudit.ftl", root, subject, user.getEmail(), userRole);
        }
    }

    public void notifyProviderAdminsForResourceAuditing(ServiceBundle infraService) {

        ProviderBundle providerBundle = providerRepository.get(infraService.getService().getResourceOrganisation());

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("infraService", infraService);

        String subject = String.format("[%s Portal] Your Resource '%s' has been audited by the EPOT team", projectName, infraService.getService().getName());

        for (User user : providerBundle.getProvider().getUsers()) {
            root.put("user", user);
            String userRole = "provider";
            sendMailsFromTemplate("resourceAudit.ftl", root, subject, user.getEmail(), userRole);
        }
    }

    public void notifyPortalAdminsForInvalidProviderUpdate(ProviderBundle providerBundle) {

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("providerBundle", providerBundle);

        // send email to Admins
        String subject = String.format("[%s Portal] The Provider [%s] previously marked as [invalid] has been updated", projectName, providerBundle.getProvider().getName());
        String userRole = "admin";
        sendMailsFromTemplate("invalidProviderUpdate.ftl", root, subject, registrationEmail, userRole);
    }

    public void notifyPortalAdminsForInvalidCatalogueUpdate(CatalogueBundle catalogueBundle) {

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("catalogueBundle", catalogueBundle);

        // send email to Admins
        String subject = String.format("[%s Portal] The Catalogue [%s] previously marked as [invalid] has been updated", projectName, catalogueBundle.getCatalogue().getName());
        String userRole = "admin";
        sendMailsFromTemplate("invalidCatalogueUpdate.ftl", root, subject, registrationEmail, userRole);
    }

    public void notifyPortalAdminsForInvalidResourceUpdate(ServiceBundle infraService) {

        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("infraService", infraService);

        // send email to Admins
        String subject = String.format("[%s Portal] The Resource [%s] previously marked as [invalid] has been updated", projectName, infraService.getService().getName());
        String userRole = "admin";
        sendMailsFromTemplate("invalidResourceUpdate.ftl", root, subject, registrationEmail, userRole);
    }

    public void sendEmailsForHelpdeskExtension(HelpdeskBundle helpdeskBundle, String action) {
        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("helpdeskBundle", helpdeskBundle);
        root.put("action", action);

        // send email to help@eosc-future.eu
        String userRole = "admin";
        String subject = "";
        if (action.equals("post")) {
            subject = String.format("[%s Portal] The Service [%s] has created a new Helpdesk Extension", projectName, helpdeskBundle.getHelpdesk().getServiceId());
        } else {
            subject = String.format("[%s Portal] The Service [%s] updated its Helpdesk Extension", projectName, helpdeskBundle.getHelpdesk().getServiceId());
        }
        sendMailsFromTemplate("serviceExtensionsHelpdesk.ftl", root, subject, helpdeskEmail, Collections.singletonList(helpdeskCC), userRole);
    }

    public void sendEmailsForMonitoringExtension(MonitoringBundle monitoringBundle, String action) {
        Map<String, Object> root = new HashMap<>();
        root.put("project", projectName);
        root.put("endpoint", endpoint);
        root.put("monitoringBundle", monitoringBundle);
        root.put("action", action);

        // send email to argo@einfra.grnet.gr
        String userRole = "admin";
        String subject = "";
        if (action.equals("post")) {
            subject = String.format("[%s Portal] The Service [%s] has created a new Monitoring Extension", projectName, monitoringBundle.getMonitoring().getServiceId());
        } else {
            subject = String.format("[%s Portal] The Service [%s] updated its Monitoring Extension", projectName, monitoringBundle.getMonitoring().getServiceId());
        }
        sendMailsFromTemplate("serviceExtensionsMonitoring.ftl", root, subject, monitoringEmail, userRole);
    }
}
