/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.templates;

import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import org.immutables.value.Value;

@Value.Immutable
public interface FeaturePropertyTransformerStringFormat extends FeaturePropertyValueTransformer {

    String TYPE = "STRING_FORMAT";

    @Override
    default String getType() {
        return TYPE;
    }

    String getServiceUrl();

    //TODO: double cols
    @Override
    default String transform(String input) {
        //boolean more = false;
        //if (currentFormatter == null) {

        String formattedValue = StringTemplateFilters.applyTemplate(getParameter(), input);

        formattedValue = formattedValue.replace("{{serviceUrl}}", getServiceUrl());

        int subst = formattedValue.indexOf("}}");
        if (subst > -1) {
            formattedValue = formattedValue.substring(0, formattedValue.indexOf("{{")) + input + formattedValue.substring(subst + 2);
            //more = formattedValue.contains("}}");
        }
        /*} else {
            int subst = currentFormatter.indexOf("}}");
            if (subst > -1) {
                property.value = currentFormatter.substring(0, currentFormatter.indexOf("{{")) + value + currentFormatter.substring(subst + 2);
                more = property.value.contains("}}");
            }
        }
        if (more) {
            this.currentFormatter = property.value;
            return;
        } else {
            currentFormatter = null;
        }*/

        return formattedValue;
    }
}
