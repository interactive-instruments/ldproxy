# Security

ldproxy supports token based security for [secured services](). It expects tokens to be passed in the request headers as `Authorization: Bearer <JWT>`.

#### JSON Web Token (jwtSigningKey)
ldproxy supports supports verification of signed JWTs. If you configure a signing key, ldproxy will use it to verify given JWTs. Additionally, if the JWT contains an expiration time in the `exp` claim, requests with expired tokens will be rejected.

#### Other tokens (userInfoEndpoint)
Tokens may also be verified by sending them to a given endpoint. The token is sent as ... and ... is expected as response.

#### External dynamic authorization (externalDynamicAuthorizationEndpoint)
For [secured services](), ldproxy supports dynamic authorization using an external endpoint using JSON based XACML. For a request with a valid token, an request with Content-Type `application/xacml+json` will be sent to the configured endpoint:

```
{
  "Request": {
    "AccessSubject": {
      "Attribute": [
        { "AttributeId": "urn:oasis:names:tc:xacml:1.0:subject:subject-id", "Value": "johndoe" } 
      ]
    },
    "Resource": {
      "Attribute": [
        { "AttributeId": "urn:oasis:names:tc:xacml:1.0:resource:resource-id", "Value": "/collections/afeaturetype/items" } 
      ]
    },
    "Action": {
      "Attribute": [
        { "AttributeId": "urn:oasis:names:tc:xacml:1.0:action:action-id", "Value": "POST" },
        { "AttributeId": "payload", "Value": "e30=" } 
      ]
    }
  }
}
```

An responsewith Content-Type `application/xacml+json` that permits or denies authorization is expected:

```
{
  "Response": [ {
      "Decision": "Deny|Permit"
    }
  ]
}
``` 


#### Postprocessing for POST/PUT (postProcessingEndpoint)
For services with POST/PUT/DELETE support, request bodies for POST/PUT requests may be passed through a postprocessing endpoint. The body is sent as ... and ... is expected as response.