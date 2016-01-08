package de.ii.ldproxy.output.html;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeMapping;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLAnalyzer;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import org.codehaus.staxmate.in.SMInputCursor;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * @author zahnen
 */
public class HtmlFeatureWriter implements GMLAnalyzer {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(HtmlFeatureWriter.class);

    private OutputStreamWriter outputStreamWriter;
    protected XMLPathTracker currentPath;
    protected FeatureDTO currentFeature;
    protected String outputFormat; // as constant somewhere
    protected WfsProxyFeatureTypeMapping featureTypeMapping; // reduceToOutputFormat
    protected boolean isFeatureCollection;
    protected boolean isAddress;
    protected List<String> groupings;
    protected boolean isGrouped;
    protected String query;
    protected MustacheFactory mustacheFactory;
    protected int page;

    public String jsonQuery;
    public String xmlQuery;
    public List<FeatureDTO> features;
    public List<NavigationDTO> breadCrumbs;
    public List<NavigationDTO> pagination;
    public DatasetDTO dataset;

    public HtmlFeatureWriter(OutputStreamWriter outputStreamWriter, WfsProxyFeatureTypeMapping featureTypeMapping, String outputFormat, boolean isFeatureCollection, boolean isAddress, List<String> groupings, boolean isGrouped, String query, List<NavigationDTO> breadCrumbs, int[] range, DatasetDTO featureTypeDataset) {
        this.outputStreamWriter = outputStreamWriter;
        this.currentPath = new XMLPathTracker();
        this.featureTypeMapping = featureTypeMapping;
        this.outputFormat = outputFormat;
        this.isFeatureCollection = isFeatureCollection;
        this.isAddress = isAddress;
        this.groupings = groupings;
        this.isGrouped = isGrouped;
        this.query = (query == null ? "" : query);
        this.mustacheFactory = new DefaultMustacheFactory(){
            @Override
            public Reader getReader(String resourceName) {
                final InputStream is = getClass().getResourceAsStream(resourceName);
                if (is == null) {
                    throw new MustacheException("Template " + resourceName + " not found");
                }
                return new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
            }

            @Override
            public void encode(String value, Writer writer) {
                try {
                    writer.write(value);
                } catch (IOException e) {
                    // ignore
                }
            }
        };
        if (range != null && range.length > 2) {
            this.page = range[2];
        }

        this.jsonQuery = "?" + this.query + "&f=json";
        this.xmlQuery = "?" + this.query + "&f=xml";
        this.features = new ArrayList<>();
        this.breadCrumbs = breadCrumbs;
        this.dataset = featureTypeDataset;
    }

