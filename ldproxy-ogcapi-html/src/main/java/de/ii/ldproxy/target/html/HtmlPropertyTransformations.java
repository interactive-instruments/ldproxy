/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.xtraplatform.feature.provider.api.FeatureProperty;
import de.ii.xtraplatform.feature.transformer.api.FeaturePropertySchemaTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeaturePropertyTransformations;
import de.ii.xtraplatform.feature.transformer.api.FeaturePropertyValueTransformer;
import org.immutables.value.Value;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
public abstract class HtmlPropertyTransformations implements FeaturePropertyTransformations<ValueDTO> {

    public abstract Optional<I18n> getI18n();

    public abstract Optional<Locale> getLanguage();

    //TODO: make explicit as booleanNormalize + i18n
    @Value.Derived
    public FeaturePropertyTransformerBooleanTranslate getBooleanTransformer() {
        return ImmutableFeaturePropertyTransformerBooleanTranslate.builder()
                                                           .i18n(getI18n())
                                                           .language(getLanguage())
                                                           .build();
    }

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
            }
            if (schema.getType() == FeatureProperty.Type.BOOLEAN) {
                transformedValue = getBooleanTransformer().transform(transformedValue);
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

        wrapper.property.name = transformedSchema.getName();
        wrapper.value = transformedValue;

        if (Objects.isNull(wrapper.value)) {
            return Optional.of(wrapper);
        }

        if (isHtml(wrapper.value)) {
            wrapper.isHtml = true;
        } else if (isImageUrl(wrapper.value)) {
            wrapper.isImg = true;
        } else if (isUrl(wrapper.value)) {
            wrapper.isUrl = true;
        }

        return Optional.of(wrapper);
    }

    private boolean isHtml(String value) {
        return value.startsWith("<") && (value.endsWith(">") || value.endsWith(">\n")) && value.contains("</");
    }

    private boolean isUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private boolean isImageUrl(String value) {
        return isUrl(value) && (value.toLowerCase()
                                     .endsWith(".png") || value.toLowerCase()
                                                               .endsWith(".jpg") || value.toLowerCase()
                                                                                         .endsWith(".jpeg") || value.toLowerCase()
                                                                                                                    .endsWith(".gif"));
    }
}
