# Upgrades

## Software

How exactly the software is upgraded depends on your container runtime. The first step is to identify and set the desired [image tag](https://hub.docker.com/r/iide/ldproxy/tags), followed by triggering the actual update.

The releases use semantic versioning, which means for minor and patch versions no further actions are needed and the upgrade should be seamless. For major versions you should make sure that your configuration files are up-to-date (see below) and check the [release notes](https://github.com/interactive-instruments/ldproxy/releases) for breaking changes.

## Configuration

The syntax and structure of configuration files will change over time. Deprecations can happen in any minor version, major versions will remove all deprecated options and might introduce other breaking changes.

To keep your configurations up-to-date you can use the CLI tool [xtracfg](../cli/xtracfg). It allows you to check your configurations for deprecated options and errors and can also automatically upgrade your configurations to the recent version.
