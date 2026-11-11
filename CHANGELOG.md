# Changelog

## 1.0.0 (2026-11-11)


### Features

* add a Dockerfile ([cc17267](https://github.com/madgeek-arc/openaire-catalogue/commit/cc172679a4bcf63b171a23f9856fec13c4021804))
* **admin:** administrators can add, update and delete services and datasources ([5081354](https://github.com/madgeek-arc/openaire-catalogue/commit/5081354e6cc4e35922618f2e9ce190828221d304))
* **api:** OpenAPI documentation with springdoc and a configurable server URL ([1402ef3](https://github.com/madgeek-arc/openaire-catalogue/commit/1402ef3deba7ea371c3fd41f533f3ed7fd6578c1))
* **auth:** add the onboarding team role ([246949c](https://github.com/madgeek-arc/openaire-catalogue/commit/246949c5cc07fd9fb12c2ffb87477ed7c2961447))
* **auth:** delete the AccessToken cookie on logout ([c19a3a3](https://github.com/madgeek-arc/openaire-catalogue/commit/c19a3a3b89b26cc3f9ea57187792b4c833f387f0))
* **auth:** OIDC login with an authorities mapper ([4bec8b2](https://github.com/madgeek-arc/openaire-catalogue/commit/4bec8b26f06208f959fbfa44111166c96b9c279d))
* **datasources:** fetch the registered OpenAIRE datasources from the DSM API ([ed9fb60](https://github.com/madgeek-arc/openaire-catalogue/commit/ed9fb600cc2d3d67f0426efdcf51b0a9a737b066))
* **datasources:** get the datasource of a service by the service id ([9560c23](https://github.com/madgeek-arc/openaire-catalogue/commit/9560c23e5c2f89120e7fc88ce508a9227ca3412e))
* **logging:** log4j2 logging with the user's details on each request ([7fe07a0](https://github.com/madgeek-arc/openaire-catalogue/commit/7fe07a0a277f983c51d270eeff3c89e8a715b718))
* **logging:** record verification and activation actions in loggingInfo ([69071e9](https://github.com/madgeek-arc/openaire-catalogue/commit/69071e956bf9d1d9f465a24b39125eb30a3a9e05))
* **models:** OpenAIRE models independent of the Beyond and lot1 models ([8bbb83c](https://github.com/madgeek-arc/openaire-catalogue/commit/8bbb83cab2f673763c54b9b637662902e61c3ba2))
* **monitoring:** add Sentry error reporting ([45889d9](https://github.com/madgeek-arc/openaire-catalogue/commit/45889d94cbc1c0ee69f5649e44140026a909e4a8))
* **monitoring:** add Spring Boot Actuator ([a24d604](https://github.com/madgeek-arc/openaire-catalogue/commit/a24d6040c17663b8543b8fa448f59932b297a5fc))
* **notifications:** turn off email notifications to admins and providers ([16d287f](https://github.com/madgeek-arc/openaire-catalogue/commit/16d287f597218e7818fa874d639d9b7403c5264a))
* **providers:** invitation tokens let non-admin users create providers ([d7ef924](https://github.com/madgeek-arc/openaire-catalogue/commit/d7ef924d855042ad7909a35bb7b5aed59f407490))
* **resources:** browse resource bundles ([50bd61d](https://github.com/madgeek-arc/openaire-catalogue/commit/50bd61d54026212701fde39b4b5e4be6d06f2220))
* **resources:** create, read, update and delete providers, services and datasources ([0637f57](https://github.com/madgeek-arc/openaire-catalogue/commit/0637f576dd12451a2ab56ec3e3f3b35cf83fa78b))
* **resources:** filter resources with query parameters ([a54c8f1](https://github.com/madgeek-arc/openaire-catalogue/commit/a54c8f100d699456426503a2670cd74fe755dd7c))
* **resources:** get resources by id and by field, sorted by name ([7f24bc5](https://github.com/madgeek-arc/openaire-catalogue/commit/7f24bc54f4af926fea6a31e8c4ef7a0d91cc5fb3))
* **resources:** new resources start as pending ([8d340ba](https://github.com/madgeek-arc/openaire-catalogue/commit/8d340bac1ef4e89d349c2e7dd0726a92e696eb22))
* **resources:** order providers and services by name by default ([3785902](https://github.com/madgeek-arc/openaire-catalogue/commit/37859024f6012164cb55ba34431931c7eac2401e))
* **services:** use eosc as the default catalogue id of registered services ([6c353e6](https://github.com/madgeek-arc/openaire-catalogue/commit/6c353e6a35fbd88da8995135f739e0fa8136e561))
* **services:** validate services against the vocabularies ([048c8d1](https://github.com/madgeek-arc/openaire-catalogue/commit/048c8d1d9acd14a3a7e48aee3a921c27562f3960))
* **session:** Redis-backed sessions with configurable cookie path and secure flag ([5ea1be7](https://github.com/madgeek-arc/openaire-catalogue/commit/5ea1be71bc0aed990e63547129ac366508f47a7d))

### Bug Fixes

* **auth:** create the user from the authentication token correctly ([62aadae](https://github.com/madgeek-arc/openaire-catalogue/commit/62aadaea29557042c170600f12f4985ce19eb89d))
* **auth:** make logout compatible with Google ([2dd26f9](https://github.com/madgeek-arc/openaire-catalogue/commit/2dd26f9ccc1b7beff23e4aad1066e4ea842ddf73))
* **config:** load the FreeMarker configuration bean ([010ecaf](https://github.com/madgeek-arc/openaire-catalogue/commit/010ecaf5425235aebf27fbeb3909da64d7b3b0ac))
* **datasources:** avoid a NullPointerException when logging verification and activation ([250c0a0](https://github.com/madgeek-arc/openaire-catalogue/commit/250c0a0c8f4d4daabf6f1e32d59a51183ca5e54a))
* **logging:** record the correct user full name in loggingInfo ([a96af23](https://github.com/madgeek-arc/openaire-catalogue/commit/a96af23f0155380e08bc5863c8048dd845bfec8b))
* **providers:** fix the provider update authorization expression ([4618e6d](https://github.com/madgeek-arc/openaire-catalogue/commit/4618e6da101055b0f8262e05f7b64bb590079e1a))
* **providers:** register the right user as provider admin ([6929a0a](https://github.com/madgeek-arc/openaire-catalogue/commit/6929a0a8adec5c5f17c1229314b958dbe22f4cde))
* **providers:** reject invalid, malformed or reused invitation tokens ([9c7b8c8](https://github.com/madgeek-arc/openaire-catalogue/commit/9c7b8c8485a1bf332f60a5da2a5e433d23652937))
* **resources:** avoid a NullPointerException when the user id is null ([be5b4d5](https://github.com/madgeek-arc/openaire-catalogue/commit/be5b4d5582f56987b77cd9384b00810843b2e245))
* **resources:** fix getting services and datasources by id ([bad00ca](https://github.com/madgeek-arc/openaire-catalogue/commit/bad00ca4d911585c3ec95049173c01e77f4edf5c))
* **resources:** fix resource metadata updates ([33dc094](https://github.com/madgeek-arc/openaire-catalogue/commit/33dc094a01886cba297bc66921d8ca1c2a367255))
* **resources:** fix the verify and publish methods ([1e8f3d9](https://github.com/madgeek-arc/openaire-catalogue/commit/1e8f3d909df992c058cf988e21d2cebb01dc24b0))
* **resources:** getByIds no longer returns inactive resources ([bfe1dd9](https://github.com/madgeek-arc/openaire-catalogue/commit/bfe1dd9116835c70d23aa96182b863bf21168eae))
* **resourceTypes:** fix the package paths of class properties ([076b6a1](https://github.com/madgeek-arc/openaire-catalogue/commit/076b6a1894772bf791ae67295eddc489b434ab97))
* **services:** throw an exception when service registration or update fails ([d52a2ff](https://github.com/madgeek-arc/openaire-catalogue/commit/d52a2ffe3515804a2e085c4ac3b07992b542c999))

### Refactoring

* **auth:** rename the user's sub field to id ([781a81b](https://github.com/madgeek-arc/openaire-catalogue/commit/781a81b878cb943d7229c2f3f03e10480f39e4f6))
* **datasources:** use the Datasource model from eic-registry-model ([98a2785](https://github.com/madgeek-arc/openaire-catalogue/commit/98a27852324390a81513a6c23489befa57ecf663))
* remove the catalogue-starter dependency ([9c526da](https://github.com/madgeek-arc/openaire-catalogue/commit/9c526daa5e3a2c714777a9fadb4e2840e1087fdf))
* remove the resource-catalogue dependency and copy the required classes ([5470952](https://github.com/madgeek-arc/openaire-catalogue/commit/5470952d6df593590b90312ae46f6ee741f42f64))
