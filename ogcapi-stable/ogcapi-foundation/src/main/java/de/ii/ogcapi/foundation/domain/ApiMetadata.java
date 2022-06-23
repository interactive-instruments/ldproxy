/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * # `metadata`
 *
 * @langEn General metadata for the API (version, contact details, license information). Supported
 *     keys (with affected resources in braces): `version` (*API Definition*), `contactName` (*API
 *     Definition*, *HTML Landing Page*), `contactUrl` (*API Definition*, *HTML Landing Page*),
 *     `contactEmail` (*API Definition*, *HTML Landing Page*), `contactPhone` (*HTML Landing Page*),
 *     `licenseName` (*API Definition*, *HTML Landing Page*, *Feature Collections*, *Feature
 *     Collection*), `licenseUrl` (*API Definition*, *HTML Landing Page*, *Feature Collections*,
 *     *Feature Collection*) und `keywords` (*HTML Landing Page*). All values are strings, with the
 *     exception of keywords, which are an array of strings.
 * @langDe Über dieses Objekt können grundlegende Metadaten zur API (Version, Kontaktdaten,
 *     Lizenzinformationen) festgelegt werden. Erlaubt sind die folgenden Elemente (in Klammern
 *     werden die Ressourcen genannt, in denen die Angabe verwendet wird): `version`
 *     (API-Definition), `contactName` (API-Definition, HTML-Landing-Page), `contactUrl`
 *     (API-Definition, HTML-Landing-Page), `contactEmail` (API-Definition, HTML-Landing-Page),
 *     `contactPhone` (HTML-Landing-Page), `licenseName` (API-Definition, HTML-Landing-Page,
 *     Feature-Collections, Feature-Collection), `licenseUrl` (API-Definition, HTML-Landing-Page,
 *     Feature-Collections, Feature-Collection), `keywords` (Meta-Tags und schema:Dataset in
 *     HTML-Landing-Page), `attribution` (Landing-Page, Karten), `creatorName` (schema:Dataset in
 *     HTML), `creatorUrl` (schema:Dataset in HTML), `creatorLogoUrl` (schema:Dataset in HTML),
 *     `publisherName` (schema:Dataset in HTML), `publisherUrl` (schema:Dataset in HTML),
 *     `publisherLogoUrl` (schema:Dataset in HTML). Alle Angaben sind Strings, bis auf die Keywords,
 *     die als Array von Strings angegeben werden.
 * @default `{}`
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableApiMetadata.Builder.class)
public interface ApiMetadata {

  Optional<String> getContactName();

  Optional<String> getContactUrl();

  Optional<String> getContactEmail();

  Optional<String> getContactPhone();

  Optional<String> getCreatorName();

  Optional<String> getCreatorUrl();

  Optional<String> getCreatorLogoUrl();

  Optional<String> getPublisherName();

  Optional<String> getPublisherUrl();

  Optional<String> getPublisherLogoUrl();

  Optional<String> getLicenseName();

  Optional<String> getLicenseUrl();

  List<String> getKeywords();

  Optional<String> getVersion();

  Optional<String> getAttribution();
}
