# Exception handling

1. Every exception has a message that explains the cause and is suitable for the log file (in case of an internal error) or the response (in case of a user error).

1. Exceptions should be thrown as early as possible during the execution of an API request.

1. The web exceptions (`WebApplicationException` and subtypes) are only thrown by the following classes:

   * the main dispatcher (`OgcApiRequestDispatcher`)
   * endpoints (implementations of `OgcApiEndpointExtension`)
   * query handlers (implementations of `OgcApiQueriesHandler`)  
 
   All other classes throw other exceptions that are suitable for the cause, e.g. standard Java language exceptions.
   
1. All exceptions are caught in `OgcApiExceptionMapper` (currently still `Wfs3ExceptionMapper`, to be renamed) and, if necessary, mapped to a web exception and the selected media type. 

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

   For now, create a simple plain HTML page with all the information in the JSON response. (In the future, we could support exception format extension classes.)
   
   

