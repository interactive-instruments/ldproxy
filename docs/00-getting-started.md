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

You may also change the host port or other parameters to your needs. 

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

## Changing the configuration

...
