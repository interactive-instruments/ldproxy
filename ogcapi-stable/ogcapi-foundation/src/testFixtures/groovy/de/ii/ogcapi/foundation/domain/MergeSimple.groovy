/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain

trait MergeSimple<T extends ExtensionConfiguration> {

    abstract T getSimple();

    abstract T getSimpleFullMerged();

    def getUseCases() {
        super.getUseCases() << [
                "simple into full",
                "merging a configuration with simple values into a full configuration with differing values",
                "values from the source configuration should override target values",
                getSimple(),
                getFull(),
                getSimpleFullMerged()
        ]
    }

}
