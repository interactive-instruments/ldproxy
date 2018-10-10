# Webserver

#### External URL path
If you run another webserver in front of ldproxy and change the path under which services are available (`/rest/services`), you have to configure the new path here. Two examples:

 - Moving services to the root folder: if you e.g. configure a `ProxyPass` in the Apache HTTP Server from `https://example.org` to `http://ldproxy/rest/services`, set the path to blank. This is the typical use case when you want to publish the services on the web, but not the manager.

 - Prefixing all paths: if you e.g. configure a `ProxyPass` in the Apache HTTP Server from `https://example.org/ldproxy` to `http://ldproxy`, set the path to `/ldproxy/rest/services`. In this case the manager would also be published under `https://example.org/ldproxy/manager`.