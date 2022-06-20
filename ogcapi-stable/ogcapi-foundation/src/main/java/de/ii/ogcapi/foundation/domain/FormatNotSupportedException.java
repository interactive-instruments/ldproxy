/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

public class FormatNotSupportedException extends RuntimeException {

    private static final long serialVersionUID = -4303512711221862724L;

    public FormatNotSupportedException(String message) {
        super(message);
    }
}
