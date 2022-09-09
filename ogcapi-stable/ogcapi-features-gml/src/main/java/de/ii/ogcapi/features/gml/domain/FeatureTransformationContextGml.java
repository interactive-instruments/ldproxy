/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ogcapi.features.gml.domain.GmlConfiguration.GmlVersion;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import org.immutables.value.Value;

@SuppressWarnings({
  "ConstantConditions",
  "PMD.TooManyMethods"
}) // this class needs that many methods, a refactoring makes no sense
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class FeatureTransformationContextGml implements FeatureTransformationContext {

  private static final String XML_ATTRIBUTE_PLACEHOLDER = "_$$_XML_ATTRIBUTE_i_$$_";
  private static final String GML_ID_PLACEHOLDER = "_$$_GML_ID_i_$$_";
  private static final String OBJECT_ELEMENT_PLACEHOLDER = "_$$_OBJECT_ELEMENT_i_$$_";
  private static final String SURFACE_MEMBER_PLACEHOLDER = "_$$_SURFACE_MEMBER_i_$$_";

  /**
   * Internal string buffer to buffer information. The buffer is flushed for every feature. This
   * would belong to State, but we cannot move it, because Modifiable has no support for appending
   * to a StringBuilder
   */
  @SuppressWarnings({
    "PMD.AvoidStringBufferField"
  }) // memory leak is not a risk and we need to incrementally build a string
  private final StringBuilder buffer = new StringBuilder();

  @Override
  @Value.Default
  // This is never null, but the parent is marked as nullable.
  // Warnings about potential NullPointerExceptions can be ignored.
  public ModifiableStateGml getState() {
    return ModifiableStateGml.create();
  }

  public abstract GmlVersion getGmlVersion();

  public abstract Map<String, String> getNamespaces();

  public abstract Map<String, String> getSchemaLocations();

  public abstract Optional<String> getDefaultNamespace();

  public abstract Map<String, String> getObjectTypeNamespaces();

  public abstract Map<String, VariableName> getVariableObjectElementNames();

  public abstract Optional<String> getFeatureCollectionElementName();

  public abstract Optional<String> getFeatureMemberElementName();

  public abstract boolean getSupportsStandardResponseParameters();

  public abstract List<String> getXmlAttributes();

  public abstract Optional<String> getGmlIdPrefix();

  /**
   * Add a string to the response.
   *
   * @param value the string
   */
  public void write(String value) {
    buffer.append(value);
  }

  /**
   * Add a boolean to the response.
   *
   * @param value the boolean
   */
  public void write(boolean value) {
    buffer.append(value);
  }

  /**
   * Write the contents of the string buffer to response stream. Before this requires that we first
   * process all placeholders in the buffer. Placeholders are inserted when we have to write
   * content, but do not yet know it. There are three cases:
   *
   * <p>1. The gml:id of a feature is part of the feature element, but we only know the tag once we
   * have processed the properties.
   *
   * <p>2. Properties that are mapped to XML attributes have to be written to the parent GML object
   * element.
   *
   * <p>3. If the tag of a GML object element is determined from a property, we only know the tag
   * once we have processed the properties.
   *
   * @throws IOException the buffer could not be written to the stream
   */
  public void flush() throws IOException {
    if (buffer.length() > 0) {
      // replace placeholders
      getState()
          .getPlaceholders()
          .forEach(
              (key, value) -> {
                // variable object elements appear twice - opening and closing tag
                IntStream.rangeClosed(1, 2)
                    .forEach(
                        i -> {
                          int idx = buffer.lastIndexOf(key);
                          if (idx != -1) {
                            buffer.replace(idx, idx + key.length(), value);
                          }
                        });
              });

      if (buffer.lastIndexOf("_$$_") != -1) {
        throw new IllegalStateException(
            String.format("GML feature buffer contains unresolved placeholders: %s", buffer));
      }

      // write buffer and clear the buffer and the placeholder information
      getOutputStream().write(buffer.toString().getBytes());
      getState().unsetPlaceholders();
      buffer.setLength(0);
    }
  }

  /**
   * We need to store a stack of the element names to simplify writing the closing element tags. In
   * GML elements representing objects contain elements representing properties and those contain
   * either values or an object element.
   *
   * <p>This method adds new element names to the top of the stack.
   *
   * @param elementName one or more qualified element names that are added to the stack
   */
  public void pushElement(String... elementName) {
    getState().addElements(elementName);
  }

  /**
   * We need to store a stack of the element names to simplify writing the closing element tags. In
   * GML elements representing objects contain elements representing properties and those contain
   * either values or an object element.
   *
   * <p>This method removes the topmost element name from the stack and returns it.
   *
   * @return the element name that has been removed from the stack
   */
  @Value.Auxiliary
  public String popElement() {
    List<String> elements = getState().getElements();
    int idx = elements.size() - 1;
    String elementName = elements.get(idx);
    List<String> newList = ImmutableList.copyOf(elements.subList(0, idx));
    if (newList.isEmpty()) {
      getState().unsetElements();
    } else {
      getState().setElements(newList);
    }

    return elementName;
  }

  /**
   * Geometries are represented by coordinate arrays. We need to maintain a stack with the current
   * level of nesting and for each level the position in the coordinate arrays in order to create
   * the proper GML element names.
   *
   * <p>This method determines the current nesting level.
   *
   * @return the current level
   */
  @Value.Auxiliary
  public int getGeometryArrayLevel() {
    return getState().getCurrentGeometryNesting().size() - 1;
  }

  /**
   * Geometries are represented by coordinate arrays. We need to maintain a stack with the current
   * level of nesting and for each level the position in the coordinate arrays in order to create
   * the proper GML element names.
   *
   * <p>This method adds a new array level at the first position in the array on the new level.
   *
   * @return the new nesting level
   */
  @Value.Auxiliary
  public int openGeometryArray() {
    getState().addCurrentGeometryNesting(0);
    return getState().getCurrentGeometryNesting().size() - 1;
  }

  /**
   * Geometries are represented by coordinate arrays. We need to maintain a stack with the current
   * level of nesting and for each level the position in the coordinate arrays in order to create
   * the proper GML element names.
   *
   * <p>This method advances to the next item on the top array.
   *
   * @return the new position in the top array
   */
  public int nextGeometryItem() {
    List<Integer> nesting = getState().getCurrentGeometryNesting();
    assert !nesting.isEmpty();
    int idx = nesting.get(nesting.size() - 1) + 1;
    if (nesting.size() > 1) {
      getState()
          .setCurrentGeometryNesting(
              ImmutableList.<Integer>builder()
                  .addAll(ImmutableList.copyOf(nesting.subList(0, nesting.size() - 1)))
                  .add(idx)
                  .build());
    } else {
      getState().setCurrentGeometryNesting(ImmutableList.of(idx));
    }
    return idx;
  }

  /**
   * Geometries are represented by coordinate arrays. We need to maintain a stack with the current
   * level of nesting and for each level the position in the coordinate arrays in order to create
   * the proper GML element names.
   *
   * <p>This method removes the top array, the nesting level is reduced by one.
   *
   * @return the new nesting level
   */
  public int closeGeometryArray() {
    List<Integer> nesting = getState().getCurrentGeometryNesting();
    assert !nesting.isEmpty();
    int level = nesting.size() - 1;
    if (level > 0) {
      getState().setCurrentGeometryNesting(ImmutableList.copyOf(nesting.subList(0, level)));
    } else {
      getState().unsetCurrentGeometryNesting();
    }
    return level - 1;
  }

  /**
   * Geometries are represented by coordinate arrays. We need to maintain a stack with the current
   * level of nesting and for each level the position in the coordinate arrays in order to create
   * the proper GML element names.
   *
   * <p>This method fetches the current position in the array at the specified level.
   *
   * @return the level of the array for which the current position is retrieved
   */
  public int getGeometryItem(int level) {
    List<Integer> nesting = getState().getCurrentGeometryNesting();
    assert nesting.size() > level;
    return nesting.get(level);
  }

  /**
   * Properties can be mapped to XML attributes of the parent GML object. Since GML object elements
   * are nested, we need to maintain a stack with the current GML object elements.
   *
   * <p>This method writes a unique placeholder for the XML attributes so that the XML attributes
   * can be added later when the object properties are processed. This method has to be called at
   * the end of the opening tag of the GML object element.
   */
  @Value.Auxiliary
  public void writeXmlAttPlaceholder() {
    int i = getState().getLastObject();
    String xmlAttPlaceholder = XML_ATTRIBUTE_PLACEHOLDER.replace("i", String.valueOf(i));
    write(xmlAttPlaceholder);
    getState().putPlaceholders(xmlAttPlaceholder, "");
  }

  /**
   * Properties can be mapped to XML attributes of the parent GML object. Since GML object elements
   * are nested, we need to maintain a stack with the current GML object elements.
   *
   * <p>This method writes an XML attribute to the current GML object element.
   *
   * @param name the name of the XML attribute
   * @param value the value of the XML attribute
   */
  public void writeAsXmlAtt(String name, String value) {
    // get the current object index to determine the placeholder
    List<Integer> indices = getState().getObjects();
    int idx = indices.get(indices.size() - 1);
    String xmlAttPlaceholder = XML_ATTRIBUTE_PLACEHOLDER.replace("i", String.valueOf(idx));
    String current = getState().getPlaceholders().get(xmlAttPlaceholder);
    if (Objects.isNull(current)) {
      throw new IllegalStateException(
          String.format("Placeholder '%s' not set in GML output.", xmlAttPlaceholder));
    }
    getState().putPlaceholders(xmlAttPlaceholder, current + " " + name + "=\"" + value + "\"");
  }

  /**
   * The gml:id of a feature is part of the feature element, but we only know the tag once we have
   * processed the properties.
   *
   * <p>This method writes a unique placeholder for the value of the gml:id attribute so that it can
   * be added later when the ID property is processed.
   */
  @Value.Auxiliary
  public void writeGmlIdPlaceholder() {
    int i = getState().getLastObject();
    write(GML_ID_PLACEHOLDER.replace("i", String.valueOf(i)));
  }

  /**
   * The gml:id of a feature is part of the feature element, but we only know the tag once we have
   * processed the properties.
   *
   * <p>This method writes the gml:id attribute to the current GML feature element.
   *
   * @param value the value of the ID property of the current feature
   */
  public void setCurrentGmlId(String value) {
    int i = getState().getLastObject();
    getState().putPlaceholders(GML_ID_PLACEHOLDER.replace("i", String.valueOf(i)), value);
  }

  /** Get the gml:id of the current feature. */
  @Value.Auxiliary
  public String getCurrentGmlId() {
    int i = getState().getObjects().get(0);
    return getState().getPlaceholders().get(GML_ID_PLACEHOLDER.replace("i", String.valueOf(i)));
  }

  /**
   * The name of a GML object element may depend on some property value. Since GML object elements
   * are nested, we need to maintain a stack with the current GML object elements.
   *
   * <p>This method adds a new GML object element for the current object to the stack.
   *
   * <p>If the element has a fixed name that name is used, otherwise a unique placeholder for the
   * element name is written, so that the element name can be updated later when the object
   * properties are processed.
   *
   * @param schema the OBJECT schema that is mapped to a GML object
   */
  public String startGmlObject(FeatureSchema schema) {
    String objectType = schema.getObjectType().orElse("FIX:ME");
    VariableName variableName = getVariableObjectElementNames().get(objectType);

    int idx = getState().getLastObject() + 1;
    getState().setLastObject(idx);
    getState().addObjects(idx);

    String elementName;
    if (Objects.nonNull(variableName)) {
      int i = getState().getLastObject();
      elementName = OBJECT_ELEMENT_PLACEHOLDER.replace("i", String.valueOf(i));
      getState().setVariableNameProperty(variableName.getProperty());
      getState().putAllVariableNameMapping(variableName.getMapping());
    } else {
      Optional<String> nsPrefix = Optional.ofNullable(getObjectTypeNamespaces().get(objectType));
      elementName = nsPrefix.isEmpty() ? objectType : nsPrefix.get() + ":" + objectType;
      getState().setVariableNameProperty(Optional.empty());
      getState().unsetVariableNameMapping();
    }
    return elementName;
  }

  /**
   * The name of a GML object element may depend on some property value. Since GML object elements
   * are nested, we need to maintain a stack with the current GML object elements.
   *
   * <p>This method removes the innermost GML object element from the stack.
   */
  @Value.Auxiliary
  public void closeGmlObject() {
    List<Integer> indices = getState().getObjects();
    int idx = indices.size() - 1;
    List<Integer> newList = ImmutableList.copyOf(indices.subList(0, idx));
    if (newList.isEmpty()) {
      getState().unsetObjects();
    } else {
      getState().setObjects(newList);
    }
  }

  /**
   * The name of a GML object element may depend on some property value. Since GML object elements
   * are nested, we need to maintain a stack with the current GML object elements.
   *
   * <p>This method replaces the placeholder element name with the proper element name.
   *
   * @param value the qualified element name
   */
  public void setCurrentObjectElement(String value) {
    int i = getState().getLastObject();
    getState().putPlaceholders(OBJECT_ELEMENT_PLACEHOLDER.replace("i", String.valueOf(i)), value);
  }

  /**
   * This capability is specific to CityGML LoD2 buildings.
   *
   * <p>The gml:ids of the gml:Polygon elements that make up the shell of the LoD 2 solid and that
   * must be referenced from the gml:Solid geometry will only be known when those polygons are
   * written as part of the boundary surfaces.
   *
   * <p>This method writes a unique placeholder for the gml:surfaceMember elements so that they can
   * be added later when the polygons are processed.
   */
  @Value.Auxiliary
  public void writeSurfaceMemberPlaceholder() {
    int i = getState().getLastObject();
    String surfaceMemberPlaceholder = SURFACE_MEMBER_PLACEHOLDER.replace("i", String.valueOf(i));
    write(surfaceMemberPlaceholder);
    getState().putPlaceholders(surfaceMemberPlaceholder, "");
  }

  /**
   * This capability is specific to CityGML LoD2 buildings.
   *
   * <p>The gml:ids of the gml:Polygon elements that make up the shell of the LoD 2 solid and that
   * must be referenced from the gml:Solid geometry will only be known when those polygons are
   * written as part of the boundary surfaces.
   *
   * <p>This method adds another gml:surfaceMember element to the placeholder.
   */
  public void writeAsSurfaceMemberLink(String elementName, String polygonId) {
    // get the current object index to determine the placeholder
    List<Integer> indices = getState().getObjects();
    int idx = indices.get(0);
    String surfaceMemberPlaceholder = SURFACE_MEMBER_PLACEHOLDER.replace("i", String.valueOf(idx));
    String current = getState().getPlaceholders().get(surfaceMemberPlaceholder);
    if (Objects.isNull(current)) {
      throw new IllegalStateException(
          String.format("Placeholder '%s' not set in GML output.", surfaceMemberPlaceholder));
    }
    getState()
        .putPlaceholders(
            surfaceMemberPlaceholder,
            current + "<" + elementName + " xlink:href=\"#" + polygonId + "\"/>");
  }

  @Value.Derived
  @Value.Auxiliary
  public String getGmlPrefix() {
    final GmlVersion v = getGmlVersion();
    if (v.equals(GmlVersion.GML21)) {
      return "gml21";
    }
    if (v.equals(GmlVersion.GML31)) {
      return "gml31";
    }
    return "gml";
  }

  @Value.Modifiable
  public abstract static class StateGml extends State {

    @Value.Default
    public List<String> getElements() {
      return ImmutableList.of();
    }

    @Value.Default
    public boolean getInLink() {
      return false;
    }

    @Value.Default
    public boolean getInMeasure() {
      return false;
    }

    public abstract Optional<String> getFirstMeasureProperty();

    @Value.Default
    public boolean getInGeometry() {
      return false;
    }

    @Value.Default
    public boolean getCompositeGeometry() {
      return false;
    }

    @Value.Default
    public boolean getClosedGeometry() {
      return false;
    }

    @Value.Default
    public boolean getDeferredSolidGeometry() {
      return false;
    }

    @Value.Default
    public int getDeferredFeatureId() {
      return 0;
    }

    @Value.Default
    public int getDeferredPolygonId() {
      return 0;
    }

    public abstract Optional<SimpleFeatureGeometry> getCurrentGeometryType();

    @Value.Default
    public List<Integer> getCurrentGeometryNesting() {
      return ImmutableList.of();
    }

    @Value.Default
    public Map<String, String> getPlaceholders() {
      return ImmutableMap.of();
    }

    @Value.Default
    public List<Integer> getObjects() {
      return ImmutableList.of();
    }

    @Value.Default
    public int getLastObject() {
      return 0;
    }

    public abstract Optional<String> getVariableNameProperty();

    @Value.Default
    public Map<String, String> getVariableNameMapping() {
      return ImmutableMap.of();
    }
  }
}