    @Override
    public void analyzeStart(Future<SMInputCursor> future) {



        LOGGER.getLogger().debug("START");

        try {
            SMInputCursor cursor = future.get();

            int numberMatched = Integer.parseInt(cursor.getAttrValue("numberMatched"));
            int numberReturned = Integer.parseInt(cursor.getAttrValue("numberReturned"));
            int pages = Math.max(page, numberMatched/numberReturned + (numberMatched%numberReturned > 0 ? 1 : 0));

            LOGGER.getLogger().debug("numberMatched {}", numberMatched);
            LOGGER.getLogger().debug("numberReturned {}", numberReturned);
            LOGGER.getLogger().debug("page {}", page);
            LOGGER.getLogger().debug("pages {}", pages);

            ImmutableList.Builder<NavigationDTO> pagination = new ImmutableList.Builder<>();
            if (page > 1) {
                pagination
                        .add(new NavigationDTO("&laquo;", "?page=1"))
                        .add(new NavigationDTO("&lsaquo;", "?page=" + String.valueOf(page-1)));
            } else {
                pagination
                        .add(new NavigationDTO("&laquo;"))
                        .add(new NavigationDTO("&lsaquo;"));
            }

            for (int i = Math.max(1, page-5); i <= Math.min(pages, page+5); i++) {
                if (i == page) {
                    pagination.add(new NavigationDTO(String.valueOf(i), true));
                } else {
                    pagination.add(new NavigationDTO(String.valueOf(i), "?page=" + String.valueOf(i)));
                }
            }

            if (page < pages) {
                pagination
                        .add(new NavigationDTO("&rsaquo;", "?page=" + String.valueOf(page+1)))
                        .add(new NavigationDTO("&raquo;", "?page=" + String.valueOf(pages)));
            } else {
                pagination
                        .add(new NavigationDTO("&rsaquo;"))
                        .add(new NavigationDTO("&raquo;"));
            }

            this.pagination = pagination.build();

        } catch (InterruptedException | ExecutionException | XMLStreamException |NumberFormatException ex) {
            analyzeFailed(ex);
        }

        /*try {
            outputStreamWriter.write("<html>\n" +
                    "<head>\n" +
                    "\t<title>ldproxy REST Browser</title>\n" +
                    "\t<style lang=\"text/css\">\n" +
                    "\t\tbody {\n" +
                    "\t\t\tfont-family: sans-serif;\n" +
                    "\t\t\tfont-size: 0.9em;\n" +
                    "\t\t}\n" +
                    "\t\ta {\n" +
                    "\t\t\ttext-decoration: none;\n" +
                    "\t\t}\n" +
                    "\t</style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "\t<h4>ldproxy</h4>\n" +
                    "\t<hr/>\n");

            outputStreamWriter.write("\t<h4>Other formats:</h4><ul>\n");
            outputStreamWriter.write("\t<li><a href=\"?" + query + "&f=json\" target=\"_blank\" style=\"font-size: 0.8em;\">JSON</a></li>\n");
            outputStreamWriter.write("\t<li><a href=\"?" + query + "&f=xml\" target=\"_blank\" style=\"font-size: 0.8em;\">XML</a></li>\n");
            outputStreamWriter.write("\t</ul>\n");

            if (!isGrouped && (query.isEmpty() || groupings.isEmpty() || !query.startsWith(groupings.get(0)))) {
                if (!groupings.isEmpty()) {
                    outputStreamWriter.write("<h4>Browse by:</h4><ul>");
                    for (String group : groupings) {
                        outputStreamWriter.write("<li><a href=\"?fields=");
                        outputStreamWriter.write(group);
                        outputStreamWriter.write("&distinctValues=true\">");
                        outputStreamWriter.write(group);
                        outputStreamWriter.write("</a></li>");
                    }
                    outputStreamWriter.write("</ul>");
                }
                if (isFeatureCollection) {
                    outputStreamWriter.write("<br/><h3>Features:</h3>");
                }
            } else {
                outputStreamWriter.write("<br/><h3>Browse ");
                outputStreamWriter.write("inspireadressen");
                outputStreamWriter.write(" by ");
                outputStreamWriter.write(groupings.get(0));
                if (query.startsWith(groupings.get(0))) {
                    String val = query.substring(groupings.get(0).length()+1, query.indexOf('&') > 0 ? query.indexOf('&') : query.length());
                    outputStreamWriter.write(" '" + val + "'");
                }
                outputStreamWriter.write(":</h3>");
            }

            if (isFeatureCollection || isGrouped) {
                outputStreamWriter.write("<ul>");
            }
        } catch (IOException e) {
            analyzeFailed(e);
        }*/
    }

    @Override
    public void analyzeEnd() {
        /*try {
            if (isFeatureCollection || isGrouped) {
                outputStreamWriter.write("</ul>");
            }
            outputStreamWriter.write("\n\n</body>\n" +
                    "</html>");
            outputStreamWriter.close();
        } catch (IOException e) {
            analyzeFailed(e);
        }*/

        try {
            Mustache mustache;
            if (isFeatureCollection) {
                mustache = mustacheFactory.compile("featureCollection.mustache");
            } else {
                mustache = mustacheFactory.compile("featureDetails.mustache");
            }
            mustache.execute(outputStreamWriter, this).flush();
        } catch (Exception e) {
            analyzeFailed(e);
        }
    }

    @Override
    public void analyzeFeatureStart(String id, String nsuri, String localName) {
        currentPath.clear();

        /*try {
            if (isFeatureCollection || isGrouped) {
                outputStreamWriter.write("<li>");
            }
            if (!isGrouped) {
                outputStreamWriter.write("\n\n<div itemscope itemtype=\"http://schema.org/Place\">");
                outputStreamWriter.write("<h4>");
                outputStreamWriter.write(localName);
                outputStreamWriter.write("</h4>");
                outputStreamWriter.write("<ul>");
            }

        } catch (IOException e) {
            analyzeFailed(e);
        }*/

        currentFeature = new FeatureDTO();
        if (!isFeatureCollection) {
            currentFeature.details = true;
        }
    }

    @Override
    public void analyzeFeatureEnd() {
        /*try {
            if (!isGrouped) {
                outputStreamWriter.write("</ul>");
                outputStreamWriter.write("</div>");
            }
            if (isFeatureCollection || isGrouped) {
                outputStreamWriter.write("</li>");
            }

        } catch (IOException e) {
            analyzeFailed(e);
        }*/
        features.add(currentFeature);
        currentFeature = null;
    }

