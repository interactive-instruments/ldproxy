/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app.processing;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.processing.FeatureProcess;
import de.ii.ogcapi.features.core.domain.processing.FeatureProcessChain;
import de.ii.ogcapi.features.core.domain.processing.FeatureProcessInfo;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class FeatureProcessInfoImpl implements FeatureProcessInfo {

  private final ExtensionRegistry extensionRegistry;

  @Inject
  public FeatureProcessInfoImpl(ExtensionRegistry extensionRegistry) {
    this.extensionRegistry = extensionRegistry;
  }

  @Override
  public List<FeatureProcessChain> getProcessingChains(
      OgcApiDataV2 apiData, Class<? extends FeatureProcess> processType) {
    ImmutableList.Builder<FeatureProcessChain> chainBuilder = new ImmutableList.Builder<>();
    extensionRegistry.getExtensionsForType(processType).stream()
        .filter(process -> !process.getSupportedCollections(apiData).isEmpty())
        .filter(process -> process.isEnabledForApi(apiData))
        .forEach(
            process -> {
              ImmutableList<FeatureProcess> processList = ImmutableList.of(process);
              if (!process.isNeverTerminal()) {
                chainBuilder.add(new FeatureProcessChain(processList));
              }
              nextChainElement(apiData, processType, chainBuilder, process, processList);
            });
    return chainBuilder.build();
  }

  @Override
  public List<FeatureProcessChain> getProcessingChains(
      OgcApiDataV2 apiData, String collectionId, Class<? extends FeatureProcess> processType) {
    ImmutableList.Builder<FeatureProcessChain> chainBuilder = new ImmutableList.Builder<>();
    extensionRegistry.getExtensionsForType(processType).stream()
        .filter(process -> process.getSupportedCollections(apiData).contains(collectionId))
        .filter(process -> process.isEnabledForApi(apiData))
        .forEach(
            process -> {
              ImmutableList<FeatureProcess> processList = ImmutableList.of(process);
              if (!process.isNeverTerminal()) {
                chainBuilder.add(new FeatureProcessChain(processList));
              }
              nextChainElement(apiData, processType, chainBuilder, process, processList);
            });
    return chainBuilder.build();
  }

  private void nextChainElement(
      OgcApiDataV2 apiData,
      @SuppressWarnings("unused") Class<? extends FeatureProcess> processType,
      ImmutableList.Builder<FeatureProcessChain> chainBuilder,
      FeatureProcess process,
      ImmutableList<FeatureProcess> processList) {
    extensionRegistry.getExtensionsForType(FeatureProcess.class).stream()
        .filter(nextProcess -> nextProcess.getSupportedProcesses(apiData).contains(process))
        .filter(nextProcess -> nextProcess.isEnabledForApi(apiData))
        .forEach(
            nextProcess -> {
              ImmutableList.Builder<FeatureProcess> builder = ImmutableList.builder();
              builder.addAll(processList).add(nextProcess);
              ImmutableList<FeatureProcess> newProcessList = builder.build();
              if (!nextProcess.isNeverTerminal()) {
                chainBuilder.add(new FeatureProcessChain(newProcessList));
              }
              nextChainElement(apiData, processType, chainBuilder, nextProcess, newProcessList);
            });
  }

  @Override
  public boolean matches(
      OgcApiDataV2 apiData,
      Class<? extends FeatureProcess> processType,
      String definitionPath,
      String... processNames) {
    return getProcessingChains(apiData, processType).stream()
        .filter(chain -> chain.includes(processNames))
        .map(chain -> "/collections/{collectionId}" + chain.getSubSubPath())
        .anyMatch(path -> path.equals(definitionPath));
  }
}
