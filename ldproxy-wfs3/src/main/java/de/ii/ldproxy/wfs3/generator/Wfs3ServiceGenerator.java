package de.ii.ldproxy.wfs3.generator;

import de.ii.ldproxy.wfs3.Gml2Wfs3GenericMappingProvider;
import de.ii.ldproxy.wfs3.api.ModifiableWfs3ServiceData;
import de.ii.ldproxy.wfs3.api.Wfs3ExtensionRegistry;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.xtraplatform.entity.api.EntityDataGenerator;
import de.ii.xtraplatform.feature.query.api.FeatureProvider;
import de.ii.xtraplatform.feature.query.api.FeatureProviderMetadataConsumer;
import de.ii.xtraplatform.feature.query.api.FeatureProviderRegistry;
import de.ii.xtraplatform.feature.query.api.MultiFeatureProviderMetadataConsumer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.feature.transformer.api.TransformingFeatureProvider;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3ServiceGenerator implements EntityDataGenerator<Wfs3ServiceData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3ServiceGenerator.class);

    @Requires
    private Wfs3ExtensionRegistry wfs3ConformanceClassRegistry;

    @Requires
    private FeatureProviderRegistry featureProviderRegistry;

    @Override
    public Class<Wfs3ServiceData> getType() {
        return Wfs3ServiceData.class;
    }

    @Override
    public Wfs3ServiceData generate(Wfs3ServiceData partialData) {
        try {
            FeatureProvider featureProvider = featureProviderRegistry.createFeatureProvider(partialData.getFeatureProvider());

            if (!(featureProvider instanceof FeatureProvider.MetadataAware && featureProvider instanceof TransformingFeatureProvider.SchemaAware)) {
                throw new IllegalArgumentException("feature provider not metadata aware");
            }

            LOGGER.debug("GENERATING {} {}", Wfs3ServiceData.class, partialData.getId());

            FeatureProvider.MetadataAware metadataAware = (FeatureProvider.MetadataAware) featureProvider;
            TransformingFeatureProvider.SchemaAware schemaAware = (TransformingFeatureProvider.SchemaAware) featureProvider;
            FeatureProvider.DataGenerator dataGenerator = (TransformingFeatureProvider.DataGenerator) featureProvider;

            FeatureProviderMetadataConsumer metadataConsumer = new MultiFeatureProviderMetadataConsumer(new Metadata2Wfs3((ModifiableWfs3ServiceData) partialData), dataGenerator.getDataGenerator(partialData.getFeatureProvider()));
            metadataAware.getMetadata(metadataConsumer);
            schemaAware.getSchema(((TransformingFeatureProvider.DataGenerator) dataGenerator).getMappingGenerator(partialData.getFeatureProvider(), getMappingProviders()), partialData.getFeatureProvider().getFeatureTypes());

            ((ModifiableWfs3ServiceData) partialData).setShouldStart(true);
            return partialData;
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }

    private List<TargetMappingProviderFromGml> getMappingProviders() {
        return Stream.concat(
                Stream.of(new Gml2Wfs3GenericMappingProvider()),
                wfs3ConformanceClassRegistry.getOutputFormats()
                                            .values()
                                            .stream()
                                            .map(Wfs3OutputFormatExtension::getMappingGenerator)
                                            .filter(Optional::isPresent)
                                            .map(Optional::get)
        )
                     .collect(Collectors.toList());
    }
}
