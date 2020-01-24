/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.codelists.Codelist;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformations;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.target.html.MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE;
import de.ii.xtraplatform.akka.http.HttpClient;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CoordinatesWriterType;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.dropwizard.views.FallbackMustacheViewRenderer;
import de.ii.xtraplatform.feature.provider.api.FeatureProperty;
import de.ii.xtraplatform.feature.provider.api.FeatureTransformer2;
import de.ii.xtraplatform.feature.provider.api.FeatureType;
import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.transformer.api.OnTheFly;
import de.ii.xtraplatform.feature.transformer.api.OnTheFlyMapping;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import io.dropwizard.views.ViewRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

/**
 * @author zahnen
 */
public class FeatureTransformerHtmlComplexObjects implements FeatureTransformer2, OnTheFly {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerHtmlComplexObjects.class);

    private OutputStreamWriter outputStreamWriter;
    protected XMLPathTracker currentPath;
    protected FeatureDTO currentFeature;
    protected boolean isFeatureCollection;
    protected ViewRenderer mustacheRenderer;
    protected int page;
    protected int pageSize;
    protected CrsTransformer crsTransformer;
    protected Map<String, Codelist> codelists;

    public FeatureCollectionView dataset;

    private String serviceUrl;

    MICRODATA_GEOMETRY_TYPE currentGeometryType;
    CoordinatesWriterType.Builder cwBuilder;
    private Writer coordinatesWriter;
    private Writer coordinatesOutput;
    private FeaturePropertyDTO currentGeometryPart;
    private int currentGeometryParts;
    private boolean currentGeometryWritten;

    private final FeatureTransformationContextHtml transformationContext;
    private final int offset;
    private final HtmlConfiguration htmlConfiguration;
    private final Map<String, HtmlPropertyTransformations> transformations;
    private final boolean isMicrodataEnabled;

    private PropertyContext currentPropertyContext;
    private StringBuilder currentValue = new StringBuilder();
    private Map<Integer,Object> properties;
    private Map<Integer,Object> currentObject;
    private Map<String,Integer> pathMap = new HashMap<>();
    private int nextSort;

    private class PropertyContext {
        FeatureProperty featureProperty;
        List<Integer> index;
        String htmlName;
        List<String> htmlNames;
        String baseName;
        List<String> baseNames;
        int sortPriority;
        Object valueContext;
        int arrays;
        int objectLevel;

        PropertyContext(FeatureProperty baseProperty, FeatureProperty htmlProperty, List<Integer> index) {
            this.index = index;
            htmlName = htmlProperty.getName();
            htmlNames = Splitter.on('|').splitToList(htmlName);
            baseName = baseProperty.getName();
            baseNames = Splitter.on('.').splitToList(baseName.replaceAll("\\[.+?\\]", ""));
            sortPriority = nextSort++;
            this.featureProperty = baseProperty;

            // determine context in properties
            String curPath = null;
            arrays = 0;
            objectLevel = 0;
            valueContext = properties;
            for (String name : baseNames) {
                curPath = Objects.isNull(curPath) ? name : curPath.concat("."+name);
                if (!pathMap.containsKey(curPath)) {
                    // new path or new property: register the current sort priority for it
                    pathMap.put(curPath, sortPriority);
                }
                int sort = pathMap.get(curPath);
                if (valueContext instanceof Map) {
                    Object newContext = ((Map<Integer, Object>) valueContext).get(sort);
                    if (name.endsWith("]")) {
                        // we have an array and the valueContext must be a list with valueContextIdx the index in the list
                        if (Objects.isNull(newContext)) {
                            if (index.get(arrays++) == 1) {
                                List list = new ArrayList<Object>();
                                ((Map<Integer,Object>)valueContext).put(sort, list);
                                if (curPath.equals(baseName)) {
                                    // we are at the end, so it must be a single value
                                    newContext = new PropertyValue();
                                } else {
                                    // ... otherwise we have an object
                                    newContext = new TreeMap<Integer,Object>();
                                    PropertyValue pv = new PropertyValue();
                                    pv.setNameAndLevel(htmlNames.get(objectLevel), objectLevel+1);
                                    objectLevel++;
                                    ((Map<Integer,Object>)newContext).put(Integer.MIN_VALUE, pv);
                                }
                                list.add(newContext);
                                valueContext = newContext;
                            } else {
                                // TODO error
                                LOGGER.error("TODO");
                            }
                        } else if (newContext instanceof List) {
                            List list = (List)newContext;
                            int curCount = list.size();
                            int idx = index.get(arrays++);
                            if (curCount==idx) {
                                valueContext = list.get(idx-1);
                                objectLevel++;
                            } else if (curCount==idx-1) {
                                if (curPath.equals(baseName)) {
                                    // we are at the end, so it must be a single value
                                    newContext = new PropertyValue();
                                } else {
                                    // ... otherwise we have an object
                                    newContext = new TreeMap<Integer,Object>();
                                    PropertyValue pv = new PropertyValue();
                                    pv.setNameAndLevel("————", ++objectLevel + 1);
                                    pv.property.isObjectSeparator = true;
                                    ((Map<Integer,Object>)newContext).put(Integer.MIN_VALUE, pv);
                                }
                                list.add(newContext);
                                valueContext = newContext;
                            } else {
                                // TODO error
                                LOGGER.error("TODO");
                            }
                        } else {
                            // TODO we have an error
                            LOGGER.error("TODO");
                        }
                    } else {
                        // we have a single value
                        if (Objects.isNull(newContext)) {
                            if (curPath.equals(baseName)) {
                                // we are at the end, so it must be a single value
                                newContext = new PropertyValue();
                            } else {
                                // ... otherwise we have an object
                                newContext = new TreeMap<Integer,Object>();
                                PropertyValue pv = new PropertyValue();
                                pv.setNameAndLevel(htmlNames.get(objectLevel), objectLevel + 1);
                                objectLevel++;
                                ((Map<Integer,Object>)newContext).put(Integer.MIN_VALUE, pv);
                            }
                            ((Map<Integer, Object>) valueContext).put(sort,newContext);
                            valueContext = newContext;
                        } else if (newContext instanceof Map || newContext instanceof PropertyValue) {
                            valueContext = newContext;
                            if (newContext instanceof Map)
                                objectLevel++;
                        } else {
                            // TODO we have an error
                            LOGGER.error("TODO");
                        }
                    }
                } else {
                    // TODO we have an error
                    LOGGER.error("TODO");
                }
            }
        }

        String getHtmlName() {
            int idx = this.htmlNames.size()-1;
            return this.htmlNames.get(idx);
        }

        int getLevel() {
            return this.objectLevel+1;
        }
    }

    private class PropertyValue {
        FeaturePropertyDTO property;

        PropertyValue() {
            property = new FeaturePropertyDTO();
        }

        //TODO: is this.property accessed/mutated before setProperty call? if not, just set property = featurePropertyDTO
        void setProperty(FeaturePropertyDTO featurePropertyDTO) {
            property.name = featurePropertyDTO.name;
            property.value = featurePropertyDTO.value;
            property.isHtml = featurePropertyDTO.isHtml;
            property.isImg = featurePropertyDTO.isImg;
            property.isUrl = featurePropertyDTO.isUrl;
            property.isLevel2 = featurePropertyDTO.isLevel2;
            property.isLevel3 = featurePropertyDTO.isLevel3;
        }

        void setNameAndLevel(String name, int level) {
            property.name = name;

            if (level==2) {
                property.isLevel2 = true;
            }
            else if (level==3) {
                property.isLevel3 = true;
            }
        }

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

        FeatureTypeConfigurationOgcApi featureTypeConfiguration = transformationContext.getApiData()
                                                                                       .getCollections()
                                                                                       .get(transformationContext.getCollectionId());

        Optional<FeatureTransformations> baseTransformations = featureTypeConfiguration
                .getExtension(OgcApiFeaturesCoreConfiguration.class)
                .map(coreConfiguration -> coreConfiguration);

        this.transformations = featureTypeConfiguration
                .getExtension(HtmlConfiguration.class)
                .map(htmlConfiguration -> htmlConfiguration.getTransformations(baseTransformations, transformationContext.getCodelists(), transformationContext.getServiceUrl(), isFeatureCollection))
                .orElse(ImmutableMap.of());

        this.isMicrodataEnabled = transformationContext.getHtmlConfiguration()
                                                       .getMicrodataEnabled();
    }

    @Override
    public String getTargetFormat() {
        return Gml2MicrodataMappingProvider.MIME_TYPE;
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
            //analyzeFailed(ex);
            LOGGER.error("Pagination not supported by feature provider");
        }
    }

    @Override
    public void onEnd() throws Exception {

        ((FallbackMustacheViewRenderer) mustacheRenderer).render(dataset, outputStreamWriter);
        outputStreamWriter.flush();
    }

    @Override
    public void onFeatureStart(FeatureType featureType) throws Exception {
        currentFeature = new FeatureDTO();

        if (isFeatureCollection) {
            currentFeature.titleAsLink = true;
        }

        Optional<String> itemLabelFormat = htmlConfiguration.getItemLabelFormat();
        if (itemLabelFormat.isPresent()) {
            currentFeature.name = itemLabelFormat.get();
        }

        if (isMicrodataEnabled) {
            currentFeature.itemType = "http://schema.org/Place";
        }

        // new feature, reset property contexts and property output
        currentPropertyContext = null;
        properties = new TreeMap<>();
        nextSort = 10000;
    }

    @Override
    public void onFeatureEnd() throws Exception {
        if (currentFeature.name != null) {
            currentFeature.name = currentFeature.name.replaceAll("\\{\\{[^}]*\\}\\}", "");
        } else {
            currentFeature.name = currentFeature.id.value;
        }

        if (!isFeatureCollection) {
            this.dataset.title = currentFeature.name;
            this.dataset.breadCrumbs.get(dataset.breadCrumbs.size() - 1).label = currentFeature.name;
        }

        addChildren(properties);
        dataset.features.add(currentFeature);
        currentFeature = null;
    }

    private void addChildren(Object value) {
        if (value instanceof PropertyValue) {
            FeaturePropertyDTO pv = ((PropertyValue) value).property;
            currentFeature.addChild(pv);
        } else if (value instanceof List) {
            boolean showName = true;
            for (Object item : (List)value) {
                addChildren(item);
                showName = false;
            }
        } else if (value instanceof Map) {
            /* TODO special treatment for link objects
            Map<Integer, Object> obj = (Map<Integer, Object>) value;
            Collection<Object> values = obj.values();
            values.stream()
                    .filter(val -> val instanceof PropertyValue)
                    .map(val -> ((PropertyValue) val).property.)
            if ()
             */

            for (Object item : ((Map<Integer,Object>)value).values()) {
                addChildren(item);
            }
        }
    }

    @Override
    public void onPropertyStart(FeatureProperty featureProperty, List<Integer> index) throws Exception {
        String key = featureProperty.getName().replaceAll("\\[.+?\\]", "[]");
        if (transformations.containsKey(key)) {
            Optional<FeatureProperty> htmlProperty = transformations.get(key)
                                                                    .transform(featureProperty);

            // if !isPresent, property was dropped by remove transformer
            if (htmlProperty.isPresent()) {
                currentPropertyContext = new PropertyContext(featureProperty, htmlProperty.get(), index);
            } else {
                currentPropertyContext = null;
            }
        } else {
            currentPropertyContext = new PropertyContext(featureProperty, featureProperty, index);
        }
    }

    @Override
    public void onPropertyText(String text) throws Exception {
        if (Objects.nonNull(currentPropertyContext)) {
            currentValue.append(text);
        }
    }

    @Override
    public void onPropertyEnd() throws Exception {
        if (Objects.nonNull(currentPropertyContext)) {
            FeaturePropertyDTO featurePropertyDTO = processProperty(currentValue.toString(), currentPropertyContext.featureProperty, currentPropertyContext.getHtmlName(), currentPropertyContext.getLevel());
            Object valueContext = currentPropertyContext.valueContext;

            if (valueContext instanceof PropertyValue) {
                PropertyValue pv = (PropertyValue) valueContext;
                pv.setProperty(featurePropertyDTO);
            } else {
                // TODO
                LOGGER.error("TODO");
            }
        }

        // reset
        currentValue.setLength(0);
        currentPropertyContext = null;
    }

    private FeaturePropertyDTO processProperty(String value, FeatureProperty featureProperty,
                                               String htmlName, int level) {
        if (featureProperty.isId()) {
            currentFeature.id = new FeaturePropertyDTO();
            currentFeature.id.value = currentValue.toString();
            currentFeature.id.itemProp = "url";
        }

        FeaturePropertyDTO property = new FeaturePropertyDTO();
        property.name = htmlName;
        property.value = value;

        if (level==2) {
            property.isLevel2 = true;
        }
        else if (level==3) {
            property.isLevel3 = true;
        }

        if (currentFeature.name != null) {
            int pos = currentFeature.name.indexOf("{{" + featureProperty.getName() + "}}");
            if (pos > -1) {
                currentFeature.name = currentFeature.name.substring(0, pos) + property.value + currentFeature.name.substring(pos);
            }
        }
        Optional<HtmlPropertyTransformations> valueTransformations = getTransformations(featureProperty);
        if (valueTransformations.isPresent()) {
            property.value = valueTransformations.get().transform(featureProperty, value);
        }

        return property;
    }

    @Override
    public void onGeometryStart(FeatureProperty featureProperty, SimpleFeatureGeometry type, Integer dimension) throws Exception {
        if (Objects.isNull(featureProperty)) return;

        dataset.hideMap = false;

        if (!isMicrodataEnabled) return;

        if (transformations.containsKey(featureProperty.getName())) {

            Optional<FeaturePropertyDTO> transformedProperty = transformations.get(featureProperty.getName())
                                                                              .transform(new FeaturePropertyDTO(), featureProperty);

            if (!transformedProperty.isPresent()) {
                return;
            }
        }

        currentGeometryType = MICRODATA_GEOMETRY_TYPE.forGmlType(type);

        coordinatesOutput = new StringWriter();

        // for around-relations
        this.cwBuilder = CoordinatesWriterType.builder();

        cwBuilder.format(new MicrodataCoordinatesFormatter(coordinatesOutput));

        if (transformationContext.getCrsTransformer()
                                 .isPresent()) {
            cwBuilder.transformer(transformationContext.getCrsTransformer()
                                                       .get());
        }

        if (dimension != null) {
            cwBuilder.dimension(dimension);
        }

        coordinatesWriter = cwBuilder.build();


        currentGeometryParts = 0;
    }

    @Override
    public void onGeometryNestedStart() throws Exception {
        if (currentGeometryType == null) return;

        currentGeometryParts++;
        if (currentGeometryParts == 1) {
            currentFeature.geo = new FeaturePropertyDTO();
            currentFeature.geo.itemType = "http://schema.org/GeoShape";
            currentFeature.geo.itemProp = "geo";
            currentFeature.geo.name = "geometry";

            currentGeometryPart = new FeaturePropertyDTO();
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
                currentFeature.geo = new FeaturePropertyDTO();
                currentFeature.geo.itemType = "http://schema.org/GeoCoordinates";
                currentFeature.geo.itemProp = "geo";
                currentFeature.geo.name = "geometry";

                String[] coordinates = text.split(" ");
                CoordinateTuple point = new CoordinateTuple(coordinates[0], coordinates[1]);
                if (crsTransformer != null) {
                    point = crsTransformer.transform(point, false);
                }

                FeaturePropertyDTO longitude = new FeaturePropertyDTO();
                longitude.name = "longitude";
                longitude.itemProp = "longitude";
                longitude.value = point.getXasString();

                FeaturePropertyDTO latitude = new FeaturePropertyDTO();
                latitude.name = "latitude";
                latitude.itemProp = "latitude";
                latitude.value = point.getYasString();

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
            currentGeometryPart.value = coordinatesOutput.toString();
        }

        currentGeometryType = null;
        currentGeometryWritten = false;
    }

    @Override
    public OnTheFlyMapping getOnTheFlyMapping() {
        return new OnTheFlyMappingHtml();
    }

    private Optional<HtmlPropertyTransformations> getTransformations(FeatureProperty featureProperty) {
        if (featureProperty.getType() == FeatureProperty.Type.BOOLEAN) {
            return Optional.of(ImmutableHtmlPropertyTransformations.builder()
                                                                   .i18n(transformationContext.getI18n())
                                                                   .language(transformationContext.getLanguage())
                                                                   .build());
        }

        return Optional.ofNullable(transformations.get(featureProperty.getName().replaceAll("\\[.+?\\]", "[]")));
    }

}
