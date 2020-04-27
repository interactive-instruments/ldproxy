/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.tiles.TileMatrixSetData;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class OgcApiTileMatrixSetView extends OgcApiView {
    public TileMatrixSetData tileMatrixSet;
    public String idTitle;
    public String boundingBoxTitle;
    public String wellKnownScaleSetTitle;
    public String supportedCrsTitle;
    public String scaleDenominatorTitle;
    public String topLeftCornerTitle;
    public String tileWidthTitle;
    public String tileHeightTitle;
    public String matrixWidthTitle;
    public String matrixHeightTitle;

    public OgcApiTileMatrixSetView(OgcApiApiDataV2 apiData,
                                   TileMatrixSetData tileMatrixSet,
                                   List<NavigationDTO> breadCrumbs,
                                   String staticUrlPrefix,
                                   HtmlConfig htmlConfig,
                                   boolean noIndex,
                                   URICustomizer uriCustomizer,
                                   I18n i18n,
                                   Optional<Locale> language) {
        super("tileMatrixSet.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, staticUrlPrefix,
                tileMatrixSet.getLinks(),
                null,
                null);
        this.tileMatrixSet = tileMatrixSet;

        this.idTitle = i18n.get("idTitle", language);
        this.boundingBoxTitle = i18n.get("boundingBoxTitle", language);
        this.wellKnownScaleSetTitle = i18n.get("wellKnownScaleSetTitle", language);
        this.supportedCrsTitle = i18n.get("supportedCrsTitle", language);
        this.scaleDenominatorTitle = i18n.get("scaleDenominatorTitle", language);
        this.topLeftCornerTitle = i18n.get("topLeftCornerTitle", language);
        this.tileWidthTitle = i18n.get("tileWidthTitle", language);
        this.tileHeightTitle = i18n.get("tileHeightTitle", language);
        this.matrixWidthTitle = i18n.get("matrixWidthTitle", language);
        this.matrixHeightTitle = i18n.get("matrixHeightTitle", language);
    }
}
