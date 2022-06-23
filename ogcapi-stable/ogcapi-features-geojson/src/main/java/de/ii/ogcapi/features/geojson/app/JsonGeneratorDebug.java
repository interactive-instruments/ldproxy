/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.app;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fischer
 */
public class JsonGeneratorDebug extends JsonGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonGeneratorDebug.class);
  private final JsonGenerator jsonGenerator;

  public JsonGeneratorDebug(JsonGenerator jsonGenerator) {
    this.jsonGenerator = jsonGenerator;
  }

  @Override
  public void setSchema(FormatSchema schema) {
    jsonGenerator.setSchema(schema);
  }

  @Override
  public FormatSchema getSchema() {
    return jsonGenerator.getSchema();
  }

  @Override
  public boolean canUseSchema(FormatSchema schema) {
    return jsonGenerator.canUseSchema(schema);
  }

  @Override
  public Version version() {
    return jsonGenerator.version();
  }

  @Override
  public Object getOutputTarget() {
    return jsonGenerator.getOutputTarget();
  }

  @Override
  public JsonGenerator setRootValueSeparator(SerializableString sep) {
    return jsonGenerator.setRootValueSeparator(sep);
  }

  @Override
  public JsonGenerator enable(Feature ftr) {
    return jsonGenerator.enable(ftr);
  }

  @Override
  public JsonGenerator disable(Feature ftr) {
    return jsonGenerator.disable(ftr);
  }

  @Override
  public boolean isEnabled(Feature ftr) {
    return jsonGenerator.isEnabled(ftr);
  }

  @Override
  public JsonGenerator setCodec(ObjectCodec oc) {
    return jsonGenerator.setCodec(oc);
  }

  @Override
  public ObjectCodec getCodec() {
    return jsonGenerator.getCodec();
  }

  @Override
  public JsonGenerator setPrettyPrinter(PrettyPrinter pp) {
    return jsonGenerator.setPrettyPrinter(pp);
  }

  @Override
  public PrettyPrinter getPrettyPrinter() {
    return jsonGenerator.getPrettyPrinter();
  }

  @Override
  public JsonGenerator useDefaultPrettyPrinter() {
    return jsonGenerator.useDefaultPrettyPrinter();
  }

  @Override
  public JsonGenerator setHighestNonEscapedChar(int i) {
    return jsonGenerator.setHighestNonEscapedChar(i);
  }

  @Override
  public int getHighestEscapedChar() {
    return jsonGenerator.getHighestEscapedChar();
  }

  @Override
  public CharacterEscapes getCharacterEscapes() {
    return jsonGenerator.getCharacterEscapes();
  }

  @Override
  public JsonGenerator setCharacterEscapes(CharacterEscapes ce) {
    return jsonGenerator.setCharacterEscapes(ce);
  }

  @Override
  public void writeStartArray() throws IOException, JsonGenerationException {
    LOGGER.debug("[");
    jsonGenerator.writeStartArray();
  }

  @Override
  public void writeEndArray() throws IOException, JsonGenerationException {
    LOGGER.debug("]");
    jsonGenerator.writeEndArray();
  }

  @Override
  public void writeStartObject() throws IOException, JsonGenerationException {
    LOGGER.debug("{");
    jsonGenerator.writeStartObject();
  }

  @Override
  public void writeEndObject() throws IOException, JsonGenerationException {
    LOGGER.debug("}");
    jsonGenerator.writeEndObject();
  }

  @Override
  public void writeFieldName(String string) throws IOException, JsonGenerationException {
    LOGGER.debug(string + " :");
    jsonGenerator.writeFieldName(string);
  }

  @Override
  public void writeFieldName(SerializableString ss) throws IOException, JsonGenerationException {
    LOGGER.debug(ss.toString());
    jsonGenerator.writeFieldName(ss);
  }

  @Override
  public void writeString(String string) throws IOException, JsonGenerationException {
    LOGGER.debug("= " + string);
    jsonGenerator.writeString(string);
  }

  @Override
  public void writeString(char[] chars, int i, int i1) throws IOException, JsonGenerationException {
    jsonGenerator.writeString(chars, i, i1);
  }

  @Override
  public void writeString(SerializableString ss) throws IOException, JsonGenerationException {
    jsonGenerator.writeString(ss);
  }

  @Override
  public void writeRawUTF8String(byte[] bytes, int i, int i1)
      throws IOException, JsonGenerationException {
    jsonGenerator.writeRawUTF8String(bytes, i, i1);
  }

  @Override
  public void writeUTF8String(byte[] bytes, int i, int i1)
      throws IOException, JsonGenerationException {
    jsonGenerator.writeUTF8String(bytes, i, i1);
  }

  @Override
  public void writeRaw(String string) throws IOException, JsonGenerationException {
    LOGGER.debug(string);
    jsonGenerator.writeRaw(string);
  }

  @Override
  public void writeRaw(String string, int i, int i1) throws IOException, JsonGenerationException {
    LOGGER.debug(string.substring(i, i1));
    jsonGenerator.writeRaw(string, i, i1);
  }

  @Override
  public void writeRaw(char[] chars, int i, int i1) throws IOException, JsonGenerationException {
    LOGGER.debug(new String(chars, i, i1));
    jsonGenerator.writeRaw(chars, i, i1);
  }

  @Override
  public void writeRaw(char c) throws IOException, JsonGenerationException {
    LOGGER.debug(String.valueOf(c));
    jsonGenerator.writeRaw(c);
  }

  @Override
  public void writeRaw(SerializableString ss) throws IOException, JsonGenerationException {
    jsonGenerator.writeRaw(ss);
  }

  @Override
  public void writeRawValue(String string) throws IOException, JsonGenerationException {
    LOGGER.debug(string);
    jsonGenerator.writeRawValue(string);
  }

  @Override
  public void writeRawValue(String string, int i, int i1)
      throws IOException, JsonGenerationException {
    LOGGER.debug(string.substring(i, i1));
    jsonGenerator.writeRawValue(string, i, i1);
  }

  @Override
  public void writeRawValue(char[] chars, int i, int i1)
      throws IOException, JsonGenerationException {
    LOGGER.debug(new String(chars, i, i1));
    jsonGenerator.writeRawValue(chars, i, i1);
  }

  @Override
  public void writeBinary(Base64Variant bv, byte[] bytes, int i, int i1)
      throws IOException, JsonGenerationException {
    jsonGenerator.writeBinary(bv, bytes, i, i1);
  }

  @Override
  public void writeBinary(byte[] bytes, int i, int i1) throws IOException, JsonGenerationException {
    jsonGenerator.writeBinary(bytes, i, i1);
  }

  @Override
  public void writeBinary(byte[] bytes) throws IOException, JsonGenerationException {
    jsonGenerator.writeBinary(bytes);
  }

  @Override
  public int writeBinary(InputStream in, int i) throws IOException, JsonGenerationException {
    return jsonGenerator.writeBinary(in, i);
  }

  @Override
  public int writeBinary(Base64Variant bv, InputStream in, int i)
      throws IOException, JsonGenerationException {
    return jsonGenerator.writeBinary(bv, in, i);
  }

  @Override
  public void writeNumber(int i) throws IOException, JsonGenerationException {
    LOGGER.debug(String.valueOf(i));
    jsonGenerator.writeNumber(i);
  }

  @Override
  public void writeNumber(long l) throws IOException, JsonGenerationException {
    LOGGER.debug(String.valueOf(l));
    jsonGenerator.writeNumber(l);
  }

  @Override
  public void writeNumber(BigInteger bi) throws IOException, JsonGenerationException {
    LOGGER.debug(String.valueOf(bi));
    jsonGenerator.writeNumber(bi);
  }

  @Override
  public void writeNumber(double d) throws IOException, JsonGenerationException {
    LOGGER.debug(String.valueOf(d));
    jsonGenerator.writeNumber(d);
  }

  @Override
  public void writeNumber(float f) throws IOException, JsonGenerationException {
    LOGGER.debug(String.valueOf(f));
    jsonGenerator.writeNumber(f);
  }

  @Override
  public void writeNumber(BigDecimal bd) throws IOException, JsonGenerationException {
    LOGGER.debug(String.valueOf(bd));
    jsonGenerator.writeNumber(bd);
  }

  @Override
  public void writeNumber(String string)
      throws IOException, JsonGenerationException, UnsupportedOperationException {
    LOGGER.debug(string);
    jsonGenerator.writeNumber(string);
  }

  @Override
  public void writeBoolean(boolean bln) throws IOException, JsonGenerationException {
    LOGGER.debug(String.valueOf(bln));
    jsonGenerator.writeBoolean(bln);
  }

  @Override
  public void writeNull() throws IOException, JsonGenerationException {
    LOGGER.debug("NULL");
    jsonGenerator.writeNull();
  }

  @Override
  public void writeObject(Object o) throws IOException, JsonProcessingException {
    jsonGenerator.writeObject(o);
  }

  @Override
  public void writeStringField(String string, String string1)
      throws IOException, JsonGenerationException {
    LOGGER.debug(string + " : " + string1);
    jsonGenerator.writeStringField(string, string1);
  }

  @Override
  public void writeTree(TreeNode tn) throws IOException, JsonProcessingException {
    jsonGenerator.writeTree(tn);
  }

  @Override
  public void copyCurrentEvent(JsonParser jp) throws IOException, JsonProcessingException {
    jsonGenerator.copyCurrentEvent(jp);
  }

  @Override
  public void copyCurrentStructure(JsonParser jp) throws IOException, JsonProcessingException {
    jsonGenerator.copyCurrentStructure(jp);
  }

  @Override
  public JsonStreamContext getOutputContext() {
    return jsonGenerator.getOutputContext();
  }

  @Override
  public void flush() throws IOException {
    jsonGenerator.flush();
  }

  @Override
  public boolean isClosed() {
    return jsonGenerator.isClosed();
  }

  @Override
  public void close() throws IOException {
    jsonGenerator.close();
  }

  @Override
  public int getFeatureMask() {
    return jsonGenerator.getFeatureMask();
  }

  @Override
  public JsonGenerator setFeatureMask(int i) {
    return jsonGenerator.setFeatureMask(i);
  }
}
