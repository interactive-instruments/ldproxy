# General rules for all modules

## Response encoding

For operations that return a response, the encoding is chosen using standard HTTP content negotiation with `Accept` headers.

GET operations additionally support the query parameter `f`, which allows to explicitely choose the encoding and override the result of the content negotiation. The supported encodings depend on the affected resource and the configuration.

## Response language

For operations that return a response, the language for linguistic texts is chosen using standard HTTP content negiotiation with `Accept-Language` headers.

If enabled in [Common Core](common.md), GET operations additionally support the quer parameter `lang`, which allows to explicitely choose the language and override the result of the content negotiation. The supported languages depend on the affected resource and the configuration. Support for multilingualism is currently limited. There are four possible sources for linguistic texts:

* Static texts: For example link labels or static texts in HTML represenations. Currently the languages English (`en`) and German (`de`) are supported.
* Texts contained in the data: Currently not supported.
* Texts set in the configuration: Currently not supported.
* Error messages: These are always in english, the messages are currently hard-coded.

## Option `enabled`

Every module can be enabled or disabled in the configuration using `enabled`. The default value differs between modules, see the [overview](#api-module-overview)).

## Resource paths

All resource paths in this documentation are relative to the base URI of the deployment. For example given the base URI `https://example.com/pfad/zu/apis` and the resource path `/{apiId}/collections`, the full path would be `https://example.com/pfad/zu/apis/{apiId}/collections`.

<a name="transformations"></a>

## Property transformations

Modules related to feature encoding ([Core](features-core.md), [GeoJSON](geojson.md), [HTML](features-html.md), [Vector Tiles](tiles.md)) support transforming feature properties for all or only for specific encodings.

Transformations do not affect data sources, they are applied on-the-fly as part of the encoding.

Filter expressions do not take transformations into account, they have to be based on the source values. That means queryable properties (see `queryables` in [Features Core](features-core.md)) should not use transformations in most cases. The exception to the rule is the HTML encoding, where readability might be more important than filter support.


|Transformation |Data Type |Description
| --- | --- | ---
|`rename` |string |Rename a property.
|`remove` |enum |`OVERVIEW` skips the property only for the *Features* resource, `ALWAYS` always skips it, `NEVER` never skips it.
|`null` |regex |Maps all values matching the regular expression to `null`. Not applicable for properties containing objects.
|`stringFormat` |string |Format a value, where `{{value}}` is replaced with the actual value and `{{serviceUrl}}` is replaced with the API landing page URI. Additonal operations can be applied to `{{value}}` by chaining them with `|`, see the examples below.
|`dateFormat` |string |Format date(-time) values with the given [pattern](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html#patterns), e.g. `dd.MM.yyyy` for German notation. 
|`codelist`|string |Maps the value according to the given [codelist](../../codelists/README.md). If the value is not found in the codelist or the codelist does not exist, the original value is passed through.  Falls der Wert nicht in der Codelist enthalten ist oder die Codelist nicht gefunden wird, bleibt der Wert unverändert. Not applicable for properties containing objects.

### Examples for `stringFormat`

* `https://example.com/id/kinder/kita/{{value}}` inserts the value into the URI template.
* `{{value | replace:'\\s*[0-9].*$':''}}` removes all white space and numbers at the end (e.g. to remove a street number)
* `{{value | replace:'^[^0-9]*':''}}` removes everything before the first digit
* `{{value | toUpper}}` transforms the value to upper case
* `{{value | toLower}}` transforms the value to lower case
* `{{value | urlEncode}}` encodes special characters in the text for usage as aprt of an URI
* `[{{value}}](https://de.wikipedia.org/wiki/{{value | replace:' ':'_' | urlencode}})` transforms a value into a markdown link to a Wikipedia entry
