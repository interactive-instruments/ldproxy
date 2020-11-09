/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformations;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.legacy.MicrodataGeometryMapping;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CoordinateTuple;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.dropwizard.domain.MustacheRenderer;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.geometries.domain.ImmutableCoordinatesTransformer;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.streams.domain.HttpClient;
import de.ii.xtraplatform.xml.domain.XMLPathTracker;
import io.dropwizard.views.ViewRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * @author zahnen
 */
public class FeatureTransformerHtmlComplexObjects implements FeatureTransformer2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerHtmlComplexObjects.class);

    private OutputStreamWriter outputStreamWriter;
    protected XMLPathTracker currentPath;
    protected ObjectDTO currentFeature;
    protected boolean isFeatureCollection;
    protected ViewRenderer mustacheRenderer;
    protected int page;
    protected int pageSize;
    protected CrsTransformer crsTransformer;
    protected Map<String, Codelist> codelists;

    public FeatureCollectionView dataset;

    private String serviceUrl;

    MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE currentGeometryType;
    ImmutableCoordinatesTransformer.Builder coordinatesTransformerBuilder;
    private Writer coordinatesWriter;
    private Writer coordinatesOutput;
    private PropertyDTO currentGeometryPart;
    private int currentGeometryParts;
    private boolean currentGeometryWritten;

    private final FeatureTransformationContextHtml transformationContext;
    private final int offset;
    private final FeaturesHtmlConfiguration htmlConfiguration;
    private final Optional<FeatureSchema> featureSchema;
    private final Map<String, HtmlPropertyTransformations> transformations;
    private final boolean isSchemaOrgEnabled;

    private StringBuilder currentValueBuilder = new StringBuilder();
    private Map<String,Integer> pathMap = new HashMap<>();
    private ValueDTO currentValue = null;
    private FeatureProperty currentProperty = null;
    private ArrayList<FeatureProperty> currentFeatureProperties = null;
    private boolean combineCurrentPropertyValues;

    private ValueDTO getValue(FeatureProperty baseProperty, FeatureProperty htmlProperty, List<Integer> index) {
        String baseName = baseProperty.getName();
        List<String> baseNameSections = Splitter.on('.').splitToList(baseName);
        String htmlName = htmlProperty.getName();
        List<String> htmlNameSections;
        if (htmlName.equals(baseName) && featureSchema.isPresent()) {
            htmlNameSections = new ArrayList<>();
            FeatureSchema schema = featureSchema.get();
            for (String section: baseNameSections) {
                String propertyName = section.replace("[]", "");
                schema = schema.getProperties()
                        .stream()
                        .filter(prop -> prop.getName().equals(propertyName))
                        .findAny()
                        .orElse(null);
                if (schema!=null) {
                    htmlNameSections.add(schema.getLabel().orElse(schema.getName()));
                } else {
                    htmlNameSections.add(propertyName);
                }
            }
        } else {
            htmlNameSections = Splitter.on('|').splitToList(htmlName);
        }

        Map<String, String> addInfo = baseProperty.getAdditionalInfo();
        boolean linkTitle = false;
        boolean linkHref = false;
        if (Objects.nonNull(addInfo)) {
            if (addInfo.containsKey("role")) {
                linkHref = addInfo.get("role").equalsIgnoreCase("LINKHREF");
                linkTitle = addInfo.get("role").equalsIgnoreCase("LINKTITLE");
            }
        }

        // determine context in the properties of this feature
        String curPath = null;
        ObjectDTO valueContext = currentFeature;
        int arrays = 0;
        int objectLevel = 0;
        for (String name : baseNameSections) {
            curPath = Objects.isNull(curPath) ? name : curPath.concat("."+name);
            boolean isArray = name.endsWith("]");
            PropertyDTO property = valueContext.get(curPath);
            if (Objects.isNull(property)) {
                property = new PropertyDTO();
                property.baseName = curPath;
                valueContext.addChild(property);
            }
            if (curPath.equals(baseName) ||
                linkHref && curPath.concat(".href").equals(baseName) ||
                linkTitle && curPath.concat(".title").equals(baseName)) {
                // we are at the end of the path, add the (new) value and return it;
                // this includes the special case of a link object that is mapped to a single value in the HTML
                int curCount = property.values.size();
                int idx = isArray && index.size()>=arrays+1 ? index.get(arrays++) : 1;
                if (curCount==idx-1) {
                    if (curCount == 0) {
                        property.name = htmlNameSections.get(Math.min(objectLevel,htmlNameSections.size()-1));
                    }
                    return property.addValue( linkHref || linkTitle ? "<a href=\"{{href}}\">{{title}}</a>" : "{{value}}"); // template
                } else if (curCount >= idx) {
                    return property.values.get(idx-1);
                } else {
                    LOGGER.error("Access to property {} failed. Index {} could not be mapped. A null value is used.", baseName, idx);
                    return null;
                }
            } else {
                // we have an object, either the latest object in the existing list or a new object
                int curCount = property.childList.size();
                int idx = isArray && index.size()>=arrays+1 ? index.get(arrays++) : 1;
                if (curCount==idx-1) {
                    valueContext = new ObjectDTO();
                    valueContext.sortPriority = curCount+1;
                    property.addChild(valueContext);
                    if (curCount == 0) {
                        property.name = htmlNameSections.get(Math.min(objectLevel, htmlNameSections.size() - 1));
                        valueContext.name = property.name;
                    } else {
                        valueContext.name = "———";
                        valueContext.isFirstObject = false;
                    }
                    objectLevel++;
                } else if (curCount>=idx) {
                    valueContext = (ObjectDTO) property.childList.get(idx-1);
                    objectLevel++;
                } else {
                    LOGGER.error("Access to property {} failed at path element {}. Index {} could not be mapped. A null value is used.", baseName, curPath, idx);
                    return null;
                }
            }
        }

        LOGGER.error("Access to property {} failed. A null value is used.", baseName);
        return null;
    }

    public FeatureTransformerHtmlComplexObjects(FeatureTransformationContextHtml transformationContext, HttpClient httpClient) {
        this.outputStreamWriter = new OutputStreamWriter(transformationContext.getOutputStream());
        this.currentPath = new XMLPathTracker();
        this.isFeatureCollection = transformationContext.isFeatureCollection();
        this.page = transformationContext.getPage();
        this.pageSize = transformationContext.getLimit();
        this.offset = transformationContext.getOffset();
        this.crsTransformer = transformationContext.getCrsTransformer()
                                                   .orElse(null);

        this.dataset = transformationContext.getFeatureTypeDataset();

        this.serviceUrl = transformationContext.getServiceUrl();

        this.codelists = transformationContext.getCodelists();
        this.mustacheRenderer = transformationContext.getMustacheRenderer();
        this.transformationContext = transformationContext;
        this.htmlConfiguration = transformationContext.getHtmlConfiguration();
        this.featureSchema = transformationContext.getFeatureSchema();

        FeatureTypeConfigurationOgcApi featureTypeConfiguration = transformationContext.getApiData()
                                                                                       .getCollections()
                                                                                       .get(transformationContext.getCollectionId());

        Optional<FeatureTransformations> baseTransformations = featureTypeConfiguration
                .getExtension(FeaturesCoreConfiguration.class)
                .map(coreConfiguration -> coreConfiguration);

        this.transformations = featureTypeConfiguration
                .getExtension(FeaturesHtmlConfiguration.class)
                .map(htmlConfiguration -> htmlConfiguration.getTransformations(baseTransformations, transformationContext.getCodelists(), transformationContext.getServiceUrl(), isFeatureCollection))
                .orElse(ImmutableMap.of());

        this.isSchemaOrgEnabled = transformationContext.getHtmlConfiguration()
                                                       .getSchemaOrgEnabled();
    }

    @Override
    public String getTargetFormat() {
        return FeaturesFormatHtml.MEDIA_TYPE.type().toString();
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception {

        LOGGER.debug("START");

        if (isFeatureCollection && numberReturned.isPresent()) {
            long returned = numberReturned.getAsLong();
            long matched = numberMatched.orElse(-1);

            long pages = Math.max(page, 0);
            if (returned > 0 && matched > -1) {
                pages = Math.max(pages, matched / pageSize + (matched % pageSize > 0 ? 1 : 0));
            }

            LOGGER.debug("numberMatched {}", matched);
            LOGGER.debug("numberReturned {}", returned);
            LOGGER.debug("pageSize {}", pageSize);
            LOGGER.debug("page {}", page);
            LOGGER.debug("pages {}", pages);

            ImmutableList.Builder<NavigationDTO> pagination = new ImmutableList.Builder<>();
            ImmutableList.Builder<NavigationDTO> metaPagination = new ImmutableList.Builder<>();
            if (page > 1) {
                pagination
                        .add(new NavigationDTO("«", String.format("limit=%d&offset=%d", pageSize, 0)))
                        .add(new NavigationDTO("‹", String.format("limit=%d&offset=%d", pageSize, offset - pageSize)));
                metaPagination
                        .add(new NavigationDTO("prev", String.format("limit=%d&offset=%d", pageSize, offset - pageSize)));
            } else {
                pagination
                        .add(new NavigationDTO("«"))
                        .add(new NavigationDTO("‹"));
            }

            if (matched > -1) {
                long from = Math.max(1, page - 2);
                long to = Math.min(pages, from + 4);
                if (to == pages) {
                    from = Math.max(1, to - 4);
                }
                for (long i = from; i <= to; i++) {
                    if (i == page) {
                        pagination.add(new NavigationDTO(String.valueOf(i), true));
                    } else {
                        pagination.add(new NavigationDTO(String.valueOf(i), String.format("limit=%d&offset=%d", pageSize, (i - 1) * pageSize)));
                    }
                }

                if (page < pages) {
                    pagination
                            .add(new NavigationDTO("›", String.format("limit=%d&offset=%d", pageSize, offset + pageSize)))
                            .add(new NavigationDTO("»", String.format("limit=%d&offset=%d", pageSize, (pages - 1) * pageSize)));
                    metaPagination
                            .add(new NavigationDTO("next", String.format("limit=%d&offset=%d", pageSize, offset + pageSize)));
                } else {
                    pagination
                            .add(new NavigationDTO("›"))
                            .add(new NavigationDTO("»"));
                }
            } else {
                int from = Math.max(1, page - 2);
                int to = page;
                for (int i = from; i <= to; i++) {
                    if (i == page) {
                        pagination.add(new NavigationDTO(String.valueOf(i), true));
                    } else {
                        pagination.add(new NavigationDTO(String.valueOf(i), String.format("limit=%d&offset=%d", pageSize, (i - 1) * pageSize)));
                    }
                }
                if (returned >= pageSize) {
                    pagination
                            .add(new NavigationDTO("›", String.format("limit=%d&offset=%d", pageSize, offset + pageSize)));
                    metaPagination
                            .add(new NavigationDTO("next", String.format("limit=%d&offset=%d", pageSize, offset + pageSize)));
                } else {
                    pagination
                            .add(new NavigationDTO("›"));
                }
            }

            this.dataset.pagination = pagination.build();
            this.dataset.metaPagination = metaPagination.build();

        } else if (isFeatureCollection) {
            LOGGER.error("Pagination not supported by feature provider, the number of matched items was not provided.");
        }
    }

    @Override
    public void onEnd() throws Exception {

        ((MustacheRenderer) mustacheRenderer).render(dataset, outputStreamWriter);
        outputStreamWriter.flush();
    }

    @Override
    public void onFeatureStart(FeatureType featureType) throws Exception {
        currentFeature = new ObjectDTO();

        this.currentFeatureProperties = new ArrayList<>(featureType.getProperties().values());

        if (isFeatureCollection) {
            currentFeature.inCollection = true;
        }

        Optional<String> itemLabelFormat = htmlConfiguration.getItemLabelFormat();
        if (itemLabelFormat.isPresent()) {
            currentFeature.name = itemLabelFormat.get();
        }

        if (isSchemaOrgEnabled) {
            currentFeature.itemType = "http://schema.org/Place";
        }
    }

    @Override
    public void onFeatureEnd() throws Exception {
        // post-process feature to set defaults for unchanged values
        if (currentFeature.name != null) {
            currentFeature.name = currentFeature.name.replaceAll("\\{\\{[^}]*\\}\\}", "");
        } else {
            currentFeature.name = currentFeature.id.getFirstValue();
        }

        postProcessLinkTitles(currentFeature);

        if (!isFeatureCollection) {
            this.dataset.title = currentFeature.name;
            this.dataset.breadCrumbs.get(dataset.breadCrumbs.size() - 1).label = currentFeature.name;
        }
        dataset.features.add(currentFeature);
        currentFeature = null;
    }

    private void postProcessLinkTitles(ObjectDTO object) {
        for (PropertyDTO property: object.properties()) {
            for (ValueDTO value: property.values) {
                int idx = (Objects.nonNull(value.value)) ? value.value.indexOf("{{title}}") : -1;
                if (idx != -1) {
                    try {
                        String href = value.value.substring(9,idx-2);
                        String title = href.substring(href.lastIndexOf("/")+1);
                        value.value = value.value.replaceAll("\\{\\{title\\}\\}", URLDecoder.decode(title, StandardCharsets.UTF_8.toString()));
                    } catch (Exception e) {
                        value.value = value.value.replaceAll("\\{\\{title\\}\\}", "Link");
                    }
                }
            }
            for (ObjectDTO other: property.objectValues()) {
                postProcessLinkTitles(other);
            }
        }
    }

    @Override
    public void onPropertyStart(FeatureProperty featureProperty, List<Integer> index) throws Exception {
        currentValue = null;
        currentProperty = featureProperty;
        int sortPriority = currentFeatureProperties.indexOf(featureProperty);
        String key = featureProperty.getName().replaceAll("\\[[^\\]]*\\]", "[]");
        if (transformations.containsKey(key)) {

            Optional<FeatureProperty> htmlProperty = transformations.get(key)
                                                                    .transform(featureProperty);

            // if !isPresent, property was dropped by remove transformer
            if (htmlProperty.isPresent()) {
                currentValue = getValue(featureProperty, htmlProperty.get(), index);
                updatePropertySortPriorities(currentValue.property, sortPriority);
            } else {
                currentValue = null; // skip
            }
        } else {
            currentValue = getValue(featureProperty, featureProperty, index);
            updatePropertySortPriorities(currentValue.property, sortPriority);
        }

        //TODO: implement double col support as provider transformer and remove this
        if (featureProperty.hasDoubleColumn()) {
            if (combineCurrentPropertyValues) {
                this.combineCurrentPropertyValues = false;
                currentValueBuilder.append("|||");
            } else {
                this.combineCurrentPropertyValues = true;
            }
        }
    }

    private void updatePropertySortPriorities(PropertyDTO property, int sortPriority) {
        property.sortPriority = Math.min(property.sortPriority, sortPriority);
        if (Objects.nonNull(property.parent) &&
                Objects.nonNull(property.parent.parent) &&
                property.parent.parent instanceof PropertyDTO) {
            updatePropertySortPriorities((PropertyDTO)property.parent.parent, sortPriority);
        }
    }

    @Override
    public void onPropertyText(String text) throws Exception {
        if (Objects.nonNull(currentValue))
            currentValueBuilder.append(text);
    }

    @Override
    public void onPropertyEnd() throws Exception {
        if (combineCurrentPropertyValues) {
            return;
        }

        if (Objects.nonNull(currentValue)) {
            String value = currentValueBuilder.toString();

            if (currentProperty.isId()) {
                currentFeature.id = new PropertyDTO();
                currentFeature.id.addValue(value);
                currentFeature.id.itemProp = "url";
            }

            Optional<HtmlPropertyTransformations> valueTransformations = getTransformations(currentProperty);
            if (valueTransformations.isPresent()) {
                value = valueTransformations.get().transform(currentProperty, value);
            }

            // skip, if the value has been transformed to null
            if (Objects.nonNull(value)) {
                if (currentFeature.name != null) {
                    int pos = currentFeature.name.indexOf("{{" + currentProperty.getName() + "}}");
                    if (pos > -1) {
                        currentFeature.name = currentFeature.name.substring(0, pos) + value + currentFeature.name.substring(pos);
                    }
                }

                // special treatment for links - TODO generalize, use transformations?
                if (currentProperty.getType() == FeatureProperty.Type.STRING &&
                        currentProperty.getAdditionalInfo().containsKey("role") &&
                        Objects.nonNull(currentValue.value)) {
                    if (currentProperty.getAdditionalInfo().get("role").equalsIgnoreCase("LINKHREF")) {
                        String updatedValue = currentValue.value.replaceAll("\\{\\{href\\}\\}", value);
                        currentValue.setValue(updatedValue);
                    } else if (currentProperty.getAdditionalInfo().get("role").equalsIgnoreCase("LINKTITLE")) {
                        String updatedValue = currentValue.value.replaceAll("\\{\\{title\\}\\}", value);
                        currentValue.setValue(updatedValue);
                    }
                } else {
                    // the default
                    currentValue.setValue(value);
                }
            } else {
                currentValue.setValue(null);
            }
        }

        // reset
        currentValueBuilder.setLength(0);
        currentValue = null;
        currentProperty = null;
    }

    @Override
    public void onGeometryStart(FeatureProperty featureProperty, SimpleFeatureGeometry type, Integer dimension) throws Exception {
        if (Objects.isNull(featureProperty)) return;

        dataset.hideMap = false;

        if (!isSchemaOrgEnabled) return;

        if (transformations.containsKey(featureProperty.getName())) {

            Optional<ValueDTO> transformedProperty = transformations.get(featureProperty.getName())
                                                                              .transform(new ValueDTO(), featureProperty);

            if (!transformedProperty.isPresent()) {
                return;
            }

            // TODO ???
        }

        currentGeometryType = MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE.forGmlType(type);

        coordinatesOutput = new StringWriter();

        // for around-relations
        this.coordinatesTransformerBuilder = ImmutableCoordinatesTransformer.builder();

        coordinatesTransformerBuilder.coordinatesWriter(ImmutableCoordinatesWriterMicrodata.of(coordinatesOutput, Optional.ofNullable(dimension).orElse(2)));

        if (transformationContext.getCrsTransformer()
                                 .isPresent()) {
            coordinatesTransformerBuilder.crsTransformer(transformationContext.getCrsTransformer()
                                                                           .get());
        }

        if (dimension != null) {
            coordinatesTransformerBuilder.sourceDimension(dimension);
            coordinatesTransformerBuilder.targetDimension(dimension);
        }

        coordinatesWriter = coordinatesTransformerBuilder.build();


        currentGeometryParts = 0;
    }

    @Override
    public void onGeometryNestedStart() throws Exception {
        if (currentGeometryType == null) return;

        currentGeometryParts++;
        if (currentGeometryParts == 1) {
            // this skips additional parts in multi geometries as these are not supported by schema.org
            currentFeature.geo = new PropertyDTO();
            currentFeature.geo.itemType = "http://schema.org/GeoShape";
            currentFeature.geo.itemProp = "geo";
            currentFeature.geo.name = "geometry";

            currentGeometryPart = new PropertyDTO();
            currentFeature.geo.addChild(currentGeometryPart);

            switch (currentGeometryType) {
                case LINE_STRING:
                    currentGeometryPart.itemProp = "line";
                    break;
                case POLYGON:
                    currentGeometryPart.itemProp = "polygon";
                    break;
            }
        }
    }

    @Override
    public void onGeometryCoordinates(String text) throws Exception {
        if (currentGeometryType == null) return;

        switch (currentGeometryType) {
            case POINT:
                currentFeature.geo = new PropertyDTO();
                currentFeature.geo.itemType = "http://schema.org/GeoCoordinates";
                currentFeature.geo.itemProp = "geo";
                currentFeature.geo.name = "geometry";

                String[] coordinates = text.split(" ");
                CoordinateTuple point = new CoordinateTuple(coordinates[0], coordinates[1]);
                if (crsTransformer != null) {
                    point = crsTransformer.transform(point, false);
                }

                PropertyDTO latitude = new PropertyDTO();
                latitude.name = "latitude";
                latitude.itemProp = "latitude";
                latitude.addValue(point.getYasString());

                PropertyDTO longitude = new PropertyDTO();
                longitude.name = "longitude";
                longitude.itemProp = "longitude";
                longitude.addValue(point.getXasString());

                currentFeature.geo.addChild(latitude);
                currentFeature.geo.addChild(longitude);

                break;
            case LINE_STRING:
            case POLYGON:
                if (!currentGeometryWritten) {
                    try {
                        coordinatesWriter.append(text);
                        coordinatesWriter.close();
                        currentGeometryWritten = true;
                    } catch (IOException ex) {
                        // ignore
                    }
                }
                break;
        }
    }

    @Override
    public void onGeometryNestedEnd() throws Exception {
        if (currentGeometryType == null) return;
    }

    @Override
    public void onGeometryEnd() throws Exception {
        if (currentGeometryType == null) return;

        if (currentGeometryPart != null) {
            currentGeometryPart.addValue(coordinatesOutput.toString());
        }

        currentGeometryType = null;
        currentGeometryWritten = false;
    }

    private Optional<HtmlPropertyTransformations> getTransformations(FeatureProperty featureProperty) {
        if (featureProperty.getType() == FeatureProperty.Type.BOOLEAN) {
            return Optional.of(new ImmutableHtmlPropertyTransformations.Builder()
                                                                   .i18n(transformationContext.getI18n())
                                                                   .language(transformationContext.getLanguage())
                                                                   .build());
        }

        return Optional.ofNullable(transformations.get(featureProperty.getName().replaceAll("\\[[^\\]]*\\]", "[]")));
    }

}
