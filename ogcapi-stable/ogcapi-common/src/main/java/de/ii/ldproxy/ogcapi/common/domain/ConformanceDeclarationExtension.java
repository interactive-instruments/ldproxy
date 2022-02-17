/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ContentExtension;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@AutoMultiBind
public interface ConformanceDeclarationExtension extends ContentExtension {

    ImmutableConformanceDeclaration.Builder process(ImmutableConformanceDeclaration.Builder conformanceDeclarationBuilder,
                                                    OgcApiDataV2 apiData,
                                                    URICustomizer uriCustomizer,
                                                    ApiMediaType mediaType,
                                                    List<ApiMediaType> alternateMediaTypes,
                                                    Optional<Locale> language);

    default String getResourceName() { return "Conformance Declaration"; };
}
