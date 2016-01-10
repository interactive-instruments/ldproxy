package de.ii.ogc.wfs.proxy;

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLSchemaAnalyzer;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.util.HashSet;
import java.util.Set;

/**
 * @author zahnen
 */
public abstract class AbstractWfsProxyFeatureTypeAnalyzer implements GMLSchemaAnalyzer {

    public enum GML_TYPE {
        ID("ID"),
        STRING("string"),
        DATE_TIME("dateTime"),
        DATE("date"),
        GEOMETRY("geometry"),
        DECIMAL("decimal"),
        DOUBLE("double"),
        INT("int"),
        INTEGER("integer"),
        NONE("");

        private String stringRepresentation;

        GML_TYPE(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        public static GML_TYPE fromString(String type) {
            for (GML_TYPE v : GML_TYPE.values()) {
                if (v.toString().equals(type)) {
                    return v;
                }
            }

            return NONE;
        }

        public static boolean contains(String type) {
            for (GML_TYPE v : GML_TYPE.values()) {
                if (v.toString().equals(type)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isValid() {
            return this != NONE;
        }
    }

    public enum GML_GEOMETRY_TYPE {

        GEOMETRY("GeometryPropertyType"),
        ABSTRACT_GEOMETRY("GeometricPrimitivePropertyType"),
        POINT("PointPropertyType"),
        MULTI_POINT("MultiPointPropertyType"),
        LINE_STRING("LineStringPropertyType"),
        MULTI_LINESTRING("MultiLineStringPropertyType"),
        CURVE("CurvePropertyType"),
        MULTI_CURVE("MultiCurvePropertyType"),
        SURFACE("SurfacePropertyType"),
        MULTI_SURFACE("MultiSurfacePropertyType"),
        POLYGON("PolygonPropertyType"),
        MULTI_POLYGON("MultiPolygonPropertyType"),
        SOLID("SolidPropertyType"),
        NONE("");

        private String stringRepresentation;

        GML_GEOMETRY_TYPE(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        public static GML_GEOMETRY_TYPE fromString(String type) {
            for (GML_GEOMETRY_TYPE v : GML_GEOMETRY_TYPE.values()) {
                if (v.toString().equals(type)) {
                    return v;
                }
            }
            return NONE;
        }

        public static boolean contains(String type) {
            for (GML_GEOMETRY_TYPE v : GML_GEOMETRY_TYPE.values()) {
                if (v.toString().equals(type)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isValid() {
            return this != NONE;
        }
    }

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(AbstractWfsProxyFeatureTypeAnalyzer.class);
    protected static final String GML_NS_URI = GML.getNS(GML.VERSION._2_1_1);

    private WfsProxyService proxyService;
    // TODO: could it be more than one?
    private WfsProxyFeatureType currentFeatureType;
    private XMLPathTracker currentPath;
    private XMLPathTracker currentPathWithoutObjects;
    private Set<String> mappedPaths;
    //private boolean geometryMapped;
    private int geometryCounter;

    public AbstractWfsProxyFeatureTypeAnalyzer(WfsProxyService proxyService) {
        this.proxyService = proxyService;
        this.currentPath = new XMLPathTracker();
        this.currentPathWithoutObjects = new XMLPathTracker();
        this.mappedPaths = new HashSet<>();
        //this.geometryMapped = false;
        this.geometryCounter = -1;
    }

    @Override
    public void analyzeFeatureType(String nsuri, String localName) {

        //this.currentLayers = new ArrayList();


        if (nsuri.isEmpty()) {
            //LOGGER.error(FrameworkMessages.NSURI_IS_EMPTY);
        }

        String fullName = nsuri + ":" + localName;
        currentFeatureType = proxyService.getFeatureTypes().get(fullName);

        //currentLayers.add(proxyService.findLayerForFeatureType(nsuri, localName));

        //LOGGER.debug(FrameworkMessages.MAPPING_FEATURETYPE_TO_LAYER, localName, currentLayers.get(0).getId());
        mappedPaths.clear();
        currentPath.clear();
        currentPathWithoutObjects.clear();

        //geometryMapped = false;
        this.geometryCounter = -1;

        proxyService.getWfsAdapter().addNamespace(nsuri);


        TargetMapping targetMapping = getTargetMappingForFeatureType(nsuri, localName);

        if (targetMapping != null) {
            currentFeatureType.getMappings().addMapping(fullName, getTargetType(), targetMapping);
        }
    }

    @Override
    public void analyzeAttribute(String nsuri, String localName, String type, boolean required) {
        proxyService.getWfsAdapter().addNamespace(nsuri);

        currentPath.track(nsuri, "@" + localName);

        // only gml:id of the feature for now
        if ((localName.equals("id") && nsuri.startsWith(GML_NS_URI)) || localName.equals("fid")) {
            String path = currentPath.toString();

            if (currentFeatureType != null && !isPathMapped(path)) {

                TargetMapping targetMapping = getTargetMappingForAttribute(nsuri, localName, type, required);

                if (targetMapping != null) {
                    mappedPaths.add(path);

                    currentFeatureType.getMappings().addMapping(path, getTargetType(), targetMapping);
                }
            }
        }
    }

    abstract protected String getTargetType();
    abstract protected TargetMapping getTargetMappingForFeatureType(String nsuri, String localName);
    abstract protected TargetMapping getTargetMappingForAttribute(String nsuri, String localName, String type, boolean required);
    abstract protected TargetMapping getTargetMappingForProperty(String path, String nsuri, String localName, String type, long minOccurs, long maxOccurs, int depth, boolean isParentMultiple, boolean isComplex, boolean isObject);

    @Override
    public void analyzeProperty(String nsuri, String localName, String type, long minOccurs, long maxOccurs, int depth,
                                boolean isParentMultiple, boolean isComplex, boolean isObject) {
        proxyService.getWfsAdapter().addNamespace(nsuri);

        currentPath.track(nsuri, localName, depth);

        if (!isObject) {
            currentPathWithoutObjects.track(nsuri, localName, depth);
        }

        String path = currentPath.toString();

        // skip first level gml properties
        if (path.startsWith(GML_NS_URI)) {
            return;
        }

        if (currentFeatureType != null && !isPathMapped(path)) {

            TargetMapping targetMapping = getTargetMappingForProperty(currentPathWithoutObjects.toFieldName(), nsuri, localName, type, minOccurs, maxOccurs, depth, isParentMultiple, isComplex, isObject);

            if (targetMapping != null) {
                mappedPaths.add(path);

                currentFeatureType.getMappings().addMapping(path, getTargetType(), targetMapping);
            }
        }


        //LOGGER.info("localName {} TYPE {}", localName, type);

        /*MICRODATA_TYPE dataType = MICRODATA_TYPE.forGmlType(GML_TYPE.fromString(type));

        if (dataType.isValid()) {
            String path = currentPath.toString();
            if (!isPathMapped(path)) {
                mappedPaths.add(path);

                GEOMETRY_TYPE_MAPPING geoType = GEOMETRY_TYPE_MAPPING.fromString(type);
                if (geoType.isValid()) {

                    for (WFS2GSFSLayer currentLayer : currentLayers) {

                        if (geometryCounter == -1) {
                            currentLayer.setGeometryType(geoType.toEsri().toString());
                            currentLayer.initDrawingInfo();
                        }

                        currentLayer.addMapping(path, geometryCounter, (maxOccurs > 1 || maxOccurs == -1), minOccurs > 0);
                        LOGGER.debug(FrameworkMessages.MAPPED_GEOMETRY_PROPERTY_OF_TYPE_TO_GEOMETRY_FIELD_OF_TYPE, geoType.toString(), geoType.toEsri().toString());

                        //geometryMapped = true;
                        this.geometryCounter--;

                    }
                } else {
                    String fieldName = currentPathWithoutObjects.toFieldName();

                    if (fieldName.equals("id")) {
                        fieldName = "internalId";
                    }
                    for (WFS2GSFSLayer currentLayer : currentLayers) {
                        List<Integer> properties = new ArrayList();
                        if (isParentMultiple) {
                            for (int i = 1; i < 4; i++) {
                                String stri = String.valueOf(i);
                                int f = currentLayer.addField(fieldName + "." + stri, fieldName + "." + stri, dataType.toEsri());
                                properties.add(f);

                                LOGGER.debug(FrameworkMessages.MAPPED_MULTIPLE_PROPERTY_TYPE_TO_FIELD_OF_TYPE,
                                        dataType.toString(), fieldName, dataType.toEsri().toString());
                            }
                        } else {
                            int f = currentLayer.addField(fieldName, fieldName, dataType.toEsri());
                            properties.add(f);
                            LOGGER.debug(FrameworkMessages.MAPPED_PROPERTY_OF_TYPETO_FIELD_OF_TYPE,
                                    dataType.toString(), fieldName, dataType.toEsri().toString());
                        }
                        currentLayer.addMapping(path, properties, isParentMultiple, minOccurs > 0);
                    }
                }
            }
        }*/
    }

    // this prevents that we descend further on a mapped path
    private boolean isPathMapped(String path) {
        for (String mappedPath: mappedPaths) {
            if (path.startsWith(mappedPath + "/")) {
                return true;
            }
        }
        return false;
    }
}
