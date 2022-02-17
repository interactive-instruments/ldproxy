/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.json.domain.JsonConfiguration;
import de.ii.ldproxy.ogcapi.xml.domain.XmlConfiguration;

import java.util.Map;

@AutoMultiBind
public interface GenericFormatExtension extends FormatExtension {

    Map<String, Class<? extends ExtensionConfiguration>> FORMAT_MAP =
            ImmutableMap.of("HTML", HtmlConfiguration.class,
                            "JSON", JsonConfiguration.class,
                            "XML", XmlConfiguration.class);

    /**
     * By default, encodings with the labels that are enabled in Common Core are enabled. These defaults
     * should be overloaded for specific resources like features or stylesheets.
     *
     * @return the configuration class of the format from Common Core
     */
    @Override
    default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FORMAT_MAP.get(this.getMediaType().label());
    }
}

