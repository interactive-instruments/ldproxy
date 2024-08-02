/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.codelists.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ogcapi.foundation.domain.PageRepresentation;
import java.util.Comparator;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableCodelists.class)
public abstract class Codelists extends PageRepresentation {

  public static final String SCHEMA_REF = "#/components/schemas/Codelists";

  public abstract List<CodelistEntry> getCodelistEntries();

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<Codelists> FUNNEL =
      (from, into) -> {
        PageRepresentation.FUNNEL.funnel(from, into);
        from.getCodelistEntries().stream()
            .sorted(Comparator.comparing(CodelistEntry::getId))
            .forEachOrdered(value -> CodelistEntry.FUNNEL.funnel(value, into));
      };
}
