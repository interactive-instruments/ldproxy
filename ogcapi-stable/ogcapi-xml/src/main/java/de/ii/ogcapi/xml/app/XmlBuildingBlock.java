/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.xml.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.xml.domain.ImmutableXmlConfiguration;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */

/**
 * @title XML
 * @langEn The module *XML* may be enabled for every API with a feature provider.
 * It is disabled by default. It provides the resources *Landing Page*, *Conformance Declaration*,
 * *Feature Collections* and *Feature Collection* as XML.
 *
 * This module has no configuration options.
 * @langDe Das Modul "XML" kann für jede über ldproxy bereitgestellte API aktiviert werden. Es ist
 * standardmäßig deaktiviert. Soweit für eine Ressource keine speziellen Regelungen für die
 * Ausgabeformate bestehen (wie zum Beispiel für [Features](gml.md)) und die Ressource XML
 * unterstützt, können Clients das Ausgabeformat anfordern. Allerdings unterstützen nur die
 * folgenden Ressourcen XML: Landing Page, Conformance Declaration, Feature Collections und
 * Feature Collection.
 *
 * In der Konfiguration können keine Optionen gewählt werden.
 * @propertyTable {@link de.ii.ogcapi.xml.domain.XmlConfiguration}
 */
@Singleton
@AutoBind
public class XmlBuildingBlock implements ApiBuildingBlock {

    @Inject
    public XmlBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableXmlConfiguration.Builder().enabled(false)
                                                      .build();
    }

}
