/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import com.google.common.base.Charsets;
import io.dropwizard.views.View;

/**
 * @author zahnen
 */
public class LoginView extends View {

  public final String oidcUri;
  public final String authUri;
  public final String callbackUri;
  public final String redirectUri;
  public final boolean callback;

  public LoginView(
      String oidcUri, String authUri, String callbackUri, String redirectUri, boolean callback) {
    super("/templates/login.mustache", Charsets.UTF_8);
    this.oidcUri = oidcUri;
    this.authUri = authUri;
    this.callbackUri = callbackUri;
    this.redirectUri = redirectUri;
    this.callback = callback;
  }
}
