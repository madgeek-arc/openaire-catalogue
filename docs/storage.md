# Record storage

Providers, services and datasources are stored in registry as JSON maps, not as lot1 bundle classes.

## Resource types

The resource types `provider`, `service` and `datasource` are created from the `m-oa-provider`, `m-oa-service` and
`m-oa-datasource` models with:

| Property | Value                     |
|----------|---------------------------|
| `class`  | `java.util.LinkedHashMap` |

They are **not** typed as the lot1 `ProviderBundle`, `ServiceBundle` or `DatasourceBundle`. Records are read and written
as maps, so every field is kept as stored, including fields the lot1 classes don't have:

- `extras` in the payload, which the lot1 `Provider`, `Service` and `Datasource` classes don't declare;
- `properties` in the bundle, which the lot1 `Bundle` class doesn't declare. It holds the channels.

The `class` is part of the resource type, so it is included in registry dumps taken with `schema=true`. Don't change it
to a lot1 bundle class, and don't load this data into an application that reads these types as lot1 bundles. Saving a
record through those classes silently drops `extras` and `properties.channels`.

## Record layout

The payload sits under the key of its resource type. The bundle fields sit beside it:

```json
{
  "id": "...",
  "service": { "...": "payload, as defined by the m-oa-service model" },
  "status": "pending | approved | rejected",
  "active": false,
  "suspended": false,
  "catalogueId": "openaire",
  "metadata": { "registeredAt": "...", "modifiedAt": "..." },
  "loggingInfo": [ ],
  "latestOnboardingInfo": { },
  "latestUpdateInfo": { },
  "properties": {
    "channels": [ "oa", "node" ]
  }
}
```

`properties.channels` lists the channels a record is shown on, independently for each provider, service and datasource.
A record with no `channels` is on no channel.

## Index fields

Besides the fields of the model, each record type is indexed on:

| Index field                  | Path                                |
|------------------------------|-------------------------------------|
| `status`                     | `$.status`                          |
| `active`                     | `$.active`                          |
| `suspended`                  | `$.suspended`                       |
| `registeredAt`               | `$.metadata.registeredAt`           |
| `modifiedAt`                 | `$.metadata.modifiedAt`             |
| `latestOnboardingDate`       | `$.latestOnboardingInfo.date`       |
| `latestOnboardingActionType` | `$.latestOnboardingInfo.actionType` |
| `latestUpdateDate`           | `$.latestUpdateInfo.date`           |
| `latestUpdateActionType`     | `$.latestUpdateInfo.actionType`     |
| `channels` (multi-valued)    | `$.properties.channels[*]`          |

## lot1 classes

The lot1 classes are used only to write the read-only `/eosc/v1` routes (services only for now): each payload is read into the lot1 `Provider`,
`Service` or `Datasource` and written from it, so fields outside those classes, such as `extras`, are left out of that
output. They are never used to store records.
