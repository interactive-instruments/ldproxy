package de.ii.ogc.wfs.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import de.ii.ldproxy.output.geojson.Gml2GeoJsonMapper;
import de.ii.ldproxy.output.html.Gml2HtmlMapper;
import de.ii.xsf.core.api.AbstractService;
import de.ii.xsf.core.api.Resource;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.exceptions.ParseError;
import de.ii.xtraplatform.ogc.api.exceptions.WFSException;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLSchemaAnalyzer;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLSchemaParser;
import de.ii.xtraplatform.ogc.api.i18n.FrameworkMessages;
import de.ii.xtraplatform.ogc.api.wfs.client.DescribeFeatureType;
import de.ii.xtraplatform.ogc.api.wfs.client.GetCapabilities;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesParser;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.codehaus.staxmate.SMInputFactory;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
public abstract class AbstractWfsProxyService extends AbstractService implements Resource, WfsProxyService {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(AbstractWfsProxyService.class);

    private WFSAdapter wfsAdapter;
    private WfsProxyCrsTransformations crsTransformations;
    private WFSProxyServiceProperties serviceProperties;
    private final Map<String, WfsProxyFeatureType> featureTypes;

    protected HttpClient httpClient;
    protected HttpClient sslHttpClient;
    // TODO
    @JsonIgnore
    public SMInputFactory staxFactory;
    // TODO
    @JsonIgnore
    public ObjectMapper jsonMapper;
    protected JsonFactory jsonFactory;

    public AbstractWfsProxyService() {
        super();
        this.featureTypes = new HashMap<>();
    }

    public AbstractWfsProxyService(String id, String type, File configDirectory, WFSAdapter wfsAdapter) {
        super(id, type, configDirectory);
        this.wfsAdapter = wfsAdapter;
        this.featureTypes = new HashMap<>();
    }

    @Override
    public WFSAdapter getWfsAdapter() {
        return wfsAdapter;
    }

    public void setWfsAdapter(WFSAdapter wfsAdapter) {
        this.wfsAdapter = wfsAdapter;
    }

    @Override
    public WFSProxyServiceProperties getServiceProperties() {
        return serviceProperties;
    }

    public void setServiceProperties(WFSProxyServiceProperties serviceProperties) {
        this.serviceProperties = serviceProperties;
    }

    @Override
    public Map<String, WfsProxyFeatureType> getFeatureTypes() {
        return featureTypes;
    }

    public void setFeatureTypes(Map<String, WfsProxyFeatureType> featureTypes) {
        this.featureTypes.putAll(featureTypes);
    }

    public WfsProxyCrsTransformations getCrsTransformations() {
        return crsTransformations;
    }

    public final void initialize(String[] path, HttpClient httpClient, HttpClient sslHttpClient, SMInputFactory staxFactory, ObjectMapper jsonMapper, CrsTransformation crsTransformation) {
        // TODO
        //this.useFormattedJsonOutput = true;//module.getConfiguration().useFormattedJsonOutput;
        this.httpClient = httpClient;
        this.sslHttpClient = sslHttpClient;
        this.staxFactory = staxFactory;
        this.jsonMapper = jsonMapper;
        this.jsonFactory = new JsonFactory();

        /*this.path = path;
        if (path != null && path.length == 2 && path[0] != null) {
            this.orgid = path[0];
        }*/

        if (this.wfsAdapter != null) {
            this.wfsAdapter.initialize(this.httpClient, this.sslHttpClient);
        }

        this.crsTransformations = new WfsProxyCrsTransformations(crsTransformation, wfsAdapter.getDefaultCrs(), new EpsgCrs(4326, true));

        /*for (WFS2GSFSLayer l : fullLayers) {
            l.initialize(this);
        }*/
    }

    // TODO
    public JsonGenerator createJsonGenerator(OutputStream output) throws IOException {
        JsonGenerator json = jsonFactory.createGenerator(output);
        json.setCodec(jsonMapper);
        //if (useFormattedJsonOutput) {
            json.useDefaultPrettyPrinter();
        //}
        // Zum JSON debuggen hier einschalten.
        //JsonGenerator jsond = new JsonGeneratorDebug(json);
        return json;
    }

