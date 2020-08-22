/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.app;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.I18n;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.*;

@Component
@Provides
@Instantiate
public class I18nDefault implements I18n {

    private static final Map<Locale, ResourceBundle> LOCALE_RESOURCE_BUNDLE_MAP = ImmutableMap.of(
            Locale.ENGLISH, ResourceBundle.getBundle("i18n-en"),
            Locale.GERMAN, ResourceBundle.getBundle("i18n-de"));

    @Override
    public String get(String key) {
        return get(key, Optional.of(Locale.ENGLISH));
    }

    @Override
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

    ;
}
