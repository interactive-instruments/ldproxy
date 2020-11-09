/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertySchemaTransformer;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformations;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import org.immutables.value.Value;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
public abstract class HtmlPropertyTransformations implements FeaturePropertyTransformations<ValueDTO> {

    public abstract Optional<I18n> getI18n();

    public abstract Optional<Locale> getLanguage();

    @Override
    public String getValue(ValueDTO wrapper) {
        return wrapper.value;
    }

    public Optional<FeatureProperty> transform(FeatureProperty schema) {
        FeatureProperty transformedSchema = schema;

        for (FeaturePropertySchemaTransformer schemaTransformer : getSchemaTransformers()) {
            transformedSchema = schemaTransformer.transform(transformedSchema);
        }

        return Optional.ofNullable(transformedSchema);
    }

    public String transform(FeatureProperty schema, String value) {
        String transformedValue = value;

        if (Objects.nonNull(value)) {
            for (FeaturePropertyValueTransformer valueTransformer : getValueTransformers()) {
                transformedValue = valueTransformer.transform(transformedValue);
                if (Objects.isNull(transformedValue))
                    break;
            }
            if (schema.getType() == FeatureProperty.Type.BOOLEAN && Objects.nonNull(transformedValue)) {
                //TODO: make explicit as booleanNormalize + i18n
                transformedValue = new ImmutableFeaturePropertyTransformerBooleanTranslate.Builder()
                        .i18n(getI18n())
                        .language(getLanguage())
                        .build()
                        .transform(transformedValue);
            }
        }

        return transformedValue;
    }

    @Override
    public Optional<ValueDTO> transform(ValueDTO wrapper, FeatureProperty transformedSchema,
                                                  String transformedValue) {
        if (Objects.isNull(transformedSchema)) {
            return Optional.empty();
        }

        if (Objects.nonNull(wrapper.property))
           wrapper.property.name = transformedSchema.getName();
        wrapper.value = transformedValue;

        if (Objects.isNull(wrapper.value)) {
            return Optional.of(wrapper);
        }

        return Optional.of(wrapper);
    }
}
