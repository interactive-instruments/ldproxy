/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.gml.domain.GmlConfiguration;
import de.ii.ogcapi.features.gml.domain.ImmutableGmlConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Features GML
 * @langEn The module *Features GML* may be enabled for every API with a SQL or WFS feature
 *     provider. It provides the resources *Features* and *Feature* encoded as GML.
 *     <p>For a WFS feature provider, the features are accessed as GML from the WFS and rewritten to
 *     the response. In case of *Features* the root element is `sf:FeatureCollection`.
 *     <p>For a SQL feature provider, the features are mapped to GML object and property elements
 *     based on the provider schema. A number of configuration options exist to control how the
 *     features are mapped to XML.
 * @conformanceEn In general, *Features GML* implements all requirements of conformance class
 *     *Geography Markup Language (GML), Simple Features Profile, Level 0* and *Geography Markup
 *     Language (GML), Simple Features Profile, Level 2* from [OGC API - Features - Part 1: Core
 *     1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_gmlsf0). However, conformance
 *     depends on the conformance of the GML application schema with the GML Simple Features
 *     standard. Since the GML application schema is not controlled by ldproxy, the conformance
 *     level needs to be declared as part of the configuration.
 *     <p>For SQL feature providers a different root element than `sf:FeatureCollection` can be
 *     configured for the *Features* resource. In that case, the API cannot conform to any of the
 *     GML conformance classes of OGC API Features.
 * @langDe Das Modul *Features GML* kann für jede über ldproxy bereitgestellte API mit einem SQL-
 *     oder WFS-Feature-Provider aktiviert werden. Es aktiviert die Bereitstellung der Ressourcen
 *     Features und Feature in GML.
 *     <p>Bei einem WFS-Feature-Provider werden die Features als GML vom WFS abgerufen und in die
 *     Antwort umgeschrieben. Im Falle von *Features* ist das Wurzelelement `sf:FeatureCollection`.
 *     <p>Bei einem SQL-Feature-Provider werden die Features auf der Grundlage des Provider-Schemas
 *     auf GML-Objekt- und Eigenschaftselemente abgebildet. Es gibt eine Reihe von
 *     Konfigurationsoptionen, um zu steuern, wie die Merkmale auf XML abgebildet werden.
 * @conformanceDe Im Allgemeinen implementiert *Features GML* alle Anforderungen der
 *     Konformitätsklassen *Geography Markup Language (GML), Simple Features Profile, Level 0* und
 *     *Geography Markup Language (GML), Simple Features Profile, Level 2* aus [OGC API - Features -
 *     Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_gmlsf0). Die
 *     Konformität hängt jedoch von der Konformität des GML-Anwendungsschemas mit dem GML Simple
 *     Features Standard ab. Da das GML-Anwendungsschema nicht von ldproxy kontrolliert wird, muss
 *     die Einstufung der Konformität als Teil der Konfiguration deklariert werden.
 *     <p>Für SQL-Feature-Provider kann außerdem ein anderes Root-Element als `sf:FeatureCollection`
 *     für die *Features*-Ressource konfiguriert werden. In diesem Fall kann die API nicht konform
 *     zu einer der GML-Konformitätsklassen von OGC API Features sein.
 * @propertyTable {@link de.ii.ogcapi.features.gml.domain.GmlConfiguration}
 */
@Singleton
@AutoBind
public class GmlBuildingBlock implements ApiBuildingBlock {

  @Inject
  public GmlBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableGmlConfiguration.Builder()
        .enabled(false)
        .conformance(GmlConfiguration.Conformance.NONE)
        .featureCollectionElementName("sf:FeatureCollection")
        .featureMemberElementName("sf:featureMember")
        .supportsStandardResponseParameters(false)
        .build();
  }
}
