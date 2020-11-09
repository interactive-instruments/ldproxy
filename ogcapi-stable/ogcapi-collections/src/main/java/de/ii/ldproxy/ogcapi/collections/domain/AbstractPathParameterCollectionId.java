package de.ii.ldproxy.ogcapi.collections.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractPathParameterCollectionId implements OgcApiPathParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPathParameterCollectionId.class);

    protected final Map<Integer,Set<String>> apiCollectionMap;

    public AbstractPathParameterCollectionId() {
        this.apiCollectionMap = new HashMap<>();
    };

    @Override
    public String getPattern() {
        return "[\\w\\-]+";
    }

    @Override
    public boolean getExplodeInOpenApi() {
        return true;
    }

    @Override
    public Set<String> getValues(OgcApiDataV2 apiData) {
        if (!apiCollectionMap.containsKey(apiData.hashCode())) {
            apiCollectionMap.put(apiData.hashCode(), apiData.getCollections().keySet().stream()
                                                            .filter(collectionId -> apiData.isCollectionEnabled(collectionId))
                                                            .collect(Collectors.toSet()));
        }

        return apiCollectionMap.get(apiData.hashCode());
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return new StringSchema()._enum(ImmutableList.copyOf(getValues(apiData)));
    }

    @Override
    public String getName() {
        return "collectionId";
    }

    @Override
    public String getDescription() {
        return "The local identifier of a feature collection.";
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CollectionsConfiguration.class;
    }
}
