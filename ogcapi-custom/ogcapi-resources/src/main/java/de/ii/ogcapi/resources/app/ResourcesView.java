/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.app;

import de.ii.ogcapi.html.domain.OgcApiView;
import java.util.List;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Value.Immutable
@Value.Style(
    builder = "new",
    visibility = ImplementationVisibility.PUBLIC,
    deepImmutablesDetection = true)
public abstract class ResourcesView extends OgcApiView {

  public ResourcesView() {
    super("resources.mustache");
  }

  public abstract List<Resource> resources();

  public abstract String none();
}
