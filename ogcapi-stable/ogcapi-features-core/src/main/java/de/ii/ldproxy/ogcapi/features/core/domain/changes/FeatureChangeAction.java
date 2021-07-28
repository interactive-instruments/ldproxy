/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain.changes;

import java.io.IOException;
import java.util.function.Consumer;

public interface FeatureChangeAction {

    FeatureChangeAction create();

    int getSortPriority();

    default void onEvent(ChangeContext changeContext, Consumer<ChangeContext> next) throws IOException {
        switch (changeContext.getOperation()) {
            case INSERT:
                onInsert(changeContext, next);
                break;
            case UPDATE:
                onUpdate(changeContext, next);
                break;
            case DELETE:
                onDelete(changeContext, next);
                break;
        }
    }

    default void onInsert(ChangeContext changeContext, Consumer<ChangeContext> next) { next.accept(changeContext); }

    default void onUpdate(ChangeContext changeContext, Consumer<ChangeContext> next) { next.accept(changeContext); }

    default void onDelete(ChangeContext changeContext, Consumer<ChangeContext> next) { next.accept(changeContext); }
}
