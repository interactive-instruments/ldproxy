/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles.representation;

import de.ii.xtraplatform.rest.views.GenericView;

public class StyleView extends GenericView {
    public String styleUrl;
    public String serviceId;

    public StyleView(String styleUrl, String serviceId) {
        super("style", null);
        this.styleUrl = styleUrl;
        this.serviceId = serviceId;
    }
}
