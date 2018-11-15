# Logging

#### View log messages
To view the log messages, you should use dockers `logs` command.

 - list all log messages: `docker logs container_name`
 - list last 50 messages and follow log output: `docker logs --tail 50 -f container_name`

#### Log levels
Log levels can be configured in the file `xtraplatform.json` in the mounted data directory. If for example you encounter an issue and need more context, set the `de.ii` logger to `DEBUG`:

 ```
"loggers" : {
    "de.ii": "DEBUG"
}

 ```

 If you want all messages from third party libraries as well, set the main level accordingly:

 ```
"logging": {
    "level": "DEBUG",
    "appenders": [
 ```