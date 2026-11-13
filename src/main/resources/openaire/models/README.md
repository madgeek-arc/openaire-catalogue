# OpenAIRE models

The reviewed sources of the `m-oa-service`, `m-oa-provider` and `m-oa-datasource` models. The application doesn't load
them: models and their resource types are stored in the catalogue and travel in registry dumps
(`GET /api/dump/?schema=true`). To change a live model, `PUT /api/forms/models/{id}`; its resource type is then
updated and its records reindexed.

The tests check these files against the lot1 classes, and the migration pipeline reads them with `--models`.
