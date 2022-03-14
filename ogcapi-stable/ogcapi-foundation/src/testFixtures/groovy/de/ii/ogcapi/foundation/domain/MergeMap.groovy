/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain

trait MergeMap<T extends ExtensionConfiguration> {

    abstract T getMap();

    abstract T getMapFullMerged();

    def getUseCases() {
        super.getUseCases() << [
                "map into full",
                "merging a configuration with map values into a full configuration with differing values",
                "source and target maps should be merged",
                getMap(),
                getFull(),
                getMapFullMerged()
        ]
    }

}
