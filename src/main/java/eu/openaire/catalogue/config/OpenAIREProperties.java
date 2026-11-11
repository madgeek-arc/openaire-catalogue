package eu.openaire.catalogue.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "openaire")
public class OpenAIREProperties {

    /**
     * Name used in the subject and body of notification emails.
     */
    private String projectName = "OpenAIRE";

    /**
     * Catalogue id written into the payload of new records.
     */
    private String catalogueId = "openaire";

    /**
     * Address of the portal, linked from notification emails.
     */
    private String homepage = "";

    /**
     * Where the browser goes after a successful login.
     */
    private String loginRedirect = "/";

    /**
     * Where the browser goes after a logout.
     */
    private String logoutRedirect = "/";

    /**
     * Origins allowed to call the API from a browser. Empty disables cross-origin requests.
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * Channels a record may be listed on. Each has its own API under {@code /<channel>/}, except those used only by
     * {@code /eosc}.
     */
    private List<String> channels = new ArrayList<>(List.of("oa", "node"));

    /**
     * Channel whose records {@code /eosc} returns. One of {@link #channels}.
     */
    private String eoscChannel = "node";

    /**
     * Whether the OpenAIRE models are stored at startup and applied to their resource types.
     */
    private boolean loadModels = true;

    private Invitations invitations = new Invitations();

    private Emails emails = new Emails();

    private Dsm dsm = new Dsm();

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public String getLoginRedirect() {
        return loginRedirect;
    }

    public void setLoginRedirect(String loginRedirect) {
        this.loginRedirect = loginRedirect;
    }

    public String getLogoutRedirect() {
        return logoutRedirect;
    }

    public void setLogoutRedirect(String logoutRedirect) {
        this.logoutRedirect = logoutRedirect;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public String getEoscChannel() {
        return eoscChannel;
    }

    public void setEoscChannel(String eoscChannel) {
        this.eoscChannel = eoscChannel;
    }

    public boolean isLoadModels() {
        return loadModels;
    }

    public void setLoadModels(boolean loadModels) {
        this.loadModels = loadModels;
    }

    public Invitations getInvitations() {
        return invitations;
    }

    public void setInvitations(Invitations invitations) {
        this.invitations = invitations;
    }

    public Emails getEmails() {
        return emails;
    }

    public void setEmails(Emails emails) {
        this.emails = emails;
    }

    public Dsm getDsm() {
        return dsm;
    }

    public void setDsm(Dsm dsm) {
        this.dsm = dsm;
    }

    public static class Invitations {

        /**
         * Whether registering a provider requires a valid invitation from users that are neither administrators nor
         * members of the onboarding team.
         */
        private boolean required = true;

        /**
         * Name of the request parameter carrying the invitation token.
         */
        private String parameter = "invitation";

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public String getParameter() {
            return parameter;
        }

        public void setParameter(String parameter) {
            this.parameter = parameter;
        }
    }

    public static class Emails {

        /**
         * Whether notification emails are sent. When off, they are only logged.
         */
        private boolean enabled = false;

        /**
         * Sender address.
         */
        private String from = "";

        /**
         * Address of the team that reviews registrations.
         */
        private String registrationTo = "";

        /**
         * Whether provider users are notified, besides the registration team.
         */
        private boolean providerNotifications = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getRegistrationTo() {
            return registrationTo;
        }

        public void setRegistrationTo(String registrationTo) {
            this.registrationTo = registrationTo;
        }

        public boolean isProviderNotifications() {
            return providerNotifications;
        }

        public void setProviderNotifications(boolean providerNotifications) {
            this.providerNotifications = providerNotifications;
        }
    }

    public static class Dsm {

        /**
         * Base address of the OpenAIRE datasource management API, for example
         * {@code https://services.openaire.eu/openaire/}.
         */
        private String url = "";

        /**
         * Base address of the OpenAIRE PROVIDE API, which returns a single enriched datasource.
         */
        private String provideUrl = "";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getProvideUrl() {
            return provideUrl;
        }

        public void setProvideUrl(String provideUrl) {
            this.provideUrl = provideUrl;
        }
    }
}
