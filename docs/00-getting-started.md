# Getting Started 

## Installing and starting ldproxy

ldproxy is available on [Docker Hub](https://hub.docker.com/r/iide/ldproxy/). 

If you are new to docker, read [this](https://docs.docker.com/).

To install ldproxy, just run the following command.

```bash
docker run -d -p 7080:7080 -v ldproxy_data:/ldproxy/data -w /ldproxy iide/ldproxy:latest
```

Instead of using a relative directory where ldproxy will store its data (i.e. "ldproxy_data) you may also specify an absolute path, for example:

```bash
docker run --name ldproxy -d -p 7080:7080 -v ~/docker/ldproxy_data:/ldproxy/data -w /ldproxy iide/ldproxy:latest
```

Here, we have added `--name ldproxy` to fix the name of the docker process to "ldproxy".

You may also change the host port or other parameters to your needs. In that case you will need to adjust the other command shown on this page.

To check that the docker process is running, use

```bash
docker ps
```

which should return something similar to

```bash
CONTAINER ID        IMAGE                 COMMAND                  CREATED             STATUS              PORTS                    NAMES
62db022d9bee        iide/ldproxy:latest   "/ldproxy/bin/ldproxy"   16 minutes ago      Up 16 minutes       0.0.0.0:7080->7080/tcp   ldproxy
```

Check that ldproxy is running by opening the URI http://localhost:7080/ in a web browser.

If ldproxy is not responding, consult `ldproxy_data/log/xtraplatform.log`.

## Updating ldproxy

To update ldproxy, just remove the container and create a new one with the run command as above. For example:

```bash
docker stop ldproxy
docker rm ldproxy
docker run --name ldproxy -d -p 7080:7080 -v ~/docker/ldproxy_data:/ldproxy/data -w /ldproxy iide/ldproxy:latest
```
Your data is saved in a volume, not in the container, so your service configurations will still be there after the update.

## Adding a service

To add a service, start the ldproxy manager at http://localhost:7080/ (which redirects to http://localhost:7080/manager).

![ldproxy Manager](https://github.com/interactive-instruments/ldproxy/blob/master/docs/manager1.png)

Press `New Service`.

A dialog will appear where you can enter a service identifier and the URL of the WFS.

On this page we will use a WFS from the Netherlands as an example: http://services.rce.geovoorziening.nl/landschapsatlas/wfs?service=WFS&request=GetCapabilities

name: landschapsatlas
url: http://services.rce.geovoorziening.nl/landschapsatlas/wfs?service=WFS&request=GetCapabilities

![New Service](https://github.com/interactive-instruments/ldproxy/blob/master/docs/newservice.png)

Press `Add`.

ldproxy now analyses the WFS and configures the proxy service. Once the service is configured, it will appear in the list of proxy services.

![ldproxy Manager with Service](https://github.com/interactive-instruments/ldproxy/blob/master/docs/serviceregistered.png)

If an issue is identified, a message should explain why a service cannot be created. A typical issue are invalid or missing schemas. The logs may also contain additional information.

To start browsing the proxy service, click on the "directory symbol" of service which will lead to the main page, generated from the WFS capabilities document.

## Changing the configuration

The configuration of an ldproxy service is a JSON file in the directory `config-store/ldproxy-services` with the name of the ldproxy service, for example `landschapsatlas` in the case of the example proxy service above.

In the future, a user interface in the service manager should support editing the service configuration, but in the meantime the JSON configuration may be edited directly. Here we will explain a few typical changes that are often helpful to improve how the feature data is displayed in the HTML.

**Change the label used to display a feature type to a more human friendly name**

For example, let's change "lands2:watertorens" to "Watertorens". 

Search the configuration for the JSON object that is about the feature type. Change the property `displayName` to the new label. In the example, change `"displayName" : "lands2:watertorens"` to `"displayName" : "Watertorens"`.

**Change the label used to display a property of a feature type to a more human friendly name**

For example, change "WOONPLAATS" to "Woonplats".

Search the configuration for the JSON object that is about the attribute in the feature type. In the `text/html`section, set the property `name` to the desired name.

**Remove an attribute from the overview pages**

For example, disable that `Foto_groot` is shown in the overviews.

Search the configuration for the JSON object that is about the attribute in the feature type. In the `text/html`section, set the property `showInCollection` to `false`.

**Suppress an attribute everywhere**

For example, disable that `OBJECTID` is shown in the overviews and the page of each feature.

Search the configuration for the JSON object that is about the attribute in the feature type. In the `text/html`section, set the properties `enabled` and `showInCollection` to `false`.

**Change the label of a feature to a more useful name**

By default, the label will use the gml:id as an identifier, which in many cases will be of no use for a user.

For example "watertorens.1" will not be a useful name for a user. A better fit would be the name of the municipality.

Search the configuration for the JSON object that is about the feature type. In the `text/html` section, set the property `showInCollection` to `true` and add a new JSON property `name` with values of feature attributes in double curly brackets. E.g., `"name" : "{{Woonplaats}}"`.


