package de.ii.ldproxy.codelists;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.entity.api.AbstractEntityData;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Map;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ImmutableCodelistData.class)
public abstract class CodelistData extends AbstractEntityData {
    public abstract Map<String,String> getEntries();

    @Value.Default
    @Override
    public long getCreatedAt() {
        return Instant.now().toEpochMilli();
    }

    @Value.Default
    @Override
    public long getLastModified() {
        return Instant.now().toEpochMilli();
    }
}
