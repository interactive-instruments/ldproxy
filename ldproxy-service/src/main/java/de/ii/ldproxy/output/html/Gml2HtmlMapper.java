package de.ii.ldproxy.output.html;


import de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyService;
import de.ii.xsf.logging.XSFLogger;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 * @author zahnen
 */
public class Gml2HtmlMapper extends AbstractWfsProxyFeatureTypeAnalyzer {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(Gml2HtmlMapper.class);
    public static final String MIME_TYPE = "text/html";

    public Gml2HtmlMapper(WfsProxyService proxyService) {
        super(proxyService);
    }

    @Override
    protected String getTargetType() {
        return MIME_TYPE;
    }

    @Override
    protected TargetMapping getTargetMappingForAttribute(String nsuri, String localName, String type, boolean required) {

        if ((localName.equals("id") && nsuri.startsWith(GML_NS_URI)) || localName.equals("fid")) {

            LOGGER.getLogger().debug("ID {} {} {}", nsuri, localName, type);

            HtmlMapping targetMapping = new HtmlMapping();
            targetMapping.setEnabled(true);
            targetMapping.setName("id");

            return targetMapping;
        }

        return null;
    }

    @Override
    protected TargetMapping getTargetMappingForProperty(String jsonPath, String nsuri, String localName, String type, long minOccurs, long maxOccurs, int depth, boolean isParentMultiple, boolean isComplex, boolean isObject) {

        GML_TYPE dataType = GML_TYPE.fromString(type);

        if (dataType.isValid()) {
            LOGGER.getLogger().debug("PROPERTY {} {}", jsonPath, dataType);

            HtmlMapping targetMapping = new HtmlMapping();
            targetMapping.setEnabled(true);
            targetMapping.setName(jsonPath);

            return targetMapping;
        }

        GML_GEOMETRY_TYPE geoType = GML_GEOMETRY_TYPE.fromString(type);

        if (geoType.isValid()) {
            LOGGER.getLogger().debug("GEOMETRY {} {}", jsonPath, geoType);

            HtmlMapping targetMapping = new HtmlMapping();
            targetMapping.setEnabled(false);
            targetMapping.setName(jsonPath);

            return targetMapping;
        }

        LOGGER.getLogger().debug("NOT MAPPED {} {}", jsonPath, type);

        return null;
    }
}
