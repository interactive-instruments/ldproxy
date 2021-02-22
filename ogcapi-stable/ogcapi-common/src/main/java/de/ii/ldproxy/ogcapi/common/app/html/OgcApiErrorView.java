/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app.html;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.domain.ApiErrorMessage;
import io.dropwizard.views.View;

public class OgcApiErrorView extends View {

    public String title;
    public Integer status;
    public String detail;
    public String instance;

    protected OgcApiErrorView(ApiErrorMessage errorMessage) {
        super("/templates/exception.mustache", Charsets.UTF_8);
        this.status = errorMessage.getCode();
        this.title = errorMessage.getMessage();
        this.detail = errorMessage.getDetails();
        this.instance = errorMessage.getInstance();
    }
}
