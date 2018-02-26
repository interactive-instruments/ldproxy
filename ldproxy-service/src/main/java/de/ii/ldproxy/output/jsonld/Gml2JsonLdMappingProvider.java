/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.jsonld;

import de.ii.ldproxy.output.html.Gml2MicrodataMappingProvider;

/**
 * @author zahnen
 */
public class Gml2JsonLdMappingProvider extends Gml2MicrodataMappingProvider {


    public static final String MIME_TYPE = "application/ld+json";

    @Override
    public String getTargetType() {
        return MIME_TYPE;
    }
}
