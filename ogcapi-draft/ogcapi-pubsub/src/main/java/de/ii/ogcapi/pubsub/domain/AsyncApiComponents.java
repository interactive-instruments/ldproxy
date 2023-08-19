/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.domain;

import de.ii.ogcapi.features.core.domain.JsonSchema;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
public interface AsyncApiComponents {

  Map<String, AsyncApiMessage> getMessages();

  Map<String, JsonSchema> getSchemas();

  Map<String, AsyncApiSecurity> getSecuritySchemes();
}
