/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @langEn An example of flattening. The non-flattened feature
 * @langDe Ein Beispiel zur Abflachung. Das nicht abgeflachte Feature
 * @example <code>
 * ```json
 * {
 *   "type" : "Feature",
 *   "id" : "1",
 *   "geometry" : {
 *     "type" : "Point",
 *     "coordinates" : [ 7.0, 50.0 ]
 *   },
 *   "properties" : {
 *     "name" : "Beispiel",
 *     "inspireId" : "https://example.org/id/soziales/kindergarten/1",
 *     "serviceType" : {
 *       "title" : "Kinderbetreuung",
 *       "href" : "http://inspire.ec.europa.eu/codelist/ServiceTypeValue/childCareService"
 *     },
 *     "pointOfContact" : {
 *       "address" : {
 *         "thoroughfare" : "Beispielstr.",
 *         "locatorDesignator" : "123",
 *         "postCode" : "99999",
 *         "adminUnit" : "Irgendwo"
 *       },
 *       "telephoneVoice" : "0211 16021740"
 *     },
 *     "occupancy" : [ {
 *       "typeOfOccupant" : "vorschule",
 *       "numberOfOccupants" : 20
 *     }, {
 *       "typeOfOccupant" : "schulkinder",
 *       "numberOfOccupants" : 25
 *     } ]
 *   }
 * }
 * ```
 * </code>
 */

/**
 * @langEn looks like this flattened with the default separator:
 * @langDe sieht abgeflacht mit dem Standardtrennzeichen wie folgt aus:
 * @example <code>
 * ```json
 * {
 *   "type" : "Feature",
 *   "id" : "1",
 *   "geometry" : {
 *     "type" : "Point",
 *     "coordinates" : [ 7.0, 50.0 ]
 *   },
 *   "properties" : {
 *     "name" : "Beispiel",
 *     "inspireId" : "https://example.org/id/soziales/kindergarten/1",
 *     "serviceType.title" : "Kinderbetreuung",
 *     "serviceType.href" : "http://inspire.ec.europa.eu/codelist/ServiceTypeValue/childCareService",
 *     "pointOfContact.address.thoroughfare" : "Otto-Pankok-Str.",
 *     "pointOfContact.address.locatorDesignator" : "29",
 *     "pointOfContact.address.postCode" : "40231",
 *     "pointOfContact.address.adminUnit" : "Düsseldorf",
 *     "pointOfContact.telephoneVoice" : "0211 16021740",
 *     "occupancy.1.typeOfOccupant" : "vorschule",
 *     "occupancy.1.numberOfOccupants" : 20,
 *     "occupancy.2.typeOfOccupant" : "schulkinder",
 *     "occupancy.2.numberOfOccupants" : 25
 *   }
 * }
 * ```
 * </code>
 */

/**
 * @langEn  Example of the specifications in the configuration file:
 * @langDe Beispiel für die Angaben in der Konfigurationsdatei:
 * @example <code>
 * ```yaml
 * - buildingBlock: GEO_JSON
 *   transformations:
 *     '*':
 *       flatten: '.'
 * ```
 * </code>
 */

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableGeoJsonConfiguration.Builder.class)
public interface GeoJsonConfiguration extends ExtensionConfiguration, PropertyTransformations {

  enum NESTED_OBJECTS {NEST, FLATTEN}

  enum MULTIPLICITY {ARRAY, SUFFIX}

  abstract class Builder extends ExtensionConfiguration.Builder {
    }

    /**
     * @langEn *Deprecated* Use the
     * [`flatten` transformation](../../providers/details/transformations.md) instead.
     * @langDe *Deprecated* Wird abgelöst durch die
     * [`flatten`-Transformation](../../providers/details/transformations.md).
     * @default `FLATTEN
     */
    @Deprecated(since = "3.1.0")
    @Nullable
    NESTED_OBJECTS getNestedObjectStrategy();

    /**
     * @langEn *Deprecated* Use the
     * [`flatten` transformation](../../providers/details/transformations.md) instead.
     * @langDe *Deprecated* Wird abgelöst durch die
     * [`flatten`-Transformation](../../providers/details/transformations.md).
     * @default `SUFFIX`
     */
    @Deprecated(since = "3.1.0")
    @Nullable
    MULTIPLICITY getMultiplicityStrategy();

    @Deprecated(since = "3.1.0")
    @Nullable
    Boolean getUseFormattedJsonOutput();

    /**
     * @langEn *Deprecated* Use the
     * [`flatten` transformation](../../providers/details/transformations.md) instead.
     * @langDe *Deprecated* Wird abgelöst durch die
     * [`flatten`-Transformation](../../providers/details/transformations.md).
     * @default "."
     */
    @Deprecated(since = "3.1.0")
    @Nullable
    String getSeparator();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isFlattened() {
        return hasTransformation(PropertyTransformations.WILDCARD, transformation -> transformation.getFlatten().isPresent());
    }

    @Value.Check
    default GeoJsonConfiguration backwardsCompatibility() {
        if (getNestedObjectStrategy() == NESTED_OBJECTS.FLATTEN
            && getMultiplicityStrategy() == MULTIPLICITY.SUFFIX
            && !isFlattened()) {

            Map<String, List<PropertyTransformation>> transformations = withTransformation(PropertyTransformations.WILDCARD,
                new ImmutablePropertyTransformation.Builder()
                .flatten(Optional.ofNullable(getSeparator()).orElse("."))
                .build());

            return new ImmutableGeoJsonConfiguration.Builder()
                .from(this)
                .transformations(transformations)
                .build();
        }

        return this;
    }

    @Override
    default Builder getBuilder() {
        return new ImmutableGeoJsonConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        return new ImmutableGeoJsonConfiguration.Builder()
            .from(source)
            .from(this)
            .transformations(PropertyTransformations.super.mergeInto((PropertyTransformations) source).getTransformations())
            .build();
    }
}
