package gr.madgik.catalogue.openaire.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "catalogue")
public class ApplicationProperties {

    /**
     * List of Administrator emails
     */
    private Set<Object> admins;

    /**
     * List of Onboarding Team emails
     */
    private Set<Object> onboardingTeam = new HashSet<>();

    /**
     * Page to redirect after successful login
     */
    private String loginRedirect;

    /**
     * Page to redirect after logging out
     */
    private String logoutRedirect;

    private Cookie cookie;

    public Set<Object> getAdmins() {
        return admins;
    }

    public void setAdmins(Set<Object> admins) {
        this.admins = admins;
    }

    public Set<Object> getOnboardingTeam() {
        return onboardingTeam;
    }

    public void setOnboardingTeam(Set<Object> onboardingTeam) {
        this.onboardingTeam = onboardingTeam;
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

    public Cookie getCookie() {
        return cookie;
    }

    public void setCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    public static class Cookie {
        private String name;
        private String domain;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }
    }
}
