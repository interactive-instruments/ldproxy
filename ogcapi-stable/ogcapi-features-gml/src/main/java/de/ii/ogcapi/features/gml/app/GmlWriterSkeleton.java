/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.app;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.consumerMayThrow;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.gml.domain.EncodingAwareContextGml;
import de.ii.ogcapi.features.gml.domain.GmlWriter;
import de.ii.xtraplatform.features.gml.domain.XMLNamespaceNormalizer;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class GmlWriterSkeleton implements GmlWriter {

  private static final String XML_PROLOG = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";

  @Inject
  public GmlWriterSkeleton() {}

  @Override
  public GmlWriterSkeleton create() {
    return new GmlWriterSkeleton();
  }

  @Override
  public int getSortPriority() {
    return 0;
  }

  @Override
  public void onStart(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {

    context.encoding().write(XML_PROLOG);

    if (context.encoding().isFeatureCollection()) {
      try {
        String rootElement = getFeatureCollectionTag(context);
        context.encoding().write("\n<");
        context.encoding().write(rootElement);
        context.encoding().write(getNamespaceAttributes(context, rootElement));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    next.accept(context);

    if (context.encoding().isFeatureCollection()) {
      context.encoding().write(">");
    }

    context.encoding().flush();
  }

  @Override
  public void onEnd(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {

    // next chain for extensions
    next.accept(context);

    if (context.encoding().isFeatureCollection()) {
      String rootElement = getFeatureCollectionTag(context);
      context.encoding().write("\n</");
      context.encoding().write(rootElement);
      context.encoding().write(">");
    }

    context.encoding().flush();
  }

  @Override
  public void onFeatureStart(
      EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next) throws IOException {

    context.encoding().write("\n");
    if (context.encoding().isFeatureCollection()) {
      context.encoding().write("<");
      context.encoding().write(getFeatureMemberTag(context));
      context.encoding().write(">");
    }

    String elementName = context.encoding().startGmlObject(context.schema().orElseThrow());
    context.encoding().write("<");
    context.encoding().write(elementName);
    context.encoding().write(" gml:id=\"");
    context.encoding().writeGmlIdPlaceholder();
    context.encoding().write("\"");
    context.encoding().writeXmlAttPlaceholder();

    if (!context.encoding().isFeatureCollection()) {
      // add namespace information
      context.encoding().write(getNamespaceAttributes(context, elementName));
    }

    context.encoding().pushElement(elementName);

    // next chain for extensions
    next.accept(context);

    context.encoding().write(">");
  }

  @Override
  public void onFeatureEnd(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {

    // next chain for extensions
    next.accept(context);

    String elementName = context.encoding().popElement();

    context.encoding().write("\n</");
    context.encoding().write(elementName);
    context.encoding().write(">");

    if (context.encoding().isFeatureCollection()) {
      context.encoding().write("</");
      context.encoding().write(getFeatureMemberTag(context));
      context.encoding().write(">");
    }

    context.encoding().closeGmlObject();
    context.encoding().flush();
  }

  private String getFeatureCollectionTag(EncodingAwareContextGml context) {
    return context.encoding().getFeatureCollectionElementName().orElse("sf:FeatureCollection");
  }

  private String getFeatureMemberTag(EncodingAwareContextGml context) {
    return context.encoding().getFeatureMemberElementName().orElse("sf:featureMember");
  }

  private String getNamespaceAttributes(EncodingAwareContextGml context, String rootElement) {
    StringBuilder builder = new StringBuilder();

    // default namespace
    context
        .encoding()
        .getDefaultNamespace()
        .ifPresent(
            consumerMayThrow(
                nsPrefix -> {
                  String nsUri = context.encoding().getNamespaces().get(nsPrefix);
                  if (Objects.nonNull(nsUri)) {
                    builder.append(" xmlns=\"");
                    builder.append(nsUri);
                    builder.append("\"");
                  }
                }));

    // filter namespaces
    Map<String, String> effectiveNamespaces =
        context.encoding().getNamespaces().entrySet().stream()
            .filter(
                entry ->
                    (!"sf".equals(entry.getKey()) || rootElement.startsWith("sf:"))
                        && (!"wfs".equals(entry.getKey()) || rootElement.startsWith("wfs:")))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    XMLNamespaceNormalizer namespaceNormalizer = new XMLNamespaceNormalizer(effectiveNamespaces);

    effectiveNamespaces
        .keySet()
        .forEach(
            consumerMayThrow(
                prefix -> {
                  if (!Strings.isNullOrEmpty(prefix)) {
                    builder.append(" ");
                    builder.append(namespaceNormalizer.generateNamespaceDeclaration(prefix));
                  }
                }));

    Map<String, String> effectiveSchemaLocations =
        context.encoding().getSchemaLocations().entrySet().stream()
            .filter(entry -> effectiveNamespaces.containsKey(entry.getKey()))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    builder.append(" xsi:schemaLocation=\"");
    builder.append(
        effectiveSchemaLocations.entrySet().stream()
            .map(
                entry ->
                    effectiveNamespaces.get(entry.getKey())
                        + " "
                        + entry
                            .getValue()
                            .replace("{{serviceUrl}}", context.encoding().getServiceUrl()))
            .collect(Collectors.joining(" ")));
    builder.append("\"");
    return builder.toString();
  }
}