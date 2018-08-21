package de.ii.ldproxy.codelists;

import de.ii.xtraplatform.entity.api.PersistentEntity;

/**
 * @author zahnen
 */
public interface Codelist extends PersistentEntity {
    String ENTITY_TYPE = "codelists";

    @Override
    default String getType() {
        return ENTITY_TYPE;
    }
    String getValue(String key);
}
