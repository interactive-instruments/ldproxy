/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.codelists;

import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.google.common.net.UrlEscapers;
import de.ii.xtraplatform.akka.http.Http;
import de.ii.xtraplatform.akka.http.HttpClient;
import de.ii.xtraplatform.ogc.parser.GMLDictionaryAnalyzer;
import de.ii.xtraplatform.ogc.parser.GMLDictionaryParser;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMInputCursor;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
@Provides
@Instantiate
public class CodelistImporterGmlDictionary implements CodelistImporter {

    private final HttpClient httpClient;
    private final SMInputFactory staxFactory;

    public CodelistImporterGmlDictionary(@Requires Http http) {

        this.httpClient = http.getDefaultClient();
        this.staxFactory = new SMInputFactory(new InputFactoryImpl());
    }

    @Override
    public Class<CodelistData> getType() {
        return CodelistData.class;
    }

    @Override
    public CodelistData.IMPORT_TYPE getSourceType() {
        return CodelistData.IMPORT_TYPE.GML_DICTIONARY;
    }

    @Override
    public CodelistData generate(Map<String, String> partialData) {
        if (!Objects.equals(getSourceType().toString(), partialData.get("sourceType")) || !partialData.containsKey("sourceUrl")) {
            throw new IllegalArgumentException();
        }

        String sourceUrl = partialData.get("sourceUrl");

        ImmutableCodelistData.Builder codelistBuilder = new ImmutableCodelistData.Builder()
    .sourceType(getSourceType())
                .sourceUrl(sourceUrl);

        CodelistFromGMLDictionary analyzer = new CodelistFromGMLDictionary(codelistBuilder);
        GMLDictionaryParser gmlDictionaryParser = new GMLDictionaryParser(analyzer, staxFactory);

        InputStream response = httpClient.getAsInputStream(sourceUrl);
        gmlDictionaryParser.parse(response);

        return codelistBuilder.build();
    }


    static class CodelistFromGMLDictionary implements GMLDictionaryAnalyzer {
        private final ImmutableCodelistData.Builder codelistBuilder;
        private String prefix;
        private String currentEntryId;
        private final Map<String,String> entries;

        CodelistFromGMLDictionary(ImmutableCodelistData.Builder codelistBuilder) {
            this.codelistBuilder = codelistBuilder;
            this.entries = new LinkedHashMap<>();
        }

        @Override
        public void analyzeStart(SMInputCursor searchResults) {

        }

        @Override
        public void analyzeEnd() {
            codelistBuilder.putAllEntries(entries);
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
            codelistBuilder.id(UrlEscapers.urlFormParameterEscaper().escape(identifier));
        }

        @Override
        public void analyzeName(String name) {
            codelistBuilder.label(name);
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
            if (!entries.containsKey(currentEntryId)) {
                entries.put(currentEntryId, name);
            }
        }

        @Override
        public void analyzeEntryDescription(String description) {

        }
    }
}
