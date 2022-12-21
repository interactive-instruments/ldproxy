/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.app.html;

import de.ii.ogcapi.common.domain.ConformanceDeclaration;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.html.domain.OgcApiView;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
public abstract class OgcApiConformanceDeclarationView extends OgcApiView {

  public abstract ConformanceDeclaration conformanceDeclaration();

  public abstract I18n i18n();

  public abstract Optional<Locale> language();

  public OgcApiConformanceDeclarationView() {
    super("conformanceDeclaration.mustache");
  }

  public List<String> getClasses() {
    return conformanceDeclaration().getConformsTo().stream().sorted().collect(Collectors.toList());
  }
}
