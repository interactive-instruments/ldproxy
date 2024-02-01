/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.ogcapi.features.search.domain.ImmutableQueryExpression;
import de.ii.ogcapi.styles.domain.ImmutableMbStyleStylesheet;
import de.ii.xtraplatform.codelists.domain.ImmutableCodelist;

public interface ValueBuilders {

  default ImmutableCodelist.Builder codelist() {
    return new ImmutableCodelist.Builder();
  }

  default ImmutableQueryExpression.Builder query() {
    return new ImmutableQueryExpression.Builder();
  }

  default ImmutableMbStyleStylesheet.Builder style() {
    return new ImmutableMbStyleStylesheet.Builder();
  }
}
