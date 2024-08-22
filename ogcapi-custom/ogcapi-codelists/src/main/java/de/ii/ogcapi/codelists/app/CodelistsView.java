/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.codelists.app;

import de.ii.ogcapi.codelists.domain.CodelistEntry;
import de.ii.ogcapi.html.domain.OgcApiView;
import java.util.List;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Value.Immutable
@Value.Style(
    builder = "new",
    visibility = ImplementationVisibility.PUBLIC,
    deepImmutablesDetection = true)
public abstract class CodelistsView extends OgcApiView {

  public CodelistsView() {
    super("codelists.mustache");
  }

  public abstract List<CodelistEntry> codelistEntries();

  public abstract String none();
}
