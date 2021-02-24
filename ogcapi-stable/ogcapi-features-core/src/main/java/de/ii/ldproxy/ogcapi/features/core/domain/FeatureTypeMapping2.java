/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableFeatureTypeMapping2.Builder.class)
public interface FeatureTypeMapping2 {

    List<String> REMOVE_VALUES = ImmutableList.of("ALWAYS", "OVERVIEW", "NEVER");

    Optional<String> getRename();

    Optional<String> getRemove();

    Optional<String> getStringFormat();

    Optional<String> getDateFormat();

    Optional<String> getCodelist();

    Optional<String> getNull();

    default ImmutableValidationResult.Builder validate(ImmutableValidationResult.Builder builder, String collectionId, String property, Collection<String> codelists) {
        final Optional<String> remove = getRemove();
        if (remove.isPresent()) {
            if (!REMOVE_VALUES.contains(remove.get())) {
                builder.addStrictErrors(MessageFormat.format("The remove transformation in collection ''{0}'' for property ''{1}'' is invalid. The value ''{2}'' is not one of the known values: {3}.", collectionId, property, remove.get(), REMOVE_VALUES));
            }
        }
        final Optional<String> stringFormat = getStringFormat();
        if (stringFormat.isPresent()) {
            Pattern valuePattern = Pattern.compile("\\{\\{(?:value)( ?\\| ?[\\w]+(:'[^']*')*)*\\}\\}");
            Matcher matcher = valuePattern.matcher(stringFormat.get());
            if (!matcher.find()) {
                builder.addWarnings(MessageFormat.format("The stringFormat transformation in collection ''{0}'' for property ''{1}'' with  value ''{2}'' does not include a string template for ''value''.", collectionId, property, stringFormat.get()));
            }
        }
        final Optional<String> dateFormat = getDateFormat();
        if (dateFormat.isPresent()) {
            try {
                LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat.get()));
            } catch (Exception e) {
                builder.addWarnings(MessageFormat.format("The dateFormat transformation in collection ''{0}'' for property ''{1}'' with  value ''{2}'' is invalid, if used with a timestamp: {3}.", collectionId, property, dateFormat.get(), e.getMessage()));
            }
        }
        final Optional<String> codelist = getCodelist();
        if (codelist.isPresent()) {
            if (!codelists.contains(codelist.get())) {
                builder.addStrictErrors(MessageFormat.format("The codelist transformation in collection ''{0}'' for property ''{1}'' is invalid. The codelist ''{2}'' is not one of the known values: {3}.", collectionId, property, codelist.get(), codelists));
            }
        }
        final Optional<String> null_ = getNull();
        if (null_.isPresent()) {
            try {
                Pattern.compile(null_.get());
            } catch (Exception e) {
                builder.addStrictErrors(MessageFormat.format("The null transformation in collection ''{0}'' for property ''{1}'' with  value ''{2}'' is invalid: {3}.", collectionId, property, null_.get(), e.getMessage()));
            }
        }

        return builder;
    }
}
