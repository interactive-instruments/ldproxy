# Exception handling

1. Every exception has a message that explains the cause and is suitable for the log file (in case of an internal error) or the response (in case of a user error).

1. Exceptions should be thrown as early as possible during the execution of an API request.

1. The web exceptions (`WebApplicationException` and subtypes) are only thrown by the following classes:

   * the main dispatcher (`OgcApiRequestDispatcher`)
   * endpoints (implementations of `OgcApiEndpointExtension`)
   * query handlers (implementations of `OgcApiQueriesHandler`)  
 
   All other classes throw other exceptions that are suitable for the cause, e.g. standard Java language exceptions.

1. Log messages for exceptions should in general be written in the web-layer classes, too. For exceptions that are internal errors, the messages should have "error" level and the stacktrace should be written with "debug" level. For example:

   ```
   LOGGER.error("Feature provider with id '{}' could not be started: {}", getId(), e.getMessage());
   if (LOGGER.isDebugEnabled()) {
       LOGGER.debug("Stacktrace:", e);
   }   
   ```

1. All exceptions are caught in `OgcApiExceptionMapper` and, if necessary, mapped to a web exception and the selected media type.

1. For 4xx responses, the exception message documents the cause of the exception.

1. For 5xx responses, the exception message does not document the cause of the exception and only provides enough information to find mot information in the log file. The cause of the exception and the stackdump are written to the logfile.

1. The default media type of an exception response is `application/problem+json` as defined in [RFC 7807](https://tools.ietf.org/html/rfc7807).

   The default is to

   * leave "type" empty,
   * use the title of the status code in the HTTP standard as the "title",
   * the status code as "status",
   * the exception message in "detail", and
   * the request URI in "instance".

   In the future we may identify sub-types and use URIs to link to the page in the documentation for each exception type.

1. If the requested media type of the response is "text/html", return a HTML page.

   For now, create a simple plain HTML page with all the information in the JSON response.

## Exception mapping

Java exceptions that are not web exceptions are mapped as follows:

  * `IllegalArgumentException` is mapped to the HTTP response status code 400 (Bad Request). These exceptions are used for unknown or unsupported parameters, incorrect query arguments, etc.
  * `OgcApiFormatNotSupportedException` is mapped to HTTP response status code 406 (Not Acceptable).
  * All other exceptions are mapped to HTTP response status code 500 (Internal Server Error). For exceptions created in the ldproxy layer typically `RuntimeException` is used unless a more specific exception applies.
