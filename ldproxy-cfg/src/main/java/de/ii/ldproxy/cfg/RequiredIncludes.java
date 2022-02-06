/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import com.fasterxml.jackson.databind.ext.Java7HandlersImpl;

class RequiredIncludes {

  private final Java7HandlersImpl java7Handlers;

  RequiredIncludes() {
    this.java7Handlers = new Java7HandlersImpl();
  }
}