    private void analyzeCapabilities() {
        try {
            analyzeCapabilities(null);
        } catch (WFSException ex) {
            for (WFS.VERSION version : WFS.VERSION.values()) {
                try {
                    analyzeCapabilities(version);
                    return;
                } catch (WFSException ex2) {
                    // ignore
                }
            }

            ParseError pe = new ParseError("Parsing of GetCapabilities response failed.");
            pe.addDetail(ex.getMsg());
            for( String det : ex.getDetails()) {
                pe.addDetail(det);
            }
            throw pe;
        }
    }

    private void analyzeCapabilities(WFS.VERSION version) throws ParseError {

        HttpEntity capabilities;

        if (version == null) {
            LOGGER.debug(FrameworkMessages.ANALYZING_CAPABILITIES);
            capabilities = wfsAdapter.request(new GetCapabilities());
        } else {
            LOGGER.debug(FrameworkMessages.ANALYZING_CAPABILITIES_VERSION, version.toString());
            capabilities = wfsAdapter.request(new GetCapabilities(version));
        }

        WFSCapabilitiesAnalyzer gsfsMapper = new WfsProxyCapabilitiesAnalyzer(this);
        WFSCapabilitiesParser wfsParser = new WFSCapabilitiesParser(gsfsMapper, staxFactory);

        wfsParser.parse(capabilities);

        // TODO
        // tell the WFSadapter we are done with the capabilities.
        this.wfsAdapter.capabilitiesAnalyzed();
    }

    private Map<String, List<String>> retrieveSupportedFeatureTypesPerNamespace() {
        Map<String, List<String>> featureTypesPerNamespace = new HashMap<>();

        for (WfsProxyFeatureType featureType: featureTypes.values()) {
            if (!featureTypesPerNamespace.containsKey(featureType.getNamespace())) {
                featureTypesPerNamespace.put(featureType.getNamespace(), new ArrayList<String>());
            }
            featureTypesPerNamespace.get(featureType.getNamespace()).add(featureType.getName());
        }

        return featureTypesPerNamespace;
    }

    private void analyzeFeatureTypes() {
        HttpEntity dft = wfsAdapter.request(new DescribeFeatureType());
        // TODO: ???
        URI baseuri = wfsAdapter.findUrl(WFS.OPERATION.DESCRIBE_FEATURE_TYPE, WFS.METHOD.GET);

        Map<String, List<String>> fts = retrieveSupportedFeatureTypesPerNamespace();

        List<GMLSchemaAnalyzer> analyzers = new ArrayList<>();

        // TODO: dynamic
        analyzers.add(new Gml2GeoJsonMapper(this));
        analyzers.add(new Gml2HtmlMapper(this));

        if (!featureTypes.isEmpty()) {
            // create mappings
            GMLSchemaParser gmlSchemaParser;
            // TODO: temporary basic auth hack
            //if (wfs.usesBasicAuth()) {
            //    gmlParser = new GMLSchemaParser(analyzers, baseuri, new OGCEntityResolver(sslHttpClient, wfs.getUser(), wfs.getPassword()));
            //} else {
            gmlSchemaParser = new GMLSchemaParser(analyzers, baseuri);
            //}
            gmlSchemaParser.parse(dft, fts);
        }

        // iterate through layers and analyze them
        //for (WFS2GSFSLayer layer : fullLayers) {
            // analyze the layer: gmlidmapping, has data, geometrytype ...
        //    layer.analyze();
        //}

        // only log warnings about timeouts in the analysis phase
        wfsAdapter.setIgnoreTimeouts(true);
    }

    public void analyzeWFS() {
        LOGGER.debug(FrameworkMessages.ANALYZING_WFS);
        analyzeCapabilities();

        if (wfsAdapter.getDefaultCrs() == null) {
            ParseError pe = new ParseError(FrameworkMessages.NO_VALID_SRS_FOUND_IN_GETCAPABILITIES_RESPONSE);
            throw pe;
        }

        wfsAdapter.checkHttpMethodSupport();

        analyzeFeatureTypes();
    }

    @Override
    public String getResourceId() {
        return getId();
    }

    @Override
    public void setResourceId(String id) {
        setId(id);
    }

    @Override
    public String getBrowseUrl() {
        return getId() + "/";
    }

    @Override
    protected void internalStart() {

    }

    @Override
    protected void internalStop() {

    }


    // TODO
    @Override
    public void update(String s) {
    }

    // TODO
    @Override
    public void load() throws IOException {
    }

    // TODO
    @Override
    public void save() {
    }

    @Override
    public void delete() {
        // TODO: iterate layers
    }

    @Override
    public void invalidate() {
        // TODO: iterate layers
    }
}
