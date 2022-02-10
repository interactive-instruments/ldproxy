/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.gml.app;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.escape.Escaper;
import com.google.common.xml.XmlEscapers;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.geometries.domain.ImmutableCoordinatesTransformer;
import de.ii.xtraplatform.xml.domain.XMLNamespaceNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.dropwizard.domain.LambdaWithException.biConsumerMayThrow;
import static de.ii.xtraplatform.dropwizard.domain.LambdaWithException.consumerMayThrow;

/**
 * @author zahnen
 */
public class FeatureTransformerGmlUpgrade implements FeatureConsumer {

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
    private final List<Link> links;
    private final Escaper escaper;
    private final int pageSize;
    private double maxAllowableOffset;

    private boolean inCurrentStart;
    private boolean inCurrentFeatureStart;
    private boolean inCurrentPropertyStart;
    private boolean inCurrentPropertyText;
    private boolean inCoordinates;
    private ImmutableCoordinatesTransformer.Builder coordinatesTransformerBuilder;
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
        this.namespaces.addNamespace("sf", "http://www.opengis.net/ogcapi-features-1/1.0/sf", true);
        this.namespaces.addNamespace("ogcapi", "http://www.opengis.net/ogcapi-features-1/1.0", true);
        this.namespaces.addNamespace("atom", "http://www.w3.org/2005/Atom", true);
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched,
                        Map<String, String> additionalInfos) throws Exception {
        writer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        if (isFeatureCollection) {
            writer.append("\n<sf:FeatureCollection");
            namespaces.getNamespaces()
                      .keySet()
                      .forEach(consumerMayThrow(prefix -> {
                          if (!Strings.isNullOrEmpty(prefix)) {
                              writer.append(" ");
                              writer.append(namespaces.generateNamespaceDeclaration(prefix));
                          }
                      }));

            isLastPage = numberReturned.orElse(0) < pageSize;
            inCurrentStart = true;

            additionalInfos.forEach(biConsumerMayThrow(this::onGmlAttribute));
        }
    }

    @Override
    public void onEnd() throws Exception {
        if (isFeatureCollection) {
            if (inCurrentStart) {
                writer.append(">");

                inCurrentStart = false;
            }
            writer.append("\n</sf:FeatureCollection>");
        }
        writer.flush();
        writer.close();
    }

    @Override
    public void onFeatureStart(List<String> path, Map<String, String> additionalInfos) throws Exception {
        LOGGER.debug("{}", path);

        if (inCurrentStart) {
            writer.append(">");

            inCurrentStart = false;
        }

        if (isFeatureCollection) {
            writer.append("\n<sf:featureMember>");
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

        additionalInfos.forEach(biConsumerMayThrow(this::onGmlAttribute));
    }

    @Override
    public void onFeatureEnd(List<String> path) throws Exception {
        writer.append("\n</");
        writer.append(namespaces.getNamespacePrefix(getNamespaceUri(path)));
        writer.append(":");
        writer.append(getLocalName(path));
        writer.append(">");
        if (isFeatureCollection) {
            writer.append("\n</sf:featureMember>");
        }
        writer.flush();
    }

    private void onGmlAttribute(String name, String value) throws Exception {
        onGmlAttribute(getNamespaceUri(name), getLocalName(name), ImmutableList.of(), value, ImmutableList.of());
    }

    //@Override
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
                newValue = crsTransformer.getTargetCrs().toUriString();
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
    public void onPropertyStart(List<String> path, List<Integer> multiplicities,
                                Map<String, String> additionalInfos) throws Exception {
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

        additionalInfos.forEach(biConsumerMayThrow(this::onGmlAttribute));
    }

    @Override
    public void onPropertyText(String text) throws Exception {
        if (inCurrentPropertyStart) {
            writer.append(">");
            inCurrentPropertyStart = false;
        }

        if (inCoordinates) {
            coordinatesTransformerBuilder = ImmutableCoordinatesTransformer.builder();
            coordinatesTransformerBuilder.coordinatesWriter(ImmutableCoordinatesWriterGml.of(writer, Optional.ofNullable(currentDimension).orElse(2)));

            if (crsTransformer != null) {
                coordinatesTransformerBuilder.crsTransformer(crsTransformer);
            }

            if (currentDimension != null) {
                coordinatesTransformerBuilder.sourceDimension(currentDimension);
                coordinatesTransformerBuilder.targetDimension(currentDimension);
            }

            if (maxAllowableOffset > 0) {
                coordinatesTransformerBuilder.maxAllowableOffset(maxAllowableOffset);
            }

            Writer coordinatesWriter = coordinatesTransformerBuilder.build();
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

    /*@Override
    public void onNamespaceRewrite(QName featureType, String namespace) throws Exception {

    }*/

    private String adjustSchemaLocation(String schemaLocation) {
        List<String> split = Splitter.on(' ').splitToList(schemaLocation);
        Map<String,String> locations = new LinkedHashMap<>();

        for (int i = 0; i < split.size()-1; i += 2) {
            if (!split.get(i).startsWith("http://www.opengis.net/ogcapi-features-1")) {
                locations.put(split.get(i), split.get(i + 1));
            }
        }

        locations.put("http://www.opengis.net/ogcapi-features-1/1.0/sf", "http://schemas.opengis.net/ogcapi/features/part1/1.0/xml/core-sf.xsd");
        locations.put("http://www.opengis.net/ogcapi-features-1/1.0", "http://schemas.opengis.net/ogcapi/features/part1/1.0/xml/core.xsd");
        locations.put("http://www.w3.org/2005/Atom", "http://schemas.opengis.net/kml/2.3/atom-author-link.xsd");


        return locations.entrySet().stream().map(entry -> entry.getKey() + " " + entry.getValue()).collect(Collectors.joining(" "));
    }


    private String getLocalName(List<String> path) {
        return path.isEmpty() ? null : getLocalName(path.get(path.size()-1));
    }

    private String getLocalName(String name) {
        return name.substring(name.lastIndexOf(":") + 1);
    }

    private String getNamespaceUri(List<String> path) {
        return path.isEmpty() ? null : getNamespaceUri(path.get(path.size()-1));
    }

    private String getNamespaceUri(String name) {
        return name.substring(0, name.lastIndexOf(":"));
    }
}
