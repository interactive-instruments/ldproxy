/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.codelists;

import de.ii.xtraplatform.entity.api.AbstractPersistentEntity;
import de.ii.xtraplatform.entity.api.EntityComponent;
import de.ii.xtraplatform.entity.api.handler.Entity;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.HandlerDeclaration;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author zahnen
 */

@EntityComponent
@Entity(entityType = Codelist.class, dataType = CodelistData.class)
public class CodelistEntity extends AbstractPersistentEntity<CodelistData> implements Codelist {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodelistEntity.class);

    @Override
    protected void onStart() {
        LOGGER.debug("Codelist loaded {} {}", getId(), shouldRegister());
    }

    @Override
    public String getValue(String key) {

        return Optional.ofNullable(getData().getEntries()
                                            .get(key))
                       .orElse(getData().getFallback()
                                        .orElse(key));
    }

    @Override
    public CodelistData getData() {
        return super.getData();
    }

    @Override
    protected boolean shouldRegister() {
        return true;
    }
}
