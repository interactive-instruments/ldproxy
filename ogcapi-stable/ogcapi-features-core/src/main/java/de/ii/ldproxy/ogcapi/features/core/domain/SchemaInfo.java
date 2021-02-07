package de.ii.ldproxy.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

@Component
@Provides(specifications = {SchemaInfo.class})
@Instantiate
public class SchemaInfo {

    @Requires
    FeaturesCoreProviders providers;

    /**
     * @param featureType
     * @param withArrayBrackets
     * @return the list of all property names of the feature type
     */
    public static List<String> getPropertyNames(FeatureSchema featureType, boolean withArrayBrackets) {
        if (Objects.isNull(featureType))
            return ImmutableList.of();

        return featureType.getProperties()
                          .stream()
                          .map(featureProperty -> getPropertyNames(featureProperty, "", withArrayBrackets))
                          .flatMap(List::stream)
                          .collect(Collectors.toList());
    }

    private static List<String> getPropertyNames(FeatureSchema property, String basePath, boolean withArrayBrackets) {
        List<String> propertyNames = new Vector<>();
        if (property.isObject()) {
            property.getProperties()
                    .stream()
                    .forEach(subProperty -> propertyNames.addAll(getPropertyNames(subProperty, getPropertyName(property, basePath, withArrayBrackets), withArrayBrackets)));
        } else {
            propertyNames.add(getPropertyName(property, basePath, withArrayBrackets));
        }
        return propertyNames;
    }

    private static String getPropertyName(FeatureSchema property, String basePath, boolean withArrayBrackets) {
        return (basePath.isEmpty() ? "" : basePath + ".") + property.getName().trim() + (property.isArray() && withArrayBrackets ? "[]" : "");
    }

    /**
     * @param featureType
     * @return a map with an entry for each property and the label/title of the property as the value
     */
    public Map<String, String> getNameTitleMap(FeatureSchema featureType) {
        if (Objects.isNull(featureType))
            return ImmutableMap.of();

        ImmutableMap.Builder<String, String> nameTitleMapBuilder = new ImmutableMap.Builder<>();
        featureType.getProperties()
                   .stream()
                   .forEach(featureProperty -> addPropertyTitles(nameTitleMapBuilder, featureProperty, "", ""));
        return nameTitleMapBuilder.build();
    }

    private void addPropertyTitles(ImmutableMap.Builder<String, String> nameTitleMapBuilder, FeatureSchema property, String basePath, String baseTitle) {
        if (property.isObject()) {
            nameTitleMapBuilder.put(getPropertyTitle(property, basePath, baseTitle));
            property.getProperties()
                    .stream()
                    .forEach(subProperty -> {
                        Map.Entry<String, String> entry = getPropertyTitle(property, basePath, baseTitle);
                        addPropertyTitles(nameTitleMapBuilder, subProperty, entry.getKey(), entry.getValue());
                    });
        } else {
            nameTitleMapBuilder.put(getPropertyTitle(property, basePath, baseTitle));
        }
    }

    private Map.Entry<String, String> getPropertyTitle(FeatureSchema property, String basePath, String baseTitle) {
        String name = (basePath.isEmpty() ? "" : basePath + ".") + property.getName().trim();
        String title = (baseTitle.isEmpty() ? "" : baseTitle + " > ") + property.getLabel().orElse(property.getName()).trim();
        return new AbstractMap.SimpleImmutableEntry<>(name, title);
    }

    public List<String> getPropertyNames(OgcApiDataV2 apiData, String collectionId) {
        return getPropertyNames(apiData, collectionId, false, false);
    }

    public List<String> getPropertyNames(OgcApiDataV2 apiData, String collectionId, boolean withSpatial, boolean withArrayBrackets) {
        FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, apiData.getCollections().get(collectionId));
        String featureTypeId = apiData.getCollections()
                                      .get(collectionId)
                                      .getExtension(FeaturesCoreConfiguration.class)
                                      .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                      .orElse(collectionId);
        return featureProvider.getData()
                              .getTypes()
                              .get(featureTypeId)
                              .getProperties()
                              .stream()
                              .filter(featureProperty -> !featureProperty.isSpatial() || withSpatial)
                              .map(featureProperty -> getPropertyNames(featureProperty, "", withArrayBrackets))
                              .flatMap(List::stream)
                              .collect(Collectors.toList());
    }
}
