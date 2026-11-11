# Authorization

Each signed-in user gets a set of roles. The roles come from the user's email and from roles held at the identity
provider (IdP). The roles decide what the user may do in the catalogue.

## Roles

| Role                   | Granted when                                                                                      | Grants                                                                    |
|------------------------|---------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------|
| `ROLE_USER`            | always, for every signed-in user                                                                  | the signed-in routes (`/user/**`, `/invitations/**`, `/request/**`); registering providers, with an invitation when `openaire.invitations.required` |
| `ROLE_ADMIN`           | the user matches `openaire.roles.admin`                                                           | every route and every record                                              |
| `ROLE_ONBOARDING_TEAM` | the user matches `openaire.roles.onboarding-team`                                                 | reviewing and managing every record                                       |
| `ROLE_ADMIN_<CHANNEL>` | the user matches `openaire.roles.channels.<channel>`                                              | reviewing and managing the records listed on that channel                 |

A user matches a role setting when their email is in its `emails`, **or** they have an IdP role listed in its
`idp-roles`:

| Setting                                                              | Default `idp-roles`   |
|----------------------------------------------------------------------|-----------------------|
| `openaire.roles.admin.emails`, `openaire.roles.admin.idp-roles`      | `Super Administrator` |
| `openaire.roles.onboarding-team.emails`, `.idp-roles`                | none                  |
| `openaire.roles.channels.<channel>.emails`, `.idp-roles`             | none                  |

Setting `openaire.roles.admin.idp-roles` replaces the `Super Administrator` default. `<channel>` must be one of
`openaire.channels`, and `<CHANNEL>` is that channel in upper case, e.g. `ROLE_ADMIN_OA`, `ROLE_ADMIN_NODE`. Emails are
compared ignoring case and surrounding spaces.

Besides roles, a provider is managed by the users listed in it, a service by the users of its provider, and a datasource
by those who manage its service. These are matched by email, not by role.

## Identity provider roles

`openaire.roles.idp-role-claims` lists the claims that hold the user's IdP roles. Dots separate nested keys, and a `{client}`
segment stands for the client id. When it is empty, IdP roles are ignored and roles are granted by email only.

```properties
# OpenAIRE AAI (default)
openaire.roles.idp-role-claims=eduperson_entitlement
# Keycloak
openaire.roles.idp-role-claims=realm_access.roles,resource_access.{client}.roles
```

Every value found there is an IdP role as-is. An OpenAIRE AAI entitlement of a group member is also read as the decoded
name of its group:

| Entitlement                                                                       | IdP roles                                  |
|-----------------------------------------------------------------------------------|--------------------------------------------|
| `urn:geant:openaire.eu:group:Super+Administrator:role=member#aai.openaire.eu`     | the URN and `Super Administrator`          |
| `urn:geant:openaire.eu:group:Catalogue%20Curators:role=member#aai.openaire.eu`    | the URN and `Catalogue Curators`           |
| `urn:geant:openaire.eu:group:Catalogue:oa:role=member#aai.openaire.eu` (subgroup) | the URN only; subgroups are not mapped yet |
| an entitlement with another role, e.g. `role=manager`                             | the URN only                               |

OpenAIRE AAI sends `eduperson_entitlement` only when the client requests the `eduperson_entitlement` scope:

```properties
spring.security.oauth2.client.registration.openaire.scope=openid,email,profile,eduperson_entitlement
```

## Sign-in

| Client                     | How roles are read                                                                                       |
|----------------------------|----------------------------------------------------------------------------------------------------------|
| browser, OIDC login        | email and IdP roles from the ID token and user info, and from the access token when it is a JWT signed by the provider |
| browser, plain OAuth2 login | email only                                                                                              |
| API client, bearer token   | email and IdP roles from the token introspection response; a token without an email is refused           |

Roles are fixed at sign-in, so a browser user sees a role change from their next sign-in. A bearer token is
introspected on every request.

## Checking roles

Routes and methods check roles with `hasRole`, which adds the `ROLE_` prefix:

```java
@PreAuthorize("hasRole('ADMIN')")   // needs ROLE_ADMIN
```

Every route not listed as public or signed-in needs `ROLE_ADMIN`.

`GET /user/info` returns the signed-in user with their roles without the `ROLE_` prefix, so frontends check plain names:

```json
{
  "sub": "…",
  "email": "admin@example.org",
  "name": "Ad",
  "surname": "Min",
  "roles": ["ADMIN", "ADMIN_OA", "USER"]
}
```

## Example

```properties
openaire.roles.idp-role-claims=eduperson_entitlement
openaire.roles.admin.emails=admin@example.org
openaire.roles.onboarding-team.emails=onboarding@example.org
openaire.roles.channels.oa.emails=oa-curator@example.org
openaire.roles.channels.oa.idp-roles=Catalogue Curators
```

| User                                          | Roles                                    |
|-----------------------------------------------|------------------------------------------|
| `admin@example.org`                           | `ROLE_USER`, `ROLE_ADMIN`                |
| a member of the AAI group `Super Administrator` | `ROLE_USER`, `ROLE_ADMIN`              |
| `onboarding@example.org`                      | `ROLE_USER`, `ROLE_ONBOARDING_TEAM`      |
| `oa-curator@example.org`                      | `ROLE_USER`, `ROLE_ADMIN_OA`             |
| a member of the AAI group `Catalogue Curators` | `ROLE_USER`, `ROLE_ADMIN_OA`            |
| anyone else                                   | `ROLE_USER`                              |
