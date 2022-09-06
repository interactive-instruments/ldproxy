/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface ApiSecurity {

  enum ScopeElements {
    READ_WRITE,
    TAG,
    MODULE,
    OPERATION,
    METHOD,
    FORMAT
  }

  String SCOPE_READ = "read";
  String SCOPE_WRITE = "write";

  @Nullable
  Boolean getEnabled();

  Set<ScopeElements> getScopeElements();

  List<String> getPublicScopes();
}
