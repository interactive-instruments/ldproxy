package de.ii.ldproxy.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
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
     * @param withArrayBrackets
     * @return the list of all property names of the feature type
     */
    public static Map<String, SchemaBase.Type> getPropertyTypes(FeatureSchema featureType, boolean withArrayBrackets) {
        if (Objects.isNull(featureType))
            return ImmutableMap.of();

        ImmutableMap.Builder<String, SchemaBase.Type> nameTypeMapBuilder = new ImmutableMap.Builder<>();
        featureType.getProperties()
                          .stream()
                          .forEach(featureProperty -> addPropertyTypes(nameTypeMapBuilder, featureProperty, "", withArrayBrackets));
        return nameTypeMapBuilder.build();
    }

    private static void addPropertyTypes(ImmutableMap.Builder<String, SchemaBase.Type> nameTypeMapBuilder, FeatureSchema property, String basePath, boolean withArrayBrackets) {
        if (property.isObject()) {
            property.getProperties()
                    .stream()
                    .forEach(subProperty -> addPropertyTypes(nameTypeMapBuilder, subProperty, getPropertyName(property, basePath, withArrayBrackets), withArrayBrackets));
        } else {
            nameTypeMapBuilder.put(getPropertyType(property, basePath, withArrayBrackets));
        }
    }

    private static Map.Entry<String, SchemaBase.Type> getPropertyType(FeatureSchema property, String basePath, boolean withArrayBrackets) {
        String propertyName = (basePath.isEmpty() ? "" : basePath + ".") + property.getName().trim() + (property.isArray() && withArrayBrackets ? "[]" : "");
        return new AbstractMap.SimpleImmutableEntry<>(propertyName, property.getType());
    }

    /**
     * @param featureType
     * @return a map with an entry for each property and the label/title of the property as the value
     */
    public static Map<String, String> getNameTitleMap(FeatureSchema featureType) {
        if (Objects.isNull(featureType))
            return ImmutableMap.of();

        ImmutableMap.Builder<String, String> nameTitleMapBuilder = new ImmutableMap.Builder<>();
        featureType.getProperties()
                   .stream()
                   .forEach(featureProperty -> addPropertyTitles(nameTitleMapBuilder, featureProperty, "", ""));
        return nameTitleMapBuilder.build();
    }

    private static void addPropertyTitles(ImmutableMap.Builder<String, String> nameTitleMapBuilder, FeatureSchema property, String basePath, String baseTitle) {
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

    private static Map.Entry<String, String> getPropertyTitle(FeatureSchema property, String basePath, String baseTitle) {
        String name = (basePath.isEmpty() ? "" : basePath + ".") + property.getName().trim();
        String title = (baseTitle.isEmpty() ? "" : baseTitle + " > ") + property.getLabel().orElse(property.getName()).trim();
        return new AbstractMap.SimpleImmutableEntry<>(name, title);
    }

    // TODO since the following take apiData as input an not the schema, should these be moved to a CollectionInfo class?

    public List<String> getPropertyNames(OgcApiDataV2 apiData, String collectionId) {
        return getPropertyNames(apiData, collectionId, false, false);
    }

    public List<String> getPropertyNames(OgcApiDataV2 apiData, String collectionId, boolean withSpatial, boolean withArrayBrackets) {
        FeatureSchema schema = providers.getFeatureSchema(apiData, apiData.getCollections().get(collectionId));
        return schema.getProperties()
                     .stream()
                     .filter(featureProperty -> !featureProperty.isSpatial() || withSpatial)
                     .map(featureProperty -> getPropertyNames(featureProperty, "", withArrayBrackets))
                     .flatMap(List::stream)
                     .collect(Collectors.toList());
    }
}
