package de.ii.ldproxy.ogcapi.features.processing;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import org.apache.felix.ipojo.annotations.*;

import java.util.List;

@Component
@Provides
@Instantiate
public class FeatureProcessInfoImpl implements FeatureProcessInfo {

    @Requires
    I18n i18n;

    private final ExtensionRegistry extensionRegistry;

    public FeatureProcessInfoImpl(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    public List<FeatureProcessChain> getProcessingChains(OgcApiDataV2 apiData,
                                                         Class<? extends FeatureProcess> processType) {
        ImmutableList.Builder<FeatureProcessChain> chainBuilder = new ImmutableList.Builder<FeatureProcessChain>();
        extensionRegistry.getExtensionsForType(processType).stream()
                .filter(process -> !process.getSupportedCollections(apiData).isEmpty())
                .filter(process -> process.isEnabledForApi(apiData))
                .forEach(process -> {
                    ImmutableList<FeatureProcess> processList = ImmutableList.of(process);
                    chainBuilder.add(new FeatureProcessChain(processList));
                    nextChainElement(apiData, processType, chainBuilder, process, processList);

                });
        return chainBuilder.build();
    }

    public List<FeatureProcessChain> getProcessingChains(OgcApiDataV2 apiData, String collectionId,
                                                         Class<? extends FeatureProcess> processType) {
        ImmutableList.Builder<FeatureProcessChain> chainBuilder = new ImmutableList.Builder<FeatureProcessChain>();
        extensionRegistry.getExtensionsForType(processType).stream()
                .filter(process -> process.getSupportedCollections(apiData).contains(collectionId))
                .filter(process -> process.isEnabledForApi(apiData))
                .forEach(process -> {
                    ImmutableList<FeatureProcess> processList = ImmutableList.of(process);
                    chainBuilder.add(new FeatureProcessChain(processList));
                    nextChainElement(apiData, processType, chainBuilder, process, processList);

                });
        return chainBuilder.build();
    }

    private void nextChainElement(OgcApiDataV2 apiData, Class<? extends FeatureProcess> processType,
                                  ImmutableList.Builder<FeatureProcessChain> chainBuilder, FeatureProcess process,
                                  ImmutableList<FeatureProcess> processList) {
        extensionRegistry.getExtensionsForType(FeatureProcess.class).stream()
                .filter(nextProcess -> nextProcess.getSupportedProcesses(apiData).contains(process))
                .filter(nextProcess -> nextProcess.isEnabledForApi(apiData))
                .forEach(nextProcess -> {
                    ImmutableList.Builder<FeatureProcess> builder = ImmutableList.builder();
                    builder.addAll(processList)
                            .add(nextProcess);
                    ImmutableList<FeatureProcess> newProcessList = builder.build();
                    chainBuilder.add(new FeatureProcessChain(newProcessList));
                    nextChainElement(apiData, processType, chainBuilder, nextProcess, newProcessList);
                });
    }

    public boolean matches(OgcApiDataV2 apiData, Class<? extends FeatureProcess> processType,
                           String definitionPath, String... processNames) {
        return getProcessingChains(apiData, processType)
                        .stream()
                        .filter(chain -> chain.includes(processNames))
                        .map(chain -> "/collections/{collectionId}"+chain.getSubSubPath())
                        .anyMatch(path -> path.equals(definitionPath));
    }
}
