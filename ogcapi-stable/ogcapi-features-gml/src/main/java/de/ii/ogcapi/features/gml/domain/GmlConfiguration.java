/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @langEn By default, every GML property element will receive the property name from the feature
 *     schema. That is, the element will be in the default namespace. A different name can be set
 *     using the `rename` transformation, which can be used to change the name, but also supports to
 *     add a namespace prefix.
 * @langDe TODO
 * @default `{}`
 * @example <code>
 * ```yaml
 * - buildingBlock: GML
 *   enabled: true
 *   transformations:
 *     myProperty:
 *       rename: 'ns2:myProperty'
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableGmlConfiguration.Builder.class)
public interface GmlConfiguration extends ExtensionConfiguration, PropertyTransformations {

  enum Conformance {
    NONE,
    GMLSF0,
    GMLSF2
  }

  /**
   * @langEn The default `null` declares that the GML support does not meet all requirements of the
   *     *Geography Markup Language (GML), Simple Features Profile, Level 0* or the *Geography
   *     Markup Language (GML), Simple Features Profile, Level 2* conformance classes from [OGC API
   *     - Features - Part 1: Core
   *     1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_gmlsf0).
   *     <p>If the value is set to `0`, `1` or `2`, the conformance will be declared in the
   *     *Conformance Declaration* resource.
   *     <p>If for a collection from a SQL feature provider a root element different to
   *     `sf:FeatureCollection` is configured in `featureCollectionElementName`, the value will be
   *     ignored and no conformance to a GML conformance class will be declared.
   * @langDe TODO
   * @default `null`
   * @example <code>
   * ```yaml
   * - buildingBlock: GML
   *   enabled: true
   *   gmlSfLevel: 0
   * ```
   * </code>
   */
  @Nullable
  Integer getGmlSfLevel();

  @Value.Derived
  default Conformance getConformance() {
    switch (Objects.requireNonNullElse(getGmlSfLevel(), -1)) {
      case 0:
        return Conformance.GMLSF0;
      case 1:
      case 2:
        return Conformance.GMLSF2;
    }
    return Conformance.NONE;
  }

  /**
   * @langEn Every XML element will have and XML attribute can have an XML namespace. To improve
   *     readability of the XML documents, a namespace prefix is declared for every namespace.
   *     <p>Common namespaces and prefixes are pre-defined, these are: `gml` (GML 3.2), `xlink`
   *     (XLink), `xml` (XML), `sf` (OGC API Features Core 1.0, Core-SF), `wfs` (WFS 2.0), and `xsi`
   *     (XML Schema Information).
   *     <p>Additional namespaces that are used in the data (declared in GML application schemas and
   *     imported schemas), the namespaces are configured with their prefixes. Since feature data
   *     will always use elements in application-schema-specific namespaces, this confirguration
   *     parameter will always need to be specified.
   * @langDe TODO
   * @default `{}`
   * @example <code>
   * ```yaml
   * - buildingBlock: GML
   *   enabled: true
   *   applicationNamespaces:
   *     ns1: http://www.example.com/foo/0.1
   *     ns2: http://www.example.com/bar/0.1
   * ```
   * </code>
   */
  Map<String, String> getApplicationNamespaces();

  /**
   * @langEn A default namespace that is used for XML elements, if no other namespace is specified,
   *     can be specified with this configuration parameter. The value will be the namespace prefix.
   *     It must be either a pre-defined prefix or a prefix declared in `applicationNamespaces`.
   *     This namespace will be declared as the default namespace of the XML document.
   * @langDe TODO
   * @default `null`
   * @example <code>
   * ```yaml
   * - buildingBlock: GML
   *   enabled: true
   *   applicationNamespaces:
   *     ns1: http://www.example.com/foo/0.1
   *     ns2: http://www.example.com/bar/0.1
   *   defaultNamespace: ns1
   * ```
   * </code>
   */
  @Nullable
  String getDefaultNamespace();

  /**
   * @langEn If any application namespace should be included in the `xsi:schemaLocation` attribute
   *     of the root element, the document URIs have to be provided.
   *     <p>The pre-defined namespaces `sf` and `wfs` will only be added, if that namespace is used
   *     for the feature collection root element.
   *     <p>Note that to meet XML Schema validation requirements, the namespace of the root element
   *     must be declared in the `xsi:schemaLocation` attribute, even if the namespace is imported
   *     by another schema.
   * @langDe TODO
   * @default `null`
   * @example <code>
   * ```yaml
   * - buildingBlock: GML
   *   enabled: true
   *   applicationNamespaces:
   *     ns1: http://www.example.com/foo/0.1
   *     ns2: http://www.example.com/bar/0.1
   *   schemaLocations:
   *     ns1: http://www.example.com/foo/0.1/foo.xsd
   *     ns2: http://www.example.com/bar/0.1/bar.xsd
   * ```
   * </code>
   */
  Map<String, String> getSchemaLocations();

  /**
   * @langEn All object/data type instances are represented through a GML object element.
   *     <p>In the provider schema, a name must be provided for each OBJECT in the `objectType`
   *     property, including for the feature type itself. By default, this name will be used for the
   *     unqualified name of the GML object element.
   *     <p>If the GML object element is not in the default namespace, this configuration parameter
   *     assigns a namespace prefix to an object type.
   * @langDe TODO
   * @default `{}`
   * @example <code>
   * ```yaml
   * - buildingBlock: GML
   *   enabled: true
   *   objectTypeNamespaces:
   *     FeatureTypeA: ns2
   *     FeatureTypeB: ns2
   *     DataTypeA: ns2
   *     DataTypeB: ns2
   * ```
   * </code>
   */
  Map<String, String> getObjectTypeNamespaces();

  /**
   * @langEn There may also be cases, in particular when inheritance is used in the underlying
   *     application schema, where multiple object types are represented in the same table with an
   *     attribute that specifies the name of the feature/object type. This configuration parameter
   *     provides the capability to identify these properties and map the values to qualified names
   *     for the GML object element. In the example, `_type` is the feature property with three
   *     different values mapped to the qualified element name.
   * @langDe TODO
   * @default `{}`
   * @example <code>
   * ```yaml
   * - buildingBlock: GML
   *   enabled: true
   *   variableObjectElementNames:
   *     FeatureTypeA:
   *       property: _type
   *       mapping:
   *         typeA1: 'ns1:TypeA1'
   *         typeA2: 'ns2:TypeA2'
   *         typeA3: 'ns2:TypeA3'
   * ```
   * </code>
   */
  Map<String, VariableName> getVariableObjectElementNames();

  /**
   * @langEn Various feature collection elements are in use and sometimes additional ones are
   *     specified in GML application schemas. The default is `sf:FeatureCollection` as specified by
   *     OGC API Features. This configuration parameter provides a capability to use a different
   *     feature collection element.
   * @langDe TODO
   * @default `sf:FeatureCollection`
   * @example <code>
   * ```yaml
   * - buildingBlock: GML
   *   enabled: true
   *   featureCollectionElementName: 'plan:XPlanAuszug'
   * ```
   * </code>
   */
  @Nullable
  String getFeatureCollectionElementName();

  /**
   * @langEn The feature collection element referenced in `featureCollectionElementName` has a child
   *     property element that contains each feature. The default is `sf:featureMember` as specified
   *     by OGC API Features. This configuration parameter provides a capability to declare the
   *     element name for the feature collection element.
   * @langDe TODO
   * @default `sf:featureMember`
   * @example <code>
   * ```yaml
   * - buildingBlock: GML
   *   enabled: true
   *   featureMemberElementName: 'gml:featureMember'
   * ```
   * </code>
   */
  @Nullable
  String getFeatureMemberElementName();

  /**
   * @langEn The feature collection element referenced in `featureCollectionElementName` may support
   *     the WFS 2.0 standard response parameters (`timeStamp`, `numberMatched`, `numberReturned`).
   *     This configuration parameter controls whether the attributes are included in the feature
   *     collection element as XML attributes.
   * @langDe TODO
   * @default `false`
   * @example <code>
   * ```yaml
   * - buildingBlock: GML
   *   enabled: true
   *   supportsStandardResponseParameters: false
   * ```
   * </code>
   */
  @Nullable
  Boolean getSupportsStandardResponseParameters();

  /**
   * @langEn Properties are by default represented as the XML child element (GML property element)
   *     of the XML element representing the object (GML object element). Alternatively, the
   *     property can be represented as an XML attribute of the parent GML object element. This is
   *     only possible for properties of type STRING, FLOAT, INTEGER, or BOOLEAN).
   * @langDe TODO
   * @default `[]`
   * @example <code>
   * ```yaml
   * - buildingBlock: GML
   *   enabled: true
   *   xmlAttributes:
   *     - myAttProperty
   *     - someProperty.myOtherAttProperty
   * ```
   * </code>
   */
  List<String> getXmlAttributes();

  /**
   * @langEn The feature property with role `ID` in the provider schema is mapped to the `gml:id`
   *     attribute of the feature. These properties must be a direct property of the feature type.
   *     <p>If the values violate the rule for XML IDs, e.g., if they can start with a digit, this
   *     configuration parameter can be used to add a consistent prefix to map all values to valid
   *     XML IDs.
   * @langDe TODO
   * @default `null`
   * @example <code>
   * ```yaml
   * - buildingBlock: GML
   *   enabled: true
   *   gmlIdPrefix: '_'
   * ```
   * </code>
   */
  @Nullable
  String getGmlIdPrefix();

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableGmlConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    return new ImmutableGmlConfiguration.Builder()
        .from(source)
        .from(this)
        .transformations(
            PropertyTransformations.super
                .mergeInto((PropertyTransformations) source)
                .getTransformations())
        .build();
  }
}
