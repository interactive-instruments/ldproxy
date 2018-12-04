/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.escape.Escaper;
import com.google.common.xml.XmlEscapers;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.xtraplatform.crs.api.CoordinatesWriterType;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.util.functional.LambdaWithException.consumerMayThrow;

/**
 * @author zahnen
 */
public class FeatureTransformerGmlUpgrade implements GmlConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerGmlUpgrade.class);
    private static final List<String> GEOMETRY_COORDINATES = new ImmutableList.Builder<String>()
            .add("posList")
            .add("pos")
            .add("coordinates")
            .build();;

    private final OutputStream outputStream;
    private final boolean isFeatureCollection;
    private final OutputStreamWriter writer;
    private final XMLNamespaceNormalizer namespaces;
    private final CrsTransformer crsTransformer;
    private final List<Wfs3Link> links;
    private final Escaper escaper;
    private final int pageSize;
    private double maxAllowableOffset;

    private boolean inCurrentStart;
    private boolean inCurrentFeatureStart;
    private boolean inCurrentPropertyStart;
    private boolean inCurrentPropertyText;
    private boolean inCoordinates;
    private CoordinatesWriterType.Builder cwBuilder;
    private Integer currentDimension = null;
    private boolean isLastPage;
    private String locations;

    public FeatureTransformerGmlUpgrade(FeatureTransformationContextGml transformationContext) {
        this.outputStream = transformationContext.getOutputStream();
        this.isFeatureCollection = transformationContext.isFeatureCollection();
        this.writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        this.namespaces = new XMLNamespaceNormalizer(transformationContext.getNamespaces());
        this.crsTransformer = transformationContext.getCrsTransformer().orElse(null);
        this.links = transformationContext.getLinks();
        this.escaper = XmlEscapers.xmlAttributeEscaper();
        this.pageSize  = transformationContext.getLimit();
        this.maxAllowableOffset = transformationContext.getMaxAllowableOffset();
        this.namespaces.addNamespace("wfs", "http://www.opengis.net/wfs/3.0", true);
        this.namespaces.addNamespace("atom", "http://www.w3.org/2005/Atom", true);
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception {
        writer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        if (isFeatureCollection) {
            writer.append("\n<wfs:FeatureCollection");
            namespaces.getNamespaces()
                      .keySet()
                      .forEach(consumerMayThrow(prefix -> {
                          writer.append(" ");
                          writer.append(namespaces.generateNamespaceDeclaration(prefix));
                      }));

            if (numberReturned.isPresent()) {
                writer.append(" numberReturned");
                writer.append("=\"");
                writer.append(String.valueOf(numberReturned.getAsLong()));
                writer.append("\"");
            }
            if (numberMatched.isPresent()) {
                writer.append(" numberMatched");
                writer.append("=\"");
                writer.append(String.valueOf(numberMatched.getAsLong()));
                writer.append("\"");
            }
            writer.append(" timeStamp");
            writer.append("=\"");
            writer.append(Instant.now()
                                 .truncatedTo(ChronoUnit.SECONDS)
                                 .toString());
            writer.append("\"");

            isLastPage = numberReturned.orElse(0) < pageSize;
            inCurrentStart = true;
        }
    }

    @Override
    public void onEnd() throws Exception {
        if (isFeatureCollection) {
            if (inCurrentStart) {
                writer.append(">");

                links.stream()
                     .filter(link -> !(isLastPage && Objects.equals(link.getRel(), "next")))
                     .forEach(consumerMayThrow(link -> writer.append(linkAsAtom(link))));

                inCurrentStart = false;
            }
            writer.append("\n</wfs:FeatureCollection>");
        }
        writer.flush();
        writer.close();
    }

    @Override
    public void onFeatureStart(List<String> path) throws Exception {
        LOGGER.debug("{}", path);

        if (inCurrentStart) {
            writer.append(">");

            links.stream().filter(link -> !(isLastPage && Objects.equals(link.getRel(), "next"))).forEach(consumerMayThrow(link -> writer.append(linkAsAtom(link))));

            inCurrentStart = false;
        }

        if (isFeatureCollection) {
            writer.append("\n<wfs:member>");
        }
        writer.append("\n<");
        writer.append(namespaces.getNamespacePrefix(getNamespaceUri(path)));
        writer.append(":");
        writer.append(getLocalName(path));

        if (!isFeatureCollection) {
            namespaces.getNamespaces()
                      .keySet()
                      .forEach(consumerMayThrow(prefix -> {
                          writer.append(" ");
                          writer.append(namespaces.generateNamespaceDeclaration(prefix));
                      }));
            if (!Strings.isNullOrEmpty(locations)) {
                writer.append(" ");
                writer.append(namespaces.getNamespacePrefix("http://www.w3.org/2001/XMLSchema-instance"));
                writer.append(":schemaLocation");
                writer.append("=\"");
                writer.append(locations);
                writer.append("\"");
            }
        }

        inCurrentFeatureStart = true;
    }

    @Override
    public void onFeatureEnd(List<String> path) throws Exception {
        writer.append("\n</");
        writer.append(namespaces.getNamespacePrefix(getNamespaceUri(path)));
        writer.append(":");
        writer.append(getLocalName(path));
        writer.append(">");
        if (isFeatureCollection) {
            writer.append("\n</wfs:member>");
        }
        writer.flush();
    }

    @Override
    public void onGmlAttribute(String namespace, String localName, List<String> path, String value, List<Integer> multiplicities) throws Exception {
        LOGGER.debug("ATTR {} {} {}", path, localName, value);

        String newValue = value;

        if (!isFeatureCollection && localName.equals("schemaLocation")) {
            locations = adjustSchemaLocation(value);
        }

        if (inCurrentStart) {
            if (localName.equals("schemaLocation")) {
                newValue = adjustSchemaLocation(value);
            } else {
                return;
            }
        }
        if (inCurrentPropertyStart && localName.equals("srsName")) {
            if (Objects.nonNull(crsTransformer)) {
                newValue = crsTransformer.getTargetCrs().getAsUri();
            }
        }
        if (inCurrentPropertyStart && localName.equals("srsDimension")) {
            try {
                currentDimension = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                currentDimension = null;
            }
        }

        if (isFeatureCollection || inCurrentFeatureStart || inCurrentPropertyStart) {
            writer.append(" ");
            if (!Strings.isNullOrEmpty(namespace)) {
                writer.append(namespaces.getNamespacePrefix(namespace));
                writer.append(":");
            }
            writer.append(localName);
            writer.append("=\"");
            writer.append(XmlEscapers.xmlAttributeEscaper().escape(newValue));
            writer.append("\"");
        }
    }

    @Override
    public void onPropertyStart(List<String> path, List<Integer> multiplicities) throws Exception {
        LOGGER.debug("START {} {}", path, getLocalName(path));

        if (inCurrentFeatureStart) {
            writer.append(">");
            inCurrentFeatureStart = false;
        }
        if (inCurrentPropertyStart) {
            writer.append(">");
        }

        writer.append("\n<");
        writer.append(namespaces.getNamespacePrefix(getNamespaceUri(path)));
        writer.append(":");
        writer.append(getLocalName(path));

        inCurrentPropertyStart = true;
        if (GEOMETRY_COORDINATES.contains(getLocalName(path))) {
            inCoordinates = true;
        }
    }

    @Override
    public void onPropertyText(String text) throws Exception {
        if (inCurrentPropertyStart) {
            writer.append(">");
            inCurrentPropertyStart = false;
        }

        if (inCoordinates) {
            cwBuilder = CoordinatesWriterType.builder();
            cwBuilder.format(new CoordinatesFormatterGml(writer));

            if (crsTransformer != null) {
                cwBuilder.transformer(crsTransformer);
            }

            if (currentDimension != null) {
                cwBuilder.dimension(currentDimension);
            }

            if (maxAllowableOffset > 0) {
                cwBuilder.simplifier(maxAllowableOffset, 2);
            }

            Writer coordinatesWriter = cwBuilder.build();
            // TODO: coalesce
            coordinatesWriter.write(text);
            coordinatesWriter.close();
        } else {
            writer.append(XmlEscapers.xmlContentEscaper().escape(text));
        }

        inCurrentPropertyText = true;
    }

    @Override
    public void onPropertyEnd(List<String> path) throws Exception {
        LOGGER.debug("END {} {}", path, getLocalName(path));

        if (inCurrentPropertyStart) {
            writer.append("/>");
            inCurrentPropertyStart = false;
        } else {
            if (!inCurrentPropertyText) {
                writer.append("\n");
            } else {
                inCurrentPropertyText = false;
            }

            writer.append("</");
            writer.append(namespaces.getNamespacePrefix(getNamespaceUri(path)));
            writer.append(":");
            writer.append(getLocalName(path));
            writer.append(">");
        }
        inCoordinates = false;
    }

    @Override
    public void onNamespaceRewrite(QName featureType, String namespace) throws Exception {

    }

    private String adjustSchemaLocation(String schemaLocation) {
        List<String> split = Splitter.on(' ').splitToList(schemaLocation);
        Map<String,String> locations = new LinkedHashMap<>();

        for (int i = 0; i < split.size()-1; i += 2) {
            if (!split.get(i).startsWith("http://www.opengis.net/wfs")) {
                locations.put(split.get(i), split.get(i + 1));
            }
        }

        locations.put("http://www.opengis.net/wfs/3.0", "https://raw.githubusercontent.com/opengeospatial/WFS_FES/master/core/xml/wfs.xsd");
        locations.put("http://www.w3.org/2005/Atom", "http://schemas.opengis.net/kml/2.3/atom-author-link.xsd");


        return locations.entrySet().stream().map(entry -> entry.getKey() + " " + entry.getValue()).collect(Collectors.joining(" "));
    }

    private String linkAsAtom(Wfs3Link link) {
        return String.format("\n<atom:link rel=\"%s\" title=\"%s\" type=\"%s\" href=\"%s\"/>", link.getRel(), link.getTitle(), escaper.escape(link.getType()), escaper.escape(link.getHref()));
    }

    private String getLocalName(List<String> path) {
        return path.isEmpty() ? null : path.get(path.size()-1).substring(path.get(path.size()-1).lastIndexOf(":")+1);
    }

    private String getNamespaceUri(List<String> path) {
        return path.isEmpty() ? null : path.get(path.size()-1).substring(0, path.get(path.size()-1).lastIndexOf(":"));
    }
}
