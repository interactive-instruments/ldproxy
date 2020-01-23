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
import de.ii.ldproxy.codelists.Codelist;
import de.ii.ldproxy.codelists.CodelistData;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.target.html.MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE;
import de.ii.ldproxy.target.html.MicrodataMapping.MICRODATA_TYPE;
import de.ii.ldproxy.wfs3.templates.StringTemplateFilters;
import de.ii.xtraplatform.akka.http.HttpClient;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CoordinatesWriterType;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.dropwizard.views.FallbackMustacheViewRenderer;
import de.ii.xtraplatform.feature.provider.api.FeatureTransformer;
import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author zahnen
 */
public class FeatureTransformerHtmlComplexObjects implements FeatureTransformer, OnTheFly {

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

    private PropertyContext currentPropertyContext;
    private StringBuilder currentValue = new StringBuilder();
    private Map<Integer,Object> properties;
    private Map<Integer,Object> currentObject;
    private Map<String,Integer> pathMap = new HashMap<>();
    private int nextSort;

    private class PropertyContext {
        MicrodataPropertyMapping mapping;
        List<Integer> index;
        String htmlName;
        List<String> htmlNames;
        String baseName;
        List<String> baseNames;
        int sortPriority;
        Object valueContext;
        int arrays;
        int objectLevel;

