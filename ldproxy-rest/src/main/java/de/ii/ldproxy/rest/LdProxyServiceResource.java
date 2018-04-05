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
import de.ii.ldproxy.output.geojson.Gml2GeoJsonMappingProvider;
import de.ii.ldproxy.output.html.*;
import de.ii.ldproxy.output.jsonld.Gml2JsonLdMappingProvider;
import de.ii.ldproxy.output.jsonld.JsonLdOutputWriter;
import de.ii.ldproxy.rest.util.RangeHeader;
import de.ii.ldproxy.rest.wfs3.*;
import de.ii.ldproxy.service.*;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureType;
import de.ii.xsf.core.api.MediaTypeCharset;
import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.api.exceptions.ResourceNotFound;
import de.ii.xsf.core.api.permission.AuthorizationProvider;
import de.ii.xsf.core.api.rest.ServiceResource;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLAnalyzer;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLParser;
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
import java.net.URISyntaxException;
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
    final private Wfs3LinksGenerator wfs3LinksGenerator;

    public LdProxyServiceResource() {
        this.wfs3LinksGenerator = new Wfs3LinksGenerator();
    }

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

    @GET
    @Produces(MediaType.TEXT_HTML)
    public View getDatasetAsHtml1() throws URISyntaxException {
        return getDatasetAsHtml(false);
    }

    @Path("/collections")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public View getDatasetAsHtml2() throws URISyntaxException {
        return getDatasetAsHtml(true);
    }

    public View getDatasetAsHtml(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,@QueryParam("token") String token,*/ boolean isCollection) throws URISyntaxException {
        final Wfs3Dataset wfs3Dataset = generateWfs3Dataset(Wfs3MediaTypes.HTML, Wfs3MediaTypes.JSON/*, Wfs3MediaTypes.XML*/);

        wfs3Dataset.getWfsCapabilities().title = service.getName();
        wfs3Dataset.getWfsCapabilities().description = service.getDescription();
        // TODO
        wfs3Dataset.getWfsCapabilities().url += "?SERVICE=WFS&REQUEST=GetCapabilities";

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", new URICustomizer(uriInfo.getRequestUri()).removeLastPathSegments(isCollection ? 2 : 1).toString()))
                .add(new NavigationDTO(service.getName()))
                .build();

        return new Wfs3DatasetView(wfs3Dataset, breadCrumbs);
/*
        //String pathSuffix = isCollection ? "../" : "";
        URIBuilder baseUriBuilder = new URIBuilder(uriInfo.getRequestUri());
        String path = baseUriBuilder.getPath();
        if (!path.endsWith("/")) {
            path += "/";
        }
        if (!isCollection) {
            path += "collections";
        }
        baseUriBuilder.setPath(path);
        URI baseUri = baseUriBuilder.build();

        DatasetView dataset = new DatasetView("service", baseUri);
        dataset.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", "../../"))
                .add(new NavigationDTO(service.getName()))
                .build();

        // TODO
        //String query = request.getQueryString() == null ? "" : request.getQueryString();
        dataset.formats = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("JSON", "f=json"))
                //.add(new NavigationDTO("XML", "f=xml"))
                .build();
        try {
            service.getFeatureTypes()
                   .values()
                   .stream()
                   .sorted(Comparator.comparing(WfsProxyFeatureType::getName))
                   .forEach(ft -> {
                       List<TargetMapping> mappings = ft.getMappings()
                                                        .findMappings(ft.getNamespace() + ":" + ft.getName(), TargetMapping.BASE_TYPE);
                       LOGGER.getLogger()
                             .debug("mapping for {}:{}: {}", ft.getNamespace(), ft.getName(), mappings.size());
                       if (!service.getServiceProperties()
                                   .getMappingStatus()
                                   .isEnabled() || (!mappings.isEmpty() && mappings.get(0)
                                                                                   .isEnabled())) {
                           dataset.featureTypes.add(new DatasetView("service", baseUri, ft.getName(), ft.getDisplayName()));
                       }
                   });
        } catch (Exception e) {
            LOGGER.getLogger()
                  .debug("Unexpected exception", e);
        }

        WFSOperation operation = new GetCapabilities();

        WFSCapabilitiesAnalyzer analyzer = new MultiWfsCapabilitiesAnalyzer(
                new GetCapabilities2Dataset(dataset),
                new LoggingWfsCapabilitiesAnalyzer()
        );

        WFSCapabilitiesParser wfsParser = new WFSCapabilitiesParser(analyzer, service.staxFactory);

        wfsParser.parse(service.getWfsAdapter()
                               .request(operation));

        dataset.title = service.getName();
        dataset.description = service.getDescription();

        // TODO
        dataset.url += "?" + operation.getGETParameters(null, new Versions())
                                      .entrySet()
                                      .stream()
                                      .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                                      .collect(Collectors.joining("&"));

        return dataset;*/
    }

    @GET
    @Produces({MediaTypeCharset.APPLICATION_JSON_UTF8})
    public Response getDatasetAsJson(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @QueryParam("callback") String callback) {
        return Response.ok()
                       .entity(generateWfs3Dataset(Wfs3MediaTypes.JSON, /*Wfs3MediaTypes.XML,*/ Wfs3MediaTypes.HTML))
                       .header("Access-Control-Allow-Origin", "*")
                       .header("Access-Control-Allow-Methods", "GET")
                       .build();
    }

    @Path("/collections")
    @GET
    @Produces({MediaTypeCharset.APPLICATION_JSON_UTF8})
    public Response getDatasetAsJson2(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @QueryParam("callback") String callback) {
        return Response.ok()
                       .entity(generateWfs3Dataset(Wfs3MediaTypes.JSON, /*Wfs3MediaTypes.XML,*/ Wfs3MediaTypes.HTML))
                       .header("Access-Control-Allow-Origin", "*")
                       .header("Access-Control-Allow-Methods", "GET")
                       .build();
    }

    @GET
    @Produces({"application/xml;charset=utf-8"})
    public Wfs3Dataset getDatasetAsXml(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @QueryParam("callback") String callback) {
        return generateWfs3Dataset(Wfs3MediaTypes.XML, Wfs3MediaTypes.JSON, Wfs3MediaTypes.HTML);
    }

    private Wfs3Dataset generateWfs3Dataset(String mediaType, String... alternativeMediaTypes) {
        return new Wfs3Dataset(uriInfo.getRequestUri(), service, mediaType, alternativeMediaTypes);
    }

    @Path("/api")
    @Operation
    public OpenApiResource getOpenApi() {
        return openApiResource;
    }

    @Path("/conformance")
    @GET
    @Produces({MediaTypeCharset.APPLICATION_JSON_UTF8})
    public Response getConformanceClasses() {
        return Response.ok()
                       .entity(new Wfs3ConformanceClasses())
                       .header("Access-Control-Allow-Origin", "*")
                       .header("Access-Control-Allow-Methods", "GET")
                       .build();
    }

    private FeatureCollectionView createFeatureCollectionView(WfsProxyFeatureType featureType) {
        URICustomizer uriBuilder = new URICustomizer(uriInfo.getRequestUri())
                .clearParameters()
                .ensureParameter("f", Wfs3MediaTypes.FORMATS.get(Wfs3MediaTypes.HTML))
                .ensureLastPathSegment("items");

        DatasetView dataset = new DatasetView("", uriInfo.getRequestUri());
        FeatureCollectionView featureTypeDataset = new FeatureCollectionView("featureCollection", uriInfo.getRequestUri(), featureType.getName(), featureType.getDisplayName());
        featureTypeDataset.uriBuilder = uriBuilder;
        dataset.featureTypes.add(featureTypeDataset);

        WFSOperation operation = new GetCapabilities();

        WFSCapabilitiesAnalyzer analyzer = new MultiWfsCapabilitiesAnalyzer(
                new GetCapabilities2Dataset(dataset),
                new LoggingWfsCapabilitiesAnalyzer()
        );

        WFSCapabilitiesParser wfsParser = new WFSCapabilitiesParser(analyzer, service.staxFactory);

        wfsParser.parse(service.getWfsAdapter()
                               .request(operation));


        String[] b = featureTypeDataset.bbox.split(" ");
        String[] min = b[0].split(",");
        String[] max = b[1].split(",");

        featureTypeDataset.bbox2 = ImmutableMap.of("minLng", min[1], "minLat", min[0], "maxLng", max[1], "maxLat", max[0]);

        Map<String, String> htmlNames = service.getHtmlNamesForFeatureType(featureType);
        Set<Map.Entry<String, String>> filterFields = service.getFilterableFieldsForFeatureType(featureType)
                                                             .entrySet()
                                                             .stream()
                                                             .peek(entry -> {
                                                                 if (htmlNames.containsKey(entry.getValue())) {
                                                                     entry.setValue(htmlNames.get(entry.getValue()));
                                                                 }
                                                             })
                                                             .collect(Collectors.toSet());
        featureTypeDataset.filterFields = filterFields;

        return featureTypeDataset;
    }

    private FeatureCollectionView createFeatureDetailsView(WfsProxyFeatureType featureType, String featureId, List<Wfs3Link> links) {
        FeatureCollectionView featureTypeDataset = new FeatureCollectionView("featureDetails", uriInfo.getRequestUri(), featureType.getName(), featureType.getDisplayName());
        featureTypeDataset.description = featureType.getDisplayName();

        URICustomizer uriBuilder = new URICustomizer(uriInfo.getRequestUri())
                .clearParameters()
                .ensureParameter("f", Wfs3MediaTypes.FORMATS.get(Wfs3MediaTypes.HTML))
                .removePathSegment("items", -2)
                .removeLastPathSegments(1);

        featureTypeDataset.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", uriBuilder.copy().removePathSegment("collections", -2).removeLastPathSegments(2).toString()))
                .add(new NavigationDTO(service.getName(), uriBuilder.copy().removePathSegment("collections", -2).removeLastPathSegments(1).toString()))
                .add(new NavigationDTO(featureType.getDisplayName(), uriBuilder.toString()))
                .add(new NavigationDTO(featureId))
                .build();

        featureTypeDataset.formats = links.stream()
                                          .filter(wfs3Link -> wfs3Link.rel.equals("alternate"))
                                          .map(wfs3Link -> new NavigationDTO(Wfs3MediaTypes.NAMES.get(wfs3Link.type), wfs3Link.href))
                                          .collect(Collectors.toList());

        /*new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("GeoJson", "f=json"))
                .add(new NavigationDTO("GML", "f=xml"))
                .add(new NavigationDTO("JSON-LD", "f=jsonld"))
                .build();*/

        return featureTypeDataset;
    }

    private void addDatasetNavigation(FeatureCollectionView featureCollectionView, WfsProxyFeatureType featureType, List<Wfs3Link> links) {
        URICustomizer uriBuilder = new URICustomizer(uriInfo.getRequestUri())
                .clearParameters()
                .ensureParameter("f", Wfs3MediaTypes.FORMATS.get(Wfs3MediaTypes.HTML))
                .removePathSegment("items", -1)
                .removePathSegment("collections", -2);

        featureCollectionView.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", uriBuilder.copy().removeLastPathSegments(2).toString()))
                .add(new NavigationDTO(service.getName(), uriBuilder.copy().removeLastPathSegments(1).toString()))
                .add(new NavigationDTO(featureType.getDisplayName()))
                .build();

        // TODO: only activated formats
        featureCollectionView.formats = links.stream()
                .filter(wfs3Link -> wfs3Link.rel.equals("alternate"))
                .map(wfs3Link -> new NavigationDTO(Wfs3MediaTypes.NAMES.get(wfs3Link.type), wfs3Link.href))
                .collect(Collectors.toList());

        /*new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("GeoJson", "f=json"))
                .add(new NavigationDTO("GML", "f=xml"))
                .add(new NavigationDTO("JSON-LD", "f=jsonld"))
                .build();*/


    }

    private void addDatasetIndices(FeatureCollectionView featureCollectionView, WfsProxyFeatureType featureType) {
        ImmutableList.Builder<NavigationDTO> indices = new ImmutableList.Builder<>();
        for (String index : service.findIndicesForFeatureType(featureType)
                                   .keySet()) {
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
        List<String> values = service.getIndexValues(featureType, fields, service.findIndicesForFeatureType(featureType)
                                                                                 .get(fields));
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
            announcements = service.getSparqlAdapter()
                                   .request(filterValue, SparqlAdapter.QUERY.ADDRESS_LOCALITY);
        } else if (filterKey.equals("postalCode")) {
            announcements = service.getSparqlAdapter()
                                   .request(filterValue, SparqlAdapter.QUERY.POSTAL_CODE);
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
        if (service.findIndicesForFeatureType(featureType)
                   .containsKey(filterKey)) {
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

    @Path("/collections/{layerid}")
    @GET
    @Produces(MediaTypeCharset.TEXT_HTML_UTF8)
    public Response getFeaturesAsHtml1(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @QueryParam("fields") String fields, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {
        return getFeaturesAsHtml(layerid, fields, callback, range);
    }

    @Path("/collections/{layerid}/items")
    @GET
    @Produces(MediaTypeCharset.TEXT_HTML_UTF8)
    public Response getFeaturesAsHtml2(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @QueryParam("fields") String fields, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {
        return getFeaturesAsHtml(layerid, fields, callback, range);
    }

    public Response getFeaturesAsHtml(String layerid, String fields, String callback, String range) {

        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger()
              .debug("GET HTML FOR {} {}", featureType.getNamespace(), featureType.getName());

        int[] r = RangeHeader.parseRange(range);
        List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(uriInfo.getRequestUri(), true, r[2], r[3], Wfs3MediaTypes.HTML, Wfs3MediaTypes.GEO_JSON, Wfs3MediaTypes.GML);



        FeatureCollectionView featureTypeDataset = createFeatureCollectionView(featureType);

        addDatasetNavigation(featureTypeDataset, featureType, links);

        addDatasetIndices(featureTypeDataset, featureType);


        List<String> groupings = new ArrayList<>();
        if (layerid.equals("inspireadressen")) {
            groupings.add("woonplaats");
        }

        String query = request.getQueryString() == null ? "" : request.getQueryString();


        // TODO: split fields
        if (fields != null && service.findIndicesForFeatureType(featureType)
                                     .containsKey(fields)) {

            createIndexPage(featureTypeDataset, featureType, fields);

            return Response.ok(featureTypeDataset)
                           .build();
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

    @Path("/collections/{layerid}")
    @GET
    public Response getCollectionInfo(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @QueryParam("callback") String callback, @QueryParam("resultType") String resultType, @HeaderParam("Range") String range) {
        final Optional<Wfs3Dataset.Wfs3Collection> wfs3Collection = generateWfs3Dataset(Wfs3MediaTypes.JSON, /*Wfs3MediaTypes.XML,*/ Wfs3MediaTypes.HTML).getCollections().stream().filter(collection -> collection.getName().equals(layerid)).findFirst();

        if (!wfs3Collection.isPresent()) {
            throw new ResourceNotFound();
        }

        //final int[] r = RangeHeader.parseRange(range);
        //final List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(uriInfo.getRequestUri(), true, r[2], r[3], Wfs3MediaTypes.GEO_JSON, Wfs3MediaTypes.GML, Wfs3MediaTypes.HTML);

        //wfs3Collection.get().setLinks(links);

        return Response.ok()
                       .entity(wfs3Collection.get())
                       .header("Access-Control-Allow-Origin", "*")
                       .header("Access-Control-Allow-Methods", "GET")
                       .build();

        //return getFeaturesAsJson(layerid, indexId, callback, resultType, range);
    }

    @Path("/collections/{layerid}/items")
    @GET
    public Response getFeaturesAsJson2(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @QueryParam("callback") String callback, @QueryParam("resultType") String resultType, @HeaderParam("Range") String range) {
        return getFeaturesAsJson(layerid, indexId, callback, resultType, range);
    }

    public Response getFeaturesAsJson(String layerid, String indexId, String callback, String resultType, String range) {

        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger()
              .debug("GET JSON FOR {} {}", featureType.getNamespace(), featureType.getName());

        if (resultType != null && resultType.equals("hits")) {
            return getJsonHits(getWfsHitsFiltered(featureType), featureType);
        }

        //if (indexId.toLowerCase().equals("all")) {
        return getJsonResponse(getWfsFeaturesPagedAndFiltered(featureType, range), featureType, true, range, callback);
        //} else {
        //    return getWfsPropertiesPaged(layerid, range, indexId);
        //}
    }

    @Path("/collections/{layerid}")
    @GET
    @Produces("application/ld+json;charset=utf-8")
    public Response getFeaturesAsJsonLd1(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {
        return getFeaturesAsJsonLd(layerid, indexId, callback, range);
    }

    @Path("/collections/{layerid}/items")
    @GET
    @Produces("application/ld+json;charset=utf-8")
    public Response getFeaturesAsJsonLd2(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {
        return getFeaturesAsJsonLd(layerid, indexId, callback, range);
    }

    public Response getFeaturesAsJsonLd(String layerid, String indexId, String callback, String range) {

        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger()
              .debug("GET JSON-LD FOR {} {}", featureType.getNamespace(), featureType.getName());

        FeatureCollectionView featureTypeDataset = createFeatureCollectionView(featureType);

        WFSOperation wfsOperation = getWfsFeaturesPagedAndFiltered(featureType, range, ((filterKey, filterValue) -> createFilteredByIndexPage(featureTypeDataset, featureType, filterKey, filterValue)));

        //if (indexId.toLowerCase().equals("all")) {
        return getJsonLdResponse(wfsOperation, featureType, true, featureTypeDataset);
        //} else {
        //    return getWfsPropertiesPaged(layerid, range, indexId);
        //}
    }

    @Path("/collections/{layerid}")
    @GET
    @Produces({"application/xml;charset=utf-8", "application/gml+xml;version=3.2"})
    public Response getFeaturesAsXml1(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @QueryParam("properties") String fields, @QueryParam("callback") String callback, @QueryParam("resultType") String resultType, @HeaderParam("Range") String range) {
        return getFeaturesAsXml(layerid, fields, callback, resultType, range);
    }

    @Path("/collections/{layerid}/items")
    @GET
    @Produces({"application/xml;charset=utf-8", "application/gml+xml;version=3.2"})
    public Response getFeaturesAsXml2(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @QueryParam("properties") String fields, @QueryParam("callback") String callback, @QueryParam("resultType") String resultType, @HeaderParam("Range") String range) {
        return getFeaturesAsXml(layerid, fields, callback, resultType, range);
    }

    public Response getFeaturesAsXml(String layerid, String fields, String callback, String resultType, String range) {

        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger()
              .debug("GET XML FOR {} {}", featureType.getNamespace(), featureType.getName());

        if (resultType != null && resultType.equals("hits")) {
            return getWfsResponse(getWfsHitsFiltered(featureType));
        }

        //if (indexId.toLowerCase().equals("all")) {
        return getWfsResponse(getWfsFeaturesPagedAndFiltered(featureType, range));
        //} else {
        //    return getWfsPropertiesPaged(layerid, range, indexId);
        //}
    }

    @Path("/collections/{layerid}/items/{featureid}")
    @GET
    public Response getFeatureByIdAsJson(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @PathParam("featureid") final String featureid, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {

        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger()
              .debug("GET JSON FOR {} {}", featureType.getNamespace(), featureType.getName());

        //if (indexId.toLowerCase().equals("all")) {
        return getJsonResponse(getWfsFeatureById(layerid, featureid), featureType, false);
        //} else {
        //    return getWfsFeaturesPaged(layerid, range, indexId, featureid);
        //}
    }

    @Path("/collections/{layerid}/items/{featureid}")
    @GET
    @Produces("application/ld+json;charset=utf-8")
    public Response getFeatureByIdAsJsonLd(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @PathParam("featureid") final String featureid, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {

        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger()
              .debug("GET JSON-LD FOR {} {}", featureType.getNamespace(), featureType.getName());

        //if (indexId.toLowerCase().equals("all")) {
        return getJsonLdResponse(getWfsFeatureById(layerid, featureid), featureType, false, null);
        //} else {
        //    return getWfsFeaturesPaged(layerid, range, indexId, featureid);
        //}
    }

    @Path("/collections/{layerid}/items/{featureid}")
    @GET
    @Produces({"application/xml;charset=utf-8", "application/gml+xml;version=3.2"})
    public Response getFeatureByIdAsAXml(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @PathParam("featureid") final String featureid, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {

        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger()
              .debug("GET XML FOR {} {}", featureType.getNamespace(), featureType.getName());

        //if (indexId.toLowerCase().equals("all")) {
        return getWfsResponse(getWfsFeatureById(layerid, featureid));
        //} else {
        //    return getWfsFeaturesPaged(layerid, range, indexId, featureid);
        //}
    }

    @Path("/collections/{layerid}/items/{featureid}")
    @GET
    @Produces(MediaTypeCharset.TEXT_HTML_UTF8)
    public Response getFeatureByIdAsHtml(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,*/ @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @PathParam("featureid") final String featureid, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {
        WfsProxyFeatureType featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.getLogger()
              .debug("GET HTML FOR {} {}", featureType.getNamespace(), featureType.getName());

        List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(uriInfo.getRequestUri(), false, 0, 0, Wfs3MediaTypes.HTML, Wfs3MediaTypes.GEO_JSON, Wfs3MediaTypes.GML);

        FeatureCollectionView featureTypeDataset = createFeatureDetailsView(featureType, featureid, links);

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

        int[] r = RangeHeader.parseRange(range);
        List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(uriInfo.getRequestUri(), isFeatureCollection, r[2], r[3], Wfs3MediaTypes.GEO_JSON, Wfs3MediaTypes.GML, Wfs3MediaTypes.HTML);

        return getResponse(operation, featureType,
                outputStream -> new GeoJsonFeatureWriter(service.createJsonGenerator(outputStream), service.jsonMapper, isFeatureCollection, featureType.getMappings(), Gml2GeoJsonMappingProvider.MIME_TYPE, service.getCrsTransformations()
                                                                                                                                                                                                                     .getDefaultTransformer(), links), null, range, callback)
                .type("application/geo+json")
                .build();
    }

    private Response getJsonHits(final WFSOperation operation, final WfsProxyFeatureType featureType) {

        return getResponse(operation, featureType,
                outputStream -> new GeoJsonHitsWriter(service.createJsonGenerator(outputStream), service.jsonMapper, true, service.getCrsTransformations()
                                                                                                                                  .getDefaultTransformer()))
                .build();
    }

    private Response getJsonLdResponse(final WFSOperation operation, final WfsProxyFeatureType featureType, final boolean isFeatureCollection, final FeatureCollectionView dataset) {

        return getResponse(operation, featureType,
                outputStream -> new JsonLdOutputWriter(service.createJsonGenerator(outputStream), service.jsonMapper, isFeatureCollection, featureType.getMappings(), Gml2JsonLdMappingProvider.MIME_TYPE, service.getCrsTransformations()
                                                                                                                                                                                                                  .getDefaultTransformer(), uriInfo.getRequestUri(), dataset, service.getRewrites(), service.getVocab()))
                .build();
    }

    private Response getHtmlResponse(final WFSOperation operation, final WfsProxyFeatureType featureType, final boolean isFeatureCollection, final List<String> groupings, final boolean group, final String query, final int[] range, final FeatureCollectionView featureTypeDataset) {

        return getResponse(operation, featureType,
                outputStream -> new MicrodataFeatureWriter(new OutputStreamWriter(outputStream), featureType.getMappings(), Gml2MicrodataMappingProvider.MIME_TYPE, isFeatureCollection, featureType.getName()
                                                                                                                                                                                                    .equals("inspireadressen"), groupings, group, query, range, featureTypeDataset, service.getCrsTransformations()
                                                                                                                                                                                                                                                                                           .getDefaultTransformer(), service.getSparqlAdapter(), service.getCodelistStore(), mustacheRenderer),
                wfsRequest -> {
                    if (featureTypeDataset != null) featureTypeDataset.requestUrl = wfsRequest.getAsUrl();
                }
        ).build();
    }

    private Response.ResponseBuilder getResponse(final WFSOperation operation, final WfsProxyFeatureType featureType, final GmlAnalyzerBuilder gmlAnalyzer) {
        return getResponse(operation, featureType, gmlAnalyzer, null);
    }

    private Response.ResponseBuilder getResponse(final WFSOperation operation, final WfsProxyFeatureType featureType, final GmlAnalyzerBuilder gmlAnalyzer, final WFSRequestConsumer wfsRequestConsumer) {
        return getResponse(operation, featureType, gmlAnalyzer, wfsRequestConsumer, null, null);
    }

    private Response.ResponseBuilder getResponse(final WFSOperation operation, final WfsProxyFeatureType featureType, final GmlAnalyzerBuilder gmlAnalyzer, final WFSRequestConsumer wfsRequestConsumer, final String range, final String callback) {
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
                        if (featureType.getMappings()
                                       .getMappings()
                                       .isEmpty()) {
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
                    if (featureType.getMappings()
                                   .getMappings()
                                   .isEmpty()) {
                        gmlParser.enableTextParsing();
                    }
                    gmlParser.parse(request.getResponse(), featureType.getNamespace(), featureType.getName());

                } catch (ExecutionException ex) {
                    // ignore
                }
            };
        }

        return response.entity(stream)
                       .header("Access-Control-Allow-Origin", "*")
                       .header("Access-Control-Allow-Methods", "GET");
    }

    private Response getWfsResponse(final WFSOperation operation) {
        StreamingOutput stream = output -> {
            HttpEntity res = service.getWfsAdapter()
                                    .request(operation);
            res.writeTo(output);
        };

        return Response.ok()
                       .entity(stream)
                       .type("application/gml+xml;version=3.2")
                       .build();
    }

    private WfsProxyFeatureType getFeatureTypeForLayerId(String id) {
        Optional<WfsProxyFeatureType> featureType = service.getFeatureTypeByName(id);

        if (!featureType.isPresent() || (service.getServiceProperties()
                                                .getMappingStatus()
                                                .isEnabled() && !featureType.get()
                                                                            .isEnabled())) {
            throw new ResourceNotFound();
        }

        return featureType.get();
    }
}
