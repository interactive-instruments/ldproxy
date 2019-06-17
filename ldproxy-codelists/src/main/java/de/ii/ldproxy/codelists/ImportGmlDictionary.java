/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.codelists;

import com.google.common.net.UrlEscapers;
import de.ii.xtraplatform.ogc.parser.GMLDictionaryAnalyzer;
import de.ii.xtraplatform.ogc.parser.GMLDictionaryParser;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * @author zahnen
 */
public class ImportGmlDictionary implements GMLDictionaryAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportGmlDictionary.class);
    private GMLDictionaryParser gmlDictionaryParser;
    private CodelistOld codelist;
    private String prefix;
    private String currentEntryId;

    ImportGmlDictionary(SMInputFactory staxFactory) {
        this.gmlDictionaryParser = new GMLDictionaryParser(this, staxFactory);
    }

    public void parse(InputStream inputStream, CodelistOld codelist) {
        this.codelist = codelist;
        gmlDictionaryParser.parse(inputStream);
    }

    @Override
    public void analyzeStart(SMInputCursor searchResults) {

    }

    @Override
    public void analyzeEnd() {

    }

    @Override
    public void analyzeFailed(Exception ex) {

    }

    @Override
    public void analyzeFailed(String exceptionCode, String exceptionText) {

    }

    @Override
    public void analyzeNamespace(String prefix, String uri) {

    }

    @Override
    public void analyzeIdentifier(String identifier) {
        this.prefix = identifier;
        codelist.setResourceId(UrlEscapers.urlFormParameterEscaper().escape(identifier));
    }

    @Override
    public void analyzeName(String name) {
codelist.setName(name);
    }

    @Override
    public void analyzeEntryStart() {

    }

    @Override
    public void analyzeEntryEnd() {

    }

    @Override
    public void analyzeEntryIdentifier(String identifier) {
        this.currentEntryId = identifier.startsWith(prefix) ? identifier.substring(prefix.length()+1) : identifier;
    }

    @Override
    public void analyzeEntryName(String name) {
        if (!codelist.getEntries().containsKey(currentEntryId)) {
            codelist.getEntries().put(currentEntryId, name);
        }
    }

    @Override
    public void analyzeEntryDescription(String description) {

    }
}
