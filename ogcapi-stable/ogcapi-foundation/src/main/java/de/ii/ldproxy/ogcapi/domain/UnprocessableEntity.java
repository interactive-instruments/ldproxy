/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

public class UnprocessableEntity extends RuntimeException {

  public UnprocessableEntity() {
    super();
  }

  public UnprocessableEntity(String message) {
    super(message);
  }

  public UnprocessableEntity(String message, Throwable cause) {
    super(message, cause);
  }

  public UnprocessableEntity(Throwable cause) {
    super(cause);
  }

}