    @Override
    public void analyzeAttribute(String nsuri, String localName, String value) {
        currentPath.track(nsuri, "@" + localName);
        String path = currentPath.toString();
        //LOGGER.debug(" - attribute {}", path);

        for (TargetMapping mapping: featureTypeMapping.findMappings(path, outputFormat)) {
            if (!mapping.isEnabled()) {
                continue;
            }

            writeField((HtmlMapping)mapping, value, true);
        }
    }

    @Override
    public void analyzePropertyStart(String nsuri, String localName, int depth, SMInputCursor feature, boolean nil) {
        currentPath.track(nsuri, localName, depth);
        String path = currentPath.toString();
        String value = "";

        for (TargetMapping mapping: featureTypeMapping.findMappings(path, outputFormat)) {
            if (!mapping.isEnabled()) {
                continue;
            }

            // TODO: I guess fieldCounter is for multiplicity

            // TODO: only if not geometry
            //if (((GeoJsonPropertyMapping)mapping).getType() != GeoJsonPropertyMapping.GEO_JSON_TYPE.GEOMETRY) {
                if (value.isEmpty()) {
                    try {
                        value = feature.collectDescendantText();
                    } catch (XMLStreamException ex) {
                        //LOGGER.error(FrameworkMessages.ERROR_PARSING_GETFEATURE_REQUEST);
                        analyzeFailed(ex);
                    }
                }

                writeField((HtmlMapping) mapping, value, false);
            //} else {
                // TODO: write geometry
                //writeGeometry(((GeoJsonGeometryMapping)mapping).getGeometryType(), feature);
            //}
        }
    }

    @Override
    public void analyzePropertyEnd(String s, String s1, int i) {

    }

    @Override
    public void analyzeFailed(Exception e) {
        LOGGER.getLogger().error("Error writing HTML", e);
    }

    protected void writeField(HtmlMapping mapping, String value, boolean isId) {

        /*if (value == null || value.isEmpty()) {
            return;
        }*/
        if (value == null) {
            value = "";
        }

        if (isId) {
            currentFeature.displayName = value;
        } else if(isAddress && mapping.getName().equals("geom")) {
            currentFeature.geo = true;
            String[] coords = value.split(" ");
            currentFeature.lat = coords[0];
            currentFeature.lon = coords[1];
        } else {
            currentFeature.fields.add(new ImmutableMap.Builder<String,String>().put("name", mapping.getName()).put("value", value).build());
        }

        /*try {
            if (!isGrouped) {
                if (isAddress && mapping.getName().equals("geom")) {
                    outputStreamWriter.write("<li style=\"display: table-row;\" itemprop=\"geo\" itemscope itemtype=\"http://schema.org/GeoCoordinates\">");
                } else {
                    outputStreamWriter.write("<li style=\"display: table-row;\">");
                }
                outputStreamWriter.write("<b style=\"display: table-cell;\">");
                outputStreamWriter.write(mapping.getName());
                outputStreamWriter.write(":&nbsp;</b>");
                outputStreamWriter.write("<span style=\"display: table-cell;\">");
                if (isAddress && mapping.getName().equals("geom")) {
                    String[] coords = value.split(" ");
                    outputStreamWriter.write("<span itemprop=\"latitude\">");
                    outputStreamWriter.write(coords[0]);
                    outputStreamWriter.write("</span>&nbsp;");
                    outputStreamWriter.write("<span itemprop=\"longitude\">");
                    outputStreamWriter.write(coords[1]);
                    outputStreamWriter.write("</span>");
                } else {
                    if (isId) {
                        outputStreamWriter.write("<a itemprop=\"url\" href=\"");
                        outputStreamWriter.write(value);
                        outputStreamWriter.write("\">");
                    }
                    outputStreamWriter.write(value);
                    if (isId) {
                        outputStreamWriter.write("</a>");
                    }
                }
                outputStreamWriter.write("</span>");
                outputStreamWriter.write("</li>");
            }
            else {
                outputStreamWriter.write("<a href=\"?");
                outputStreamWriter.write(groupings.get(0));
                outputStreamWriter.write("=");
                outputStreamWriter.write(value);
                outputStreamWriter.write("\">");
                outputStreamWriter.write(value);
                outputStreamWriter.write("</a>");
            }
        } catch (IOException e) {
            analyzeFailed(e);
        }*/

    }
}
