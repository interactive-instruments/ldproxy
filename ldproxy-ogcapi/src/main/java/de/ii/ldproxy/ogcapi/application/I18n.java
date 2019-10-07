/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.application;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.*;

@Component
@Provides(specifications = {I18n.class})
@Instantiate
public class I18n {

    private static final Set<Locale> LOCALES = ImmutableSet.of( Locale.ENGLISH, Locale.GERMAN);
    private static final Map<Locale, ResourceBundle> LOCALE_RESOURCE_BUNDLE_MAP = ImmutableMap.of(
            Locale.ENGLISH, ResourceBundle.getBundle("i18n-en"),
            Locale.GERMAN, ResourceBundle.getBundle("i18n-de"));

    public String get(String key) {
        return get(key, Optional.of(Locale.ENGLISH));
    }

    public String get(String key, Optional<Locale> language) {
        try {
            if (language.isPresent())
                return LOCALE_RESOURCE_BUNDLE_MAP.get(language.get()).getString(key);

            return LOCALE_RESOURCE_BUNDLE_MAP.get(Locale.ENGLISH).getString(key);
        } catch (MissingResourceException ex) {
            // just return the key
            return key;
        }
    }

    public static Set<Locale> getLanguages() {
        return LOCALES;
    };
}
