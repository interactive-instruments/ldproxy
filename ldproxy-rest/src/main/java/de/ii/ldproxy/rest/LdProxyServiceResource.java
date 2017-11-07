/**
 * Copyright 2017 European Union, interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.output.geojson.GeoJsonFeatureWriter;
import de.ii.ldproxy.output.geojson.GeoJsonHitsWriter;
import de.ii.ldproxy.output.geojson.Gml2GeoJsonMapper;
import de.ii.ldproxy.output.html.*;
import de.ii.ldproxy.output.jsonld.Gml2JsonLdMapper;
import de.ii.ldproxy.output.jsonld.JsonLdOutputWriter;
import de.ii.ldproxy.rest.util.RangeHeader;
import de.ii.ldproxy.rest.wfs3.GetCapabilities2Wfs3Collection;
import de.ii.ldproxy.rest.wfs3.Wfs3Collections;
import de.ii.ldproxy.service.*;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureType;
import de.ii.xsf.core.api.MediaTypeCharset;
import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.api.exceptions.ResourceNotFound;
import de.ii.xsf.core.api.permission.AuthorizationProvider;
import de.ii.xsf.core.api.rest.ServiceResource;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.Versions;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLAnalyzer;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLParser;
import de.ii.xtraplatform.ogc.api.gml.parser.MultiGMLAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.client.GetCapabilities;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSOperation;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSRequest;
import de.ii.xtraplatform.ogc.api.wfs.parser.LoggingWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.MultiWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesParser;
import de.ii.xtraplatform.util.json.JSONPOutputStream;
import de.ii.xtraplatform.util.json.JSONPStreamingOutput;
import io.dropwizard.views.View;
import io.dropwizard.views.ViewRenderer;
import io.swagger.oas.annotations.Operation;
import org.apache.http.HttpEntity;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static de.ii.ldproxy.rest.util.RangeHeader.parseRange;

/**
 * @author zahnen
 */
@Produces({MediaTypeCharset.APPLICATION_JSON_UTF8, "application/geo+json"})
public class LdProxyServiceResource implements ServiceResource {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(LdProxyServiceResource.class);
    protected LdProxyService service = null;
    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpServletRequest request;

    private OpenApiResource openApiResource;
    private ViewRenderer mustacheRenderer;

    @Override
    public Service getService() {
        return service;
    }

    @Override
    public void setService(Service service) {
        this.service = (LdProxyService) service;
        if (openApiResource != null) {
            openApiResource.setService(this.service);
        }
    }

    public void setOpenApiResource(OpenApiResource openApiResource) {
        this.openApiResource = openApiResource;
        if (service != null) {
            openApiResource.setService(service);
        }
    }

    @Override
    public void setMustacheRenderer(ViewRenderer mustacheRenderer) {
        this.mustacheRenderer = mustacheRenderer;
    }

    @Override
    public void init(AuthorizationProvider authorizationProvider) {

    }

    // TODO: as JSON-LD
    @GET
    @Produces(MediaType.TEXT_HTML)
    public View getDatasetAsHtml(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @QueryParam("token") String token) {
        DatasetView dataset = new DatasetView("service", uriInfo.getRequestUri());
        dataset.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", "../"))
                .add(new NavigationDTO(service.getName()))
                .build();

        // TODO
        //String query = request.getQueryString() == null ? "" : request.getQueryString();
        dataset.formats = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("JSON", "f=json"))
                .add(new NavigationDTO("XML", "f=xml"))
                .build();
        try {
            service.getFeatureTypes().values().stream().sorted(Comparator.comparing(WfsProxyFeatureType::getName)).forEach(ft -> {
                List<TargetMapping> mappings = ft.getMappings().findMappings(ft.getNamespace() + ":" + ft.getName(), TargetMapping.BASE_TYPE);
                LOGGER.getLogger().debug("mapping for {}:{}: {}", ft.getNamespace(), ft.getName(), mappings.size());
                if (!mappings.isEmpty() && mappings.get(0).isEnabled()) {
                    dataset.featureTypes.add(new DatasetView("service", uriInfo.getRequestUri(), ft.getName(), ft.getDisplayName()));
                }
            });
        } catch (Exception e) {
            LOGGER.getLogger().debug("Unexpected exception", e);
        }

