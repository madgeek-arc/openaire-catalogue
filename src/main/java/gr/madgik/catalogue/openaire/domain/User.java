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

package gr.madgik.catalogue.openaire.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class User {
    private static final Logger logger = LoggerFactory.getLogger(User.class);

    private String id;
    private String email;
    private String name;
    private String surname;
    private String fullname;
    private List<String> roles;

    public User() {
    }

    public static User of(Authentication auth) {
        logger.trace("Creating UserService from Authentication: %n{}", auth);
        User user = new User();
        if (auth == null) {
            throw new InsufficientAuthenticationException("You are not authenticated, please log in.");
        } else if (auth.getPrincipal() instanceof OidcUser) {
            OidcUser principal = ((OidcUser) auth.getPrincipal());
            user.id = principal.getSubject();
            user.email = principal.getEmail();
            user.name = principal.getGivenName();
            user.surname = principal.getFamilyName();
            user.fullname = principal.getFullName();
            user.roles = auth.getAuthorities().stream().map(Objects::toString).collect(Collectors.toList());
        } else if (auth instanceof OAuth2AuthenticationToken) {
            OAuth2User principal = ((OAuth2AuthenticationToken) auth).getPrincipal();
            user.id = principal.getAttribute("subject");
            user.email = principal.getAttribute("email");
            user.name = principal.getAttribute("givenName");
            user.surname = principal.getAttribute("familyName");
            user.fullname = principal.getAttribute("fullName");
            user.roles = auth.getAuthorities().stream().map(Objects::toString).collect(Collectors.toList());
        } else {
            throw new InsufficientAuthenticationException("Could not create user. Insufficient user authentication");
        }
        logger.debug("UserService from Authentication: {}", user);
        return user;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public static String getId(Authentication auth) {
        logger.trace("Retrieving id from Authentication: %n{}", auth);
        String id = null;
        if (auth == null) {
            throw new InsufficientAuthenticationException("You are not authenticated, please log in.");
        } else if (auth.getPrincipal() instanceof OidcUser) {
            OidcUser principal = ((OidcUser) auth.getPrincipal());
            id = principal.getEmail();
        } else if (auth instanceof OAuth2AuthenticationToken) {
            OAuth2User principal = ((OAuth2AuthenticationToken) auth).getPrincipal();
            id = principal.getAttribute("email");
        } else {
            throw new InsufficientAuthenticationException("Could not create user. Insufficient user authentication");
        }
        return id;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", fullname='" + fullname + '\'' +
                ", roles=" + roles +
                '}';
    }
}
