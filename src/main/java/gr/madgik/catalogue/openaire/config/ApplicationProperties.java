/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        private String path = "/";
        private boolean secure = true;

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

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }
    }
}
