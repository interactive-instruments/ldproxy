package de.ii.ldproxy.ogcapi.application;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ImmutableMetadata;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.Metadata;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiBuildingBlock;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.xtraplatform.entities.domain.EntityDataBuilder;
import de.ii.xtraplatform.entities.domain.EntityDataDefaults;
import de.ii.xtraplatform.entities.domain.handler.Entity;
import de.ii.xtraplatform.service.api.Service;
import de.ii.xtraplatform.store.domain.KeyPathAlias;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Provides(properties = {
        @StaticServiceProperty(name = Entity.TYPE_KEY, type = "java.lang.String", value = Service.TYPE),
        @StaticServiceProperty(name = Entity.SUB_TYPE_KEY, type = "java.lang.String", value = OgcApiApiDataV2.SERVICE_TYPE)
})
@Instantiate
public class OgcApiApiDataV2Defaults implements EntityDataDefaults<OgcApiApiDataV2> {

    private final OgcApiExtensionRegistry extensionRegistry;

    public OgcApiApiDataV2Defaults(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public int getSortPriority() {
        return 0;
    }

    @Override
    public final EntityDataBuilder<OgcApiApiDataV2> getBuilderWithDefaults() {
        return new ImmutableOgcApiApiDataV2.Builder().shouldStart(true)
                                                     .secured(false)
                                                     .metadata(getMetadata())
                                                     .extensions(getBuildingBlocks());
    }

    @Override
    public Map<String, KeyPathAlias> getAliases() {
        return extensionRegistry.getExtensionsForType(OgcApiBuildingBlock.class)
                                .stream()
                                .map(ogcApiBuildingBlock -> ogcApiBuildingBlock.getDefaultConfiguration()
                                                                               .getBuildingBlock())
                                .map(buildingBlock -> new AbstractMap.SimpleImmutableEntry<String, KeyPathAlias>(buildingBlock.toLowerCase(), value -> ImmutableMap.of("api", ImmutableList.of(ImmutableMap.builder()
                                                                                                                                                                                                           .put("buildingBlock", buildingBlock)
                                                                                                                                                                                                           .putAll(value)
                                                                                                                                                                                                           .build()))))
                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected Metadata getMetadata() {
        return new ImmutableMetadata.Builder().licenseName("MPL 2.0")
                                              .build();
    }

    protected List<ExtensionConfiguration> getBuildingBlocks() {
        return extensionRegistry.getExtensionsForType(OgcApiBuildingBlock.class)
                                .stream()
                                .map(ogcApiBuildingBlock -> ogcApiBuildingBlock.getConfigurationBuilder()
                                                                               .defaultValues(ogcApiBuildingBlock.getDefaultConfiguration())
                                                                               .build())
                                .collect(Collectors.toList());
    }
}
