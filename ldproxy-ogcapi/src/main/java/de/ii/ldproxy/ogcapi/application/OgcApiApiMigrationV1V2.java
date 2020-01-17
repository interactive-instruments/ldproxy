package de.ii.ldproxy.ogcapi.application;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.xtraplatform.event.store.EntityMigration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Provides(properties = {
        //TODO: how to connect to entity
        @StaticServiceProperty(name = "entityType", type = "java.lang.String", value = "services")
})
@Instantiate
public class OgcApiApiMigrationV1V2 implements EntityMigration<OgcApiApiDataV2> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiApiMigrationV1V2.class);

    @Override
    public long getSourceVersion() {
        return 1;
    }

    @Override
    public long getTargetVersion() {
        return 2;
    }

    @Override
    public OgcApiApiDataV2 migrate(OgcApiApiDataV2 entityData, byte[] serializedEntityData) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Migrated schema for entity '{}' with type 'services': v{} -> v{}", entityData.getId(), getSourceVersion(), getTargetVersion());
        }

        return entityData;
    }
}
