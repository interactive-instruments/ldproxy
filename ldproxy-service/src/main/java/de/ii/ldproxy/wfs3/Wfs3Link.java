/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

/**
 * @author zahnen
 */

//@XmlType(propOrder={"href","rel","type", "title"})
public class Wfs3Link {
    public String href;
    public String rel;
    public String type;
    public String title;

    public Wfs3Link(String href, String rel, String type, String title) {
        this.href = href;
        this.rel = rel;
        this.type = type;
        this.title = title;
    }
}
