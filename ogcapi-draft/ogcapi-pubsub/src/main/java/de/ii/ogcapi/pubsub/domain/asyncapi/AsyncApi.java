/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.domain.asyncapi;

import java.util.Map;
import org.immutables.value.Value;

/**
 * Currently the Java tooling for AsyncAPI is not good, so we generate the AsyncAPI definition
 * ourselves.
 */
@Value.Immutable
public interface AsyncApi {

  default String getAsyncapi() {
    return "2.6.0";
  }

  AsyncApiInfo getInfo();

  Map<String, AsyncApiServer> getServers();

  Map<String, AsyncApiChannel> getChannels();

  AsyncApiComponents getComponents();
}
