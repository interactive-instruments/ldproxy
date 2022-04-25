# Deploying ldproxy

The recommended way to deploy ldproxy is using Docker, an open source container platform. Docker images for ldproxy are available on [Docker Hub](https://hub.docker.com/r/iide/ldproxy/).

## Prerequisites

To deploy ldproxy, you will need an installation of Docker. Docker is available for Linux, Windows and Mac. You will find detailed installation guides for each platform [here](https://docs.docker.com/).

## Installing and starting ldproxy

To install ldproxy, just run the following command on a machine with Docker installed:

```bash
docker run -d -p 7080:7080 -v ldproxy_data:/ldproxy/data iide/ldproxy:latest
```

This will download the latest stable ldproxy image, deploy it as a new container, make the web application available at port 7080 and save your application data in a Docker provided directory outside of the container.

Instead of using a Docker provided directory where ldproxy will store its data (i.e. "ldproxy_data) you may specify an absolute path, for example:

```bash
docker run --name ldproxy -d -p 7080:7080 -v ~/docker/ldproxy_data:/ldproxy/data iide/ldproxy:latest
```

We additionally added `--name ldproxy` to change the name of the docker container from a random name to "ldproxy".

You may also change the host port or other parameters to your needs by adjusting the commands shown on this page.

To check that the docker process is running, use

```bash
docker ps
```

which should return something similar to

```bash
CONTAINER ID        IMAGE                 COMMAND                  CREATED             STATUS              PORTS                    NAMES
62db022d9bee        iide/ldproxy:latest   "/ldproxy/bin/ldproxy"   16 minutes ago      Up 16 minutes       0.0.0.0:7080->7080/tcp   ldproxy
```

Check that ldproxy is running by opening the URI http://localhost:7080/ in a web browser. Since the ldproxy Manager will only be available in a future version, you should receive a `404` error.

If ldproxy is not responding, consult the log with `docker logs ldproxy`.

## Updating ldproxy

To update ldproxy, just remove the container and create a new one with the run command as above. For example:

```bash
docker stop ldproxy
docker rm ldproxy
docker run --name ldproxy -d -p 7080:7080 -v ~/docker/ldproxy_data:/ldproxy/data iide/ldproxy:latest
```

Your data is saved in a volume, not in the container, so your configurations, API resources and caches will still be there after the update.
