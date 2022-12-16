# Files

All deployment-specific files except the feature provider are located in the data folder (typically "data"). It is always located outside the Docker container.

The data directory typically contains the following files and directories:

* `cfg.yml`: The [configuration file for global settings](70-reference.md).
* `api-resources`: A repository of resources or sub-resources that can be accessed through the API and modified either by the administrator or through the API. Examples include styles, map symbols, JSON-LD contexts, etc. For more details, see the [API modules](../services/building-blocks/README.md). If a module is or was never activated, then the corresponding directories are also missing.
* `cache`: The cache for resources that are cached by ldproxy for performance reasons. Currently these are only the Vector Tiles for the [module "Tiles"](../services/building-blocks/vector_tiles.md).
* `logs`: The log files according to the settings in `cfg.yml`.
* `store`: The [ldproxy configuration files](40-store.md).
* `templates`: Mustache templates for the HTML pages that override the default templates of ldproxy.
* `tmp`: A directory for temporary data. The contents can be deleted if necessary when ldproxy is stopped. It contains, for example, the cache of the OSGi bundles.
