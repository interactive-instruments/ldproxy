package de.ii.ldproxy.codelists;

import de.ii.xtraplatform.entity.api.AbstractPersistentEntity;
import de.ii.xtraplatform.entity.api.handler.Entity;
import de.ii.xtraplatform.service.api.Service;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.HandlerDeclaration;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@Component
@Provides
@Entity(entityType = Codelist.class, dataType = CodelistData.class)
// TODO: @Stereotype does not seem to work, maybe test with bnd-ipojo-plugin
// needed to register the ConfigurationHandler when no other properties are set
@HandlerDeclaration("<properties></properties>")

public class CodelistEntity extends AbstractPersistentEntity<CodelistData> implements Codelist {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodelistEntity.class);

    //TODO: setData not called without this
    @Validate
    void onStart() {
        LOGGER.debug("STARTED {} {}", getId(), shouldRegister());
    }

    @Override
    public String getValue(String key) {
        return getData().getEntries().get(key);
    }

    @Override
    protected CodelistData dataToImmutable(CodelistData data) {
        return ImmutableCodelistData.copyOf(data);
    }

    @Override
    public String getType() {
        return null;
    }
}