        PropertyContext(MicrodataPropertyMapping mapping, List<Integer> index) {
            this.index = index;
            htmlName = mapping.getName();
            htmlNames = Splitter.on('|').splitToList(htmlName);
            baseName = mapping.getBaseMapping().getName();
            baseNames = Splitter.on('.').splitToList(baseName);
            sortPriority = Objects.nonNull(mapping.getSortPriority()) ?
                    mapping.getSortPriority() :
                    Objects.nonNull(mapping.getBaseMapping().getSortPriority()) ?
                            mapping.getBaseMapping().getSortPriority() :
                            nextSort++;
            this.mapping = mapping;

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
                                    pv.setValue(htmlNames.get(objectLevel),"", false, objectLevel+1);
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
                                    pv.setValue("————","", false, ++objectLevel + 1);
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
                                pv.setValue(htmlNames.get(objectLevel),"", false, objectLevel + 1);
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

        void setValue(String name, String value, boolean isHtml, int level) {
            property.name = name;
            property.value = value;
            property.isHtml = isHtml;

            if (level==2)
                property.isLevel2 = true;
            else if (level==3)
                property.isLevel3 = true;

            if (currentFeature.name != null) {
                int pos = currentFeature.name.indexOf("{{" + property.name + "}}");
                if (pos > -1) {
                    currentFeature.name = currentFeature.name.substring(0, pos) + property.value + currentFeature.name.substring(pos);
                }
            }

            // TODO property.itemType = mapping.getItemType();
            // TODO property.itemProp = mapping.getItemProp();

            if (property.value.startsWith("http://") || property.value.startsWith("https://")) {
                if (property.value.toLowerCase()
                        .endsWith(".png") || property.value.toLowerCase()
                        .endsWith(".jpg") || property.value.toLowerCase()
                        .endsWith(".jpeg") || property.value.toLowerCase()
                        .endsWith(".gif")) {
                    property.isImg = true;
                } else {
                    property.isUrl = true;
                }
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
    public void onFeatureStart(TargetMapping mapping) throws Exception {
        currentFeature = new FeatureDTO();
        if (isFeatureCollection) {
            currentFeature.titleAsLink = true;
        }

        currentFeature.name = mapping.getName();
        currentFeature.itemType = ((MicrodataPropertyMapping) mapping).getItemType();
        currentFeature.itemProp = ((MicrodataPropertyMapping) mapping).getItemProp();

        // new feature, reset property contexts and property output
        currentPropertyContext = null;
        properties = new TreeMap<>();
        nextSort = 10000;
    }

    @Override
    public void onFeatureEnd() throws Exception {
        if (currentFeature.name != null)
            currentFeature.name = currentFeature.name.replaceAll("\\{\\{[^}]*\\}\\}", "");
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
    public void onPropertyStart(TargetMapping mapping, List<Integer> index) throws Exception {
        if (mapping instanceof MicrodataPropertyMapping) {
            MicrodataPropertyMapping htmlMapping = (MicrodataPropertyMapping) mapping;
            if (htmlMapping.isEnabled() && (!isFeatureCollection || htmlMapping.isShowInCollection())) {
                currentPropertyContext = new PropertyContext(htmlMapping, index);
            } else {
                currentPropertyContext = null;
            }
        }
    }

    @Override
    public void onPropertyText(String text) throws Exception {
        if (Objects.nonNull(currentPropertyContext))
            currentValue.append(text);
    }

    @Override
    public void onPropertyEnd() throws Exception {
        if (Objects.nonNull(currentPropertyContext)) {
            AtomicReference<Boolean> isHtml = new AtomicReference<>(false);
            String value = processValue(currentValue.toString(), currentPropertyContext.mapping, html -> isHtml.set(html));
            Object valueContext = currentPropertyContext.valueContext;
            if (valueContext instanceof PropertyValue) {
                PropertyValue pv = (PropertyValue) valueContext;
                pv.setValue(currentPropertyContext.getHtmlName(), value, isHtml.get(), currentPropertyContext.getLevel());
            } else {
                // TODO
                LOGGER.error("TODO");
            }
        }

        // reset
        currentValue.setLength(0);
        currentPropertyContext = null;
    }

    private String processValue(String value, MicrodataPropertyMapping mapping, Consumer<Boolean> isHtml) {
        // format the value, if this is specified in the mapping
        String result = value;

        // code list translations and similar mappings
        if (Objects.nonNull(mapping.getCodelist()) && codelists.containsKey(mapping.getCodelist())) {
            Codelist cl = codelists.get(mapping.getCodelist());
            result = cl.getValue(value);

            if (cl.getData()
                    .getSourceType() == CodelistData.IMPORT_TYPE.TEMPLATES) {
                result = StringTemplateFilters.applyFilterMarkdown(StringTemplateFilters.applyTemplate(result, value));
                isHtml.accept(true);
            }
        }

        if (mapping.getType() == MICRODATA_TYPE.ID) {
            currentFeature.id = new FeaturePropertyDTO();
            currentFeature.id.value = value;
            currentFeature.id.itemProp = "url";
            if (currentFeature.name == null || currentFeature.name.isEmpty()) {
                currentFeature.name = value;
            }
        } else if (mapping.getType() == MICRODATA_TYPE.BOOLEAN && transformationContext.getI18n().isPresent()) {
            I18n i18n = transformationContext.getI18n().get();
            Optional<Locale> language = transformationContext.getOgcApiRequest().getLanguage();
            if (value.matches("[fF](alse|ALSE)?|0"))
                result = i18n.get("false", language);
            else if (value.matches("[tT](rue|RUE)?|[\\-\\+]?1"))
                result = i18n.get("true", language);
        } else if (mapping.getType() == MICRODATA_TYPE.DATE) {
            try {
                DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd[['T'][' ']HH:mm:ss][.SSS][X]");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(mapping.getFormat());
                TemporalAccessor ta = parser.parseBest(value, OffsetDateTime::from, LocalDateTime::from, LocalDate::from);
                result = formatter.format(ta);
            } catch (Exception e) {
                //ignore
            }
        } else if (mapping.getType() == MICRODATA_TYPE.STRING && mapping.getFormat() != null && !mapping.getFormat()
                .isEmpty()) {
            boolean more = false;
            String formattedValue = StringTemplateFilters.applyTemplate(mapping.getFormat(), value, isHtml);

            result = formattedValue.replace("{{serviceUrl}}", serviceUrl);
            int subst = value.indexOf("}}");
            while (subst > -1) {
                result = result.substring(0, result.indexOf("{{")) + value + result.substring(subst + 2);
                subst = value.indexOf("}}");
            }
        }

        return result;
    }

    @Override
    public void onGeometryStart(TargetMapping mapping, SimpleFeatureGeometry type, Integer dimension) throws Exception {
        if (Objects.isNull(mapping)) return;

        dataset.hideMap = false;

        final MicrodataGeometryMapping geometryMapping = (MicrodataGeometryMapping) mapping;
        if (isFeatureCollection && !((MicrodataGeometryMapping) mapping).isShowInCollection()) return;

        currentGeometryType = geometryMapping.getGeometryType();
        if (currentGeometryType == MICRODATA_GEOMETRY_TYPE.GENERIC) {
            currentGeometryType = MICRODATA_GEOMETRY_TYPE.forGmlType(type);
        }

        coordinatesOutput = new StringWriter();
        //coordinatesWriter = new HtmlTransformingCoordinatesWriter(coordinatesOutput, Objects.nonNull(dimension) ? dimension : 2, crsTransformer);

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

}
