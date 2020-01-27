/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.xtraplatform.feature.provider.api.FeatureProperty;
import de.ii.xtraplatform.feature.transformer.api.FeaturePropertyValueTransformer;
import org.immutables.value.Value;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Value.Immutable
public interface FeaturePropertyTransformerBooleanTranslate extends FeaturePropertyValueTransformer {

    String TYPE = "BOOLEAN_TRANSLATE";

    @Override
    default String getType() {
        return TYPE;
    }

    @Override
    default String getParameter() { return null; }

    @Override
    default List<FeatureProperty.Type> getSupportedPropertyTypes() {
        return ImmutableList.of(FeatureProperty.Type.BOOLEAN);
    }

    Optional<I18n> getI18n();

    Optional<Locale> getLanguage();

    @Override
    default String transform(String input) {

        if (getI18n().isPresent()) {
            if (input.matches("[fF](alse|ALSE)?|0")) {
                return getI18n().get().get("false", getLanguage());
            } else if (input.matches("[tT](rue|RUE)?|[\\-\\+]?1")) {
                return getI18n().get().get("true", getLanguage());
            }
        }

        return input;
    }
}
