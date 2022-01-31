/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain

trait MergeNested<T extends ExtensionConfiguration> {

    abstract T getNested();

    abstract T getNestedFullMerged();

    def getUseCases() {
        super.getUseCases() << [
                "nested mergeable into full",
                "merging a configuration with nested mergeable values into a full configuration with differing values",
                "source and target values should be merged",
                getNested(),
                getFull(),
                getNestedFullMerged()
        ]
    }

}
