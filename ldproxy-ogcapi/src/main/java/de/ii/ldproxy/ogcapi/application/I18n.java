package de.ii.ldproxy.ogcapi.application;

import com.google.common.collect.ImmutableSet;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public interface I18n {

    Set<Locale> LOCALES = ImmutableSet.of(Locale.ENGLISH, Locale.GERMAN);

    static Set<Locale> getLanguages() {
        return I18n.LOCALES;
    }

    String get(String key);

    String get(String key, Optional<Locale> language);
}
