/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.util.StaxUtil;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class NamespaceXmlFactory extends XmlFactory {

  private final String defaultNamespace;
  private final Map<String, String> otherNamespaces;

  public NamespaceXmlFactory(String defaultNamespace, Map<String, String> otherNamespaces) {
    this.defaultNamespace = Objects.requireNonNull(defaultNamespace);
    this.otherNamespaces = Objects.requireNonNull(otherNamespaces);
  }

  @Override
  protected XMLStreamWriter _createXmlWriter(IOContext ctxt, Writer w) throws IOException {
    XMLStreamWriter writer = super._createXmlWriter(ctxt, w);
    try {
      writer.setDefaultNamespace(defaultNamespace);
      for (Map.Entry<String, String> e : otherNamespaces.entrySet()) {
        writer.setPrefix(e.getKey(), e.getValue());
      }
    } catch (XMLStreamException e) {
      StaxUtil.throwAsGenerationException(e, null);
    }
    return writer;
  }
}
