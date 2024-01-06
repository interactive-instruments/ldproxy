/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.pubsub.domain.asyncapi.AsyncApi;
import java.util.Optional;

@AutoMultiBind
public interface AsyncApiDefinitionFormatExtension extends FormatExtension {

  Object getAsyncApiEntity(AsyncApi asyncApi, ApiRequestContext apiRequestContext);

  default Optional<String> getRel() {
    return Optional.empty();
  }
}