        WFSOperation operation = new GetCapabilities();

        WFSCapabilitiesAnalyzer analyzer = new MultiWfsCapabilitiesAnalyzer(
                new GetCapabilities2Dataset(dataset),
                new LoggingWfsCapabilitiesAnalyzer()
        );

        WFSCapabilitiesParser wfsParser = new WFSCapabilitiesParser(analyzer, service.staxFactory);

        wfsParser.parse(service.getWfsAdapter().request(operation));

        dataset.title = service.getName();
        dataset.description = service.getDescription();

        // TODO
        dataset.url += "?" + operation.getGETParameters(null, new Versions()).entrySet().stream().map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue())).collect(Collectors.joining("&"));

        return dataset;
    }

    @GET
    @Produces({MediaTypeCharset.APPLICATION_JSON_UTF8, "application/xml;charset=utf-8"})
    public Wfs3Collections getDatasetAsJsonOrXml(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @QueryParam("callback") String callback) {
        Map<String, Wfs3Collections.Wfs3Collection> collections = new LinkedHashMap<>();

        service.getFeatureTypes().values().stream().sorted(Comparator.comparing(WfsProxyFeatureType::getName)).forEach(ft -> {
            String qn = service.getWfsAdapter().getNsStore().getNamespacePrefix(ft.getNamespace()) + ":" + ft.getName();
            List<TargetMapping> mappings = ft.getMappings().findMappings(ft.getNamespace() + ":" + ft.getName(), TargetMapping.BASE_TYPE);
            if (!mappings.isEmpty() && mappings.get(0).isEnabled()) {
                if (!collections.containsKey(qn)) {
                    collections.put(qn, new Wfs3Collections.Wfs3Collection());
                }
                collections.get(qn).setName(ft.getName().toLowerCase());
                collections.get(qn).setTitle(ft.getDisplayName());
            }
        });

        WFSOperation operation = new GetCapabilities();
        WFSCapabilitiesAnalyzer analyzer = new GetCapabilities2Wfs3Collection(collections);
        WFSCapabilitiesParser wfsParser = new WFSCapabilitiesParser(analyzer, service.staxFactory);
        wfsParser.parse(service.getWfsAdapter().request(operation));

        // TODO: apply local information (title, enabled, etc.)

        // TODO: ELISE compile list of dependencies with versions and licenses

        return new Wfs3Collections(collections.values());
    }

    //@GET
    //@JsonView(JsonViews.GSFSView.class)
    //@Produces("application/xml;charset=utf-8")
    //public Response getDatasetAsXml(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @QueryParam("callback") String callback) {
    //    return getWfsCapabilities();
    //}



    /*@Path("/{layerid}/")
    @GET
    //@JsonView(JsonViews.GSFSView.class)
    @Produces("application/xml;charset=utf-8")
    public Response getServiceLayerGET(@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user, @PathParam("layerid") String layerid, @QueryParam("callback") String callback) {

        return getWfsDescribeFeatureType(layerid);
    }*/

    @Path("/api")
    @Operation
    public OpenApiResource getOpenApi() {
        return openApiResource;
    }

    private FeatureCollectionView createFeatureCollectionView(WfsProxyFeatureType featureType) {
        DatasetView dataset = new DatasetView("", uriInfo.getRequestUri());
        FeatureCollectionView featureTypeDataset = new FeatureCollectionView("featureCollection", uriInfo.getRequestUri(), featureType.getName(), featureType.getDisplayName());
        dataset.featureTypes.add(featureTypeDataset);

        WFSOperation operation = new GetCapabilities();

        WFSCapabilitiesAnalyzer analyzer = new MultiWfsCapabilitiesAnalyzer(
                new GetCapabilities2Dataset(dataset),
                new LoggingWfsCapabilitiesAnalyzer()
        );

        WFSCapabilitiesParser wfsParser = new WFSCapabilitiesParser(analyzer, service.staxFactory);

        wfsParser.parse(service.getWfsAdapter().request(operation));


        String[] b = featureTypeDataset.bbox.split(" ");
        String[] min = b[0].split(",");
        String[] max = b[1].split(",");

        featureTypeDataset.bbox2 = ImmutableMap.of("minLng", min[1], "minLat", min[0], "maxLng", max[1], "maxLat", max[0]);

        Map<String, String> htmlNames = service.getHtmlNamesForFeatureType(featureType);
        Set<Map.Entry<String, String>> filterFields = service.getFilterableFieldsForFeatureType(featureType).entrySet().stream()
                .peek(entry -> {
                    if (htmlNames.containsKey(entry.getValue())) {
                        entry.setValue(htmlNames.get(entry.getValue()));
                    }
                })
                .collect(Collectors.toSet());
        featureTypeDataset.filterFields = filterFields;

        return featureTypeDataset;
    }

    private FeatureCollectionView createFeatureDetailsView(WfsProxyFeatureType featureType, String featureId) {
        FeatureCollectionView featureTypeDataset = new FeatureCollectionView("featureDetails", uriInfo.getRequestUri(), featureType.getName(), featureType.getDisplayName());
        featureTypeDataset.description = featureType.getDisplayName();

        featureTypeDataset.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", "../../../"))
                .add(new NavigationDTO(service.getName(), "../../../" + service.getBrowseUrl()))
                .add(new NavigationDTO(featureType.getDisplayName(), "../../../" + service.getBrowseUrl() + featureType.getName() + "/"))
                .add(new NavigationDTO(featureId))
                .build();

        featureTypeDataset.formats = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("GeoJson", "f=json"))
                .add(new NavigationDTO("GML", "f=xml"))
                .add(new NavigationDTO("JSON-LD", "f=jsonld"))
                .build();

        return featureTypeDataset;
    }

    private void addDatasetNavigation(FeatureCollectionView featureCollectionView, WfsProxyFeatureType featureType) {
        featureCollectionView.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", "../../"))
                .add(new NavigationDTO(service.getName(), "../../" + service.getBrowseUrl()))
                .add(new NavigationDTO(featureType.getDisplayName()))
                .build();

        // TODO: only activated formats
        featureCollectionView.formats = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("GeoJson", "f=json"))
                .add(new NavigationDTO("GML", "f=xml"))
                .add(new NavigationDTO("JSON-LD", "f=jsonld"))
                .build();


    }

    private void addDatasetIndices(FeatureCollectionView featureCollectionView, WfsProxyFeatureType featureType) {
        ImmutableList.Builder<NavigationDTO> indices = new ImmutableList.Builder<>();
        for (String index : service.findIndicesForFeatureType(featureType).keySet()) {
            indices.add(new NavigationDTO(index, "?fields=" + index + "&distinctValues=true"));
        }
        featureCollectionView.indices = indices.build();

    }

    private void createIndexPage(FeatureCollectionView featureCollectionView, WfsProxyFeatureType featureType, String fields) {
        featureCollectionView.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", "../../"))
                .add(new NavigationDTO(service.getName(), "../../" + service.getBrowseUrl()))
                .add(new NavigationDTO(featureType.getDisplayName(), "./"))
                .add(new NavigationDTO(fields))
                .build();

        featureCollectionView.formats = new ImmutableList.Builder<NavigationDTO>()
                .build();

        featureCollectionView.hideMetadata = true;
        featureCollectionView.hideMap = true;
        featureCollectionView.index = fields;
        List<String> values = service.getIndexValues(featureType, fields, service.findIndicesForFeatureType(featureType).get(fields));
        for (int i = 0; i < values.size(); i++) {
            FeatureDTO currentFeature = new FeatureDTO();
            currentFeature.name = values.get(i);
            currentFeature.id = new FeaturePropertyDTO();
            currentFeature.id.value = "?" + fields + "=" + values.get(i);
            currentFeature.noUrlClosingSlash = true;
            featureCollectionView.features.add(currentFeature);
        }
    }

    private void createFilteredByIndexPage(FeatureCollectionView featureCollectionView, WfsProxyFeatureType featureType, String filterKey, String filterValue) {
        // + TODO: same for postalCode
        // + check template url + isPartOf TODO: schema.org mapping of aggregations and links
        // + TODO: harvest all values
        // TODO: links in json-ld
        // TODO: view as for aggregation sites

        featureCollectionView.hideMetadata = true;
        featureCollectionView.index = filterKey;
        featureCollectionView.indexValue = filterValue;

        Map<String, String> announcements = null;
        if (filterKey.equals("addressLocality")) {
            announcements = service.getSparqlAdapter().request(filterValue, SparqlAdapter.QUERY.ADDRESS_LOCALITY);
        } else if (filterKey.equals("postalCode")) {
            announcements = service.getSparqlAdapter().request(filterValue, SparqlAdapter.QUERY.POSTAL_CODE);
        }

        // TODO
        if (announcements != null && !announcements.isEmpty()) {
            featureCollectionView.links = new FeaturePropertyDTO();
            featureCollectionView.links.name = "Announcements";
            for (Map.Entry<String, String> id : announcements.entrySet()) {
                FeaturePropertyDTO link = new FeaturePropertyDTO();
                link.value = id.getKey();
                link.name = makeDisplayName(id.getKey(), id.getValue());
                link.itemProp = id.getValue();
                featureCollectionView.links.addChild(link);
            }
        }
    }

    private void addFilteredByIndexNavigation(FeatureCollectionView featureCollectionView, WfsProxyFeatureType featureType, String filterKey, String filterValue) {
        if (service.findIndicesForFeatureType(featureType).containsKey(filterKey)) {
            featureCollectionView.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                    .add(new NavigationDTO("Datasets", "../../"))
                    .add(new NavigationDTO(service.getName(), "../../" + service.getBrowseUrl()))
                    .add(new NavigationDTO(featureType.getDisplayName(), "./"))
                    .add(new NavigationDTO(filterKey, "./?fields=" + filterKey + "&distinctValues=true"))
                    .add(new NavigationDTO(filterValue))
                    .build();
        } else {
            featureCollectionView.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                    .add(new NavigationDTO("Datasets", "../../"))
                    .add(new NavigationDTO(service.getName(), "../../" + service.getBrowseUrl()))
                    .add(new NavigationDTO(featureType.getDisplayName(), "./"))
                    .add(new NavigationDTO(filterKey))
                    .add(new NavigationDTO(filterValue))
                    .build();
        }
    }

    @Path("/{layerid}")
    @GET
    @Produces(MediaTypeCharset.TEXT_HTML_UTF8)
    public Response getFeaturesAsHtml(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @QueryParam("fields") String fields, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {

        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger().debug("GET HTML FOR {} {}", featureType.getNamespace(), featureType.getName());


        FeatureCollectionView featureTypeDataset = createFeatureCollectionView(featureType);

        addDatasetNavigation(featureTypeDataset, featureType);

        addDatasetIndices(featureTypeDataset, featureType);


        List<String> groupings = new ArrayList<>();
        if (layerid.equals("inspireadressen")) {
            groupings.add("woonplaats");
        }

        String query = request.getQueryString() == null ? "" : request.getQueryString();


        // TODO: split fields
        if (fields != null && service.findIndicesForFeatureType(featureType).containsKey(fields)) {

            createIndexPage(featureTypeDataset, featureType, fields);

            return Response.ok(featureTypeDataset).build();
        }

        WFSOperation wfsOperation = getWfsFeaturesPagedAndFiltered(featureType, range, ((filterKey, filterValue) -> {
            createFilteredByIndexPage(featureTypeDataset, featureType, filterKey, filterValue);
            addFilteredByIndexNavigation(featureTypeDataset, featureType, filterKey, filterValue);
        }));

        //if (indexId.toLowerCase().equals("all")) {
        return getHtmlResponse(wfsOperation, featureType, true, groupings, false, query, parseRange(range), featureTypeDataset);
        //} else {
        //    return getWfsPropertiesPaged(layerid, range, indexId);
        //}
    }

    public interface FilterConsumer {
        void with(String filterKey, String filterValue);
    }

    private WFSOperation getWfsHitsFiltered(WfsProxyFeatureType featureType) {

        Map<String, String> filterableFields = service.getFilterableFieldsForFeatureType(featureType);
        filterableFields.put("bbox", service.getGeometryPathForFeatureType(featureType));

        Map<String, String> filters = getFiltersFromQuery(uriInfo.getQueryParameters(), filterableFields);

        return getWfsHits(featureType, filters, filterableFields);

    }

    private WFSOperation getWfsFeaturesPagedAndFiltered(WfsProxyFeatureType featureType, String range) {
        return getWfsFeaturesPagedAndFiltered(featureType, range, null);
    }

    private WFSOperation getWfsFeaturesPagedAndFiltered(WfsProxyFeatureType featureType, String range, FilterConsumer indexFilterConsumer) {
        WFSOperation wfsOperation = null;

        Map<String, String> filterableFields = service.getFilterableFieldsForFeatureType(featureType);
        filterableFields.put("bbox", service.getGeometryPathForFeatureType(featureType));

        Map<String, String> indexFilterableFields = service.findIndicesForFeatureType(featureType, false);

        Map<String, String> filters = getFiltersFromQuery(uriInfo.getQueryParameters(), filterableFields, indexFilterableFields, indexFilterConsumer);

        if (!filters.isEmpty()) {
            wfsOperation = getWfsFeaturesPaged(featureType.getName(), range, filters, filterableFields);
        } else {
            wfsOperation = getWfsFeaturesPaged(featureType.getName(), range);
        }
        return wfsOperation;
    }

    private Map<String, String> getFiltersFromQuery(MultivaluedMap<String, String> query, Map<String, String> filterableFields) {
        return getFiltersFromQuery(query, filterableFields, null, null);
    }

    private Map<String, String> getFiltersFromQuery(MultivaluedMap<String, String> query, Map<String, String> filterableFields, Map<String, String> indexFilterableFields, FilterConsumer indexFilterConsumer) {

        Map<String, String> filters = new LinkedHashMap<>();

        for (String filterKey : query.keySet()) {
            if (filterableFields.containsKey(filterKey.toLowerCase())) {
                String filterValue = query.getFirst(filterKey);

                filters.put(filterKey.toLowerCase(), filterValue);
            } else if (indexFilterableFields != null && indexFilterableFields.containsKey(filterKey)) {

                String filterValue = query.getFirst(filterKey);

                if (indexFilterConsumer != null) {
                    indexFilterConsumer.with(filterKey, filterValue);
                }

                String indexId = indexFilterableFields.get(filterKey);
                indexId = indexId.substring(indexId.lastIndexOf(':') + 1);

                filters.put(indexId, filterValue);
            }
        }

        return filters;
    }

    private String makeDisplayName(String id, String title) {
        if (title.length() > 100) {
            title = title.substring(0, 99) + " ...";
        }
        return title + " (" + id.substring(id.lastIndexOf('/') + 1) + ")";
    }

    @Path("/{layerid}")
    @GET
    public Response getFeaturesAsJson(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @QueryParam("callback") String callback, @QueryParam("resultType") String resultType, @HeaderParam("Range") String range) {

        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger().debug("GET JSON FOR {} {}", featureType.getNamespace(), featureType.getName());

        if (resultType != null && resultType.equals("hits")) {
            return getJsonHits(getWfsHitsFiltered(featureType), featureType);
        }

        //if (indexId.toLowerCase().equals("all")) {
        return getJsonResponse(getWfsFeaturesPagedAndFiltered(featureType, range), featureType, true, range, callback);
        //} else {
        //    return getWfsPropertiesPaged(layerid, range, indexId);
        //}
    }

    @Path("/{layerid}")
    @GET
    @Produces("application/ld+json;charset=utf-8")
    public Response getFeaturesAsJsonLd(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {

        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger().debug("GET JSON-LD FOR {} {}", featureType.getNamespace(), featureType.getName());

        FeatureCollectionView featureTypeDataset = createFeatureCollectionView(featureType);

        WFSOperation wfsOperation = getWfsFeaturesPagedAndFiltered(featureType, range, ((filterKey, filterValue) -> createFilteredByIndexPage(featureTypeDataset, featureType, filterKey, filterValue)));

        //if (indexId.toLowerCase().equals("all")) {
        return getJsonLdResponse(wfsOperation, featureType, true, featureTypeDataset);
        //} else {
        //    return getWfsPropertiesPaged(layerid, range, indexId);
        //}
    }

    @Path("/{layerid}")
    @GET
    @Produces("application/xml;charset=utf-8")
    public Response getFeaturesAsXml(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @QueryParam("properties") String fields, @QueryParam("callback") String callback, @QueryParam("resultType") String resultType, @HeaderParam("Range") String range) {

        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger().debug("GET XML FOR {} {}", featureType.getNamespace(), featureType.getName());

        if (resultType != null && resultType.equals("hits")) {
            return getWfsResponse(getWfsHitsFiltered(featureType));
        }

        //if (indexId.toLowerCase().equals("all")) {
        return getWfsResponse(getWfsFeaturesPagedAndFiltered(featureType, range));
        //} else {
        //    return getWfsPropertiesPaged(layerid, range, indexId);
        //}
    }

    @Path("/{layerid}/{featureid}")
    @GET
    public Response getFeatureByIdAsJson(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @PathParam("featureid") final String featureid, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {

        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger().debug("GET JSON FOR {} {}", featureType.getNamespace(), featureType.getName());

        //if (indexId.toLowerCase().equals("all")) {
        return getJsonResponse(getWfsFeatureById(layerid, featureid), featureType, false);
        //} else {
        //    return getWfsFeaturesPaged(layerid, range, indexId, featureid);
        //}
    }

    @Path("/{layerid}/{featureid}")
    @GET
    @Produces("application/ld+json;charset=utf-8")
    public Response getFeatureByIdAsJsonLd(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @PathParam("featureid") final String featureid, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {

        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger().debug("GET JSON-LD FOR {} {}", featureType.getNamespace(), featureType.getName());

        //if (indexId.toLowerCase().equals("all")) {
        return getJsonLdResponse(getWfsFeatureById(layerid, featureid), featureType, false, null);
        //} else {
        //    return getWfsFeaturesPaged(layerid, range, indexId, featureid);
        //}
    }

    @Path("/{layerid}/{featureid}")
    @GET
    @Produces("application/xml;charset=utf-8")
    public Response getFeatureByIdAsAXml(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @PathParam("featureid") final String featureid, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {

        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger().debug("GET XML FOR {} {}", featureType.getNamespace(), featureType.getName());

        //if (indexId.toLowerCase().equals("all")) {
        return getWfsResponse(getWfsFeatureById(layerid, featureid));
        //} else {
        //    return getWfsFeaturesPaged(layerid, range, indexId, featureid);
        //}
    }

    @Path("/{layerid}/{featureid}")
    @GET
    @Produces(MediaTypeCharset.TEXT_HTML_UTF8)
    public Response getFeatureByIdAsHtml(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @PathParam("featureid") final String featureid, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {
        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger().debug("GET HTML FOR {} {}", featureType.getNamespace(), featureType.getName());

        FeatureCollectionView featureTypeDataset = createFeatureDetailsView(featureType, featureid);

        //if (indexId.toLowerCase().equals("all")) {
        return getHtmlResponse(getWfsFeatureById(layerid, featureid), featureType, false, new ArrayList<>(), false, "", null, featureTypeDataset);
        //} else {
        //    return getWfsFeaturesPaged(layerid, range, indexId, featureid);
        //}
    }

    /*@Path("/{layerid}/{indexId}/{featureid}/{featureid2}")
    @GET
    @Produces("application/xml;charset=utf-8")
    public Response getServiceLayerFeature2GET(@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user, @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @PathParam("featureid") final String featureid, @PathParam("featureid2") final String featureid2, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {

        // TODO: filter, error handling
            return getWfsResponse(getWfsFeatureById(layerid, featureid2));
    }*/


    private WFSOperation getWfsHits(WfsProxyFeatureType featureType, Map<String, String> filterValues, Map<String, String> filterPaths) {
        return new GetFeatureHits(featureType.getNamespace(), featureType.getName(), filterValues, filterPaths);
    }

    private WFSOperation getWfsFeaturesPaged(String layerId, String range, Map<String, String> filterValues, Map<String, String> filterPaths) {
        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerId);
        int[] countFrom = parseRange(range);
        int count = countFrom[0];
        int startIndex = countFrom[1];

        return new GetFeaturePaging(featureType.getNamespace(), featureType.getName(), count, startIndex, filterValues, filterPaths);
    }

    private WFSOperation getWfsFeaturesPaged(String layerId, String range) {
        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerId);
        int[] countFrom = parseRange(range);
        int count = countFrom[0];
        int startIndex = countFrom[1];

        return new GetFeaturePaging(featureType.getNamespace(), featureType.getName(), count, startIndex);
    }

    private WFSOperation getWfsFeatureById(String layerId, String featureId) {
        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerId);
        return new GetFeatureById(featureType.getNamespace(), featureType.getName(), featureId);
    }

    /*private Response getWfsDescribeFeatureType(String layerId) {
        String[] ft = parseLayerId(layerId);

        Map<String, List<String>> fts = new ImmutableMap.Builder<String, List<String>>().put(ft[0], Lists.newArrayList(ft[1])).build();
        LOGGER.getLogger().debug("GET LAYER {}", fts);
        WFSOperation operation = new DescribeFeatureType(fts);

        return getWfsResponse(operation);
    }*/

    private Response getWfsCapabilities() {
        WFSOperation operation = new GetCapabilities();

        return getWfsResponse(operation);
    }

    /*private WFSOperation getWfsPropertiesPaged(String layerId, String range, String indexId) {
        String[] ft = parseLayerId(layerId);
        int[] countFrom = parseRange(range);
        int count = countFrom[0];
        int startIndex = countFrom[1];

        return new GetPropertyValuePaging(ft[0], ft[1], indexId, count, startIndex);
    }*/


    public interface GmlAnalyzerBuilder {
        GMLAnalyzer with(OutputStream outputStream) throws IOException;
    }

    public interface WFSRequestConsumer {
        void with(WFSRequest request);
    }

    private Response getJsonResponse(final WFSOperation operation, final WfsProxyFeatureType featureType, final boolean isFeatureCollection) {
        return getJsonResponse(operation, featureType, isFeatureCollection, null, null);
    }

    private Response getJsonResponse(final WFSOperation operation, final WfsProxyFeatureType featureType, final boolean isFeatureCollection, String range, String callback) {

        return getResponse(operation, featureType,
                outputStream -> new GeoJsonFeatureWriter(service.createJsonGenerator(outputStream), service.jsonMapper, isFeatureCollection, featureType.getMappings(), Gml2GeoJsonMapper.MIME_TYPE, service.getCrsTransformations().getDefaultTransformer()), null, range, callback);
    }

    private Response getJsonHits(final WFSOperation operation, final WfsProxyFeatureType featureType) {

        return getResponse(operation, featureType,
                outputStream -> new GeoJsonHitsWriter(service.createJsonGenerator(outputStream), service.jsonMapper, true, service.getCrsTransformations().getDefaultTransformer()));
    }

    private Response getJsonLdResponse(final WFSOperation operation, final WfsProxyFeatureType featureType, final boolean isFeatureCollection, final FeatureCollectionView dataset) {

        return getResponse(operation, featureType,
                outputStream -> new JsonLdOutputWriter(service.createJsonGenerator(outputStream), service.jsonMapper, isFeatureCollection, featureType.getMappings(), Gml2JsonLdMapper.MIME_TYPE, service.getCrsTransformations().getDefaultTransformer(), uriInfo.getRequestUri(), dataset, service.getRewrites(), service.getVocab()));
    }

    private Response getHtmlResponse(final WFSOperation operation, final WfsProxyFeatureType featureType, final boolean isFeatureCollection, final List<String> groupings, final boolean group, final String query, final int[] range, final FeatureCollectionView featureTypeDataset) {

        return getResponse(operation, featureType,
                outputStream -> new MicrodataFeatureWriter(new OutputStreamWriter(outputStream), featureType.getMappings(), Gml2MicrodataMapper.MIME_TYPE, isFeatureCollection, featureType.getName().equals("inspireadressen"), groupings, group, query, range, featureTypeDataset, service.getCrsTransformations().getDefaultTransformer(), service.getSparqlAdapter(), service.getCodelistStore(), mustacheRenderer),
                wfsRequest -> {
                    if (featureTypeDataset != null) featureTypeDataset.requestUrl = wfsRequest.getAsUrl();
                }
        );
    }

    private Response getResponse(final WFSOperation operation, final WfsProxyFeatureType featureType, final GmlAnalyzerBuilder gmlAnalyzer) {
        return getResponse(operation, featureType, gmlAnalyzer, null);
    }

    private Response getResponse(final WFSOperation operation, final WfsProxyFeatureType featureType, final GmlAnalyzerBuilder gmlAnalyzer, final WFSRequestConsumer wfsRequestConsumer) {
        return getResponse(operation, featureType, gmlAnalyzer, wfsRequestConsumer, null, null);
    }

    private Response getResponse(final WFSOperation operation, final WfsProxyFeatureType featureType, final GmlAnalyzerBuilder gmlAnalyzer, final WFSRequestConsumer wfsRequestConsumer, final String range, final String callback) {
        Response.ResponseBuilder response = Response.ok();

        StreamingOutput stream;
        if (callback != null) {
            stream = new JSONPStreamingOutput(callback) {
                @Override
                public void writeCallback(JSONPOutputStream os) throws IOException, WebApplicationException {
                    WFSRequest request = new WFSRequest(service.getWfsAdapter(), operation);

                    if (wfsRequestConsumer != null) {
                        wfsRequestConsumer.with(request);
                    }

                    try {
                        GMLAnalyzer analyzer;
                        //if (range != null) {
                        //    analyzer = new MultiGMLAnalyzer(gmlAnalyzer.with(output), new RangeHeader.RangeWriter(response, range));
                        //} else {
                        analyzer = gmlAnalyzer.with(os);
                        //}

                        GMLParser gmlParser = new GMLParser(analyzer, service.staxFactory);
                        if (featureType.getMappings().getMappings().isEmpty()) {
                            gmlParser.enableTextParsing();
                        }
                        gmlParser.parse(request.getResponse(), featureType.getNamespace(), featureType.getName());

                    } catch (ExecutionException ex) {
                        // ignore
                    }
                }
            };
        } else {
            //TODO: start parsing before this line, in StreamingOutput call Analyzer.startWrite(output)
            stream = output -> {

                WFSRequest request = new WFSRequest(service.getWfsAdapter(), operation);

                if (wfsRequestConsumer != null) {
                    wfsRequestConsumer.with(request);
                }

                try {
                    GMLAnalyzer analyzer;
                    //if (range != null) {
                    //    analyzer = new MultiGMLAnalyzer(gmlAnalyzer.with(output), new RangeHeader.RangeWriter(response, range));
                    //} else {
                    analyzer = gmlAnalyzer.with(output);
                    //}

                    GMLParser gmlParser = new GMLParser(analyzer, service.staxFactory);
                    if (featureType.getMappings().getMappings().isEmpty()) {
                        gmlParser.enableTextParsing();
                    }
                    gmlParser.parse(request.getResponse(), featureType.getNamespace(), featureType.getName());

                } catch (ExecutionException ex) {
                    // ignore
                }
            };
        }

        return response.entity(stream).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET").build();
    }

    private Response getWfsResponse(final WFSOperation operation) {
        StreamingOutput stream = output -> {
            HttpEntity res = service.getWfsAdapter().request(operation);
            res.writeTo(output);
        };

        return Response.ok().entity(stream).build();
    }

    private WfsProxyFeatureType getFeatureTypeForLayerId(String id) {
        Optional<WfsProxyFeatureType> featureType = service.getFeatureTypeByName(id);

        if (!featureType.isPresent() || !featureType.get().isEnabled()) {
            throw new ResourceNotFound();
        }

        return featureType.get();
    }
}
