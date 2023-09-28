/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * @langEn External document with additional information about this API, the `url` key is required,
 *     the `description` key is recommended.
 * @langDe Es kann ein externes Dokument mit weiteren Informationen angegeben werden, auf das aus
 *     der API verlinkt wird. Anzugeben ist die `url` des Dokuments, die Angabe von `description`
 *     wird empfohlen.
 * @default {}
 */
@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = ImmutableExternalDocumentation.Builder.class)
public abstract class ExternalDocumentation {

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<ExternalDocumentation> FUNNEL =
      (from, into) -> {
        assert from != null;
        from.getDescription().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        into.putString(from.getUrl(), StandardCharsets.UTF_8);
      };

  public static ExternalDocumentation of(String url) {
    return new ImmutableExternalDocumentation.Builder().url(url).build();
  }

  public static ExternalDocumentation of(String url, String description) {
    return new ImmutableExternalDocumentation.Builder().url(url).description(description).build();
  }

  /**
   * @langEn Description of the content of the document or website.
   * @langDe Beschreibung des Inhalts des Dokuments oder der Website.
   * @since v2.1
   */
  public abstract Optional<String> getDescription();

  /**
   * @langEn URL of the document or website.
   * @langDe URL des Dokuments oder der Website.
   * @since v2.1
   */
  public abstract String getUrl();
}
