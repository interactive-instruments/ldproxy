# Adding a service

To add a proxy service for a WFS, click on the plus sign at the top.

![ldproxy Manager - add](../img/manager-02.png)

A dialog will appear where you can enter a service identifier and the URL of the WFS.

![ldproxy Manager - new service](../img/manager-03.png)

We will use a WFS from the Netherlands as an example:

- ID: landschapsatlas
- WFS URL: [http://services.rce.geovoorziening.nl/landschapsatlas/wfs](http://services.rce.geovoorziening.nl/landschapsatlas/wfs?service=WFS&request=GetCapabilities)

![ldproxy Manager - add landschapsatlas](../img/manager-04.png)

When you press `Add`, ldproxy will analyze the WFS and configure the proxy service. 

![ldproxy Manager - adding landschapsatlas](../img/manager-05.png)

Once the service is configured, it will switch its state from `Initializing` to `Online`.

![ldproxy Manager - added landschapsatlas](../img/manager-06.png)

If an issue with the service is identified, a message appears. Clicking on the arrow shows the details why a service cannot be created. A typical issue are invalid or missing schemas.

![ldproxy Manager - failure](../img/manager-07.png)

If you click on the service in the service list, the detail view will be opened.

![ldproxy Manager - detail view](../img/manager-08.png)

To start browsing the proxy service, click on `View` at the right. That will lead to the main page, which is generated from the WFS capabilities document.

![ldproxy Manager - landing page](../img/landing-page-01.png)