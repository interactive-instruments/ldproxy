/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.collect.ImmutableMap;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

@Component
@Provides(specifications = {I18n.class})
@Instantiate
//TODO: whiteboard for overwrites
public class I18n {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("i18n");
    private static final Map<Locale, ResourceBundle> LOCALE_RESOURCE_BUNDLE_MAP = ImmutableMap.of();

    public String get(String key) {
        return RESOURCE_BUNDLE.getString(key);
    }

    public String get(String key, Locale locale) {
        //TODO: cache bundles per locale
        return LOCALE_RESOURCE_BUNDLE_MAP.get(locale).getString(key);
    }
}
