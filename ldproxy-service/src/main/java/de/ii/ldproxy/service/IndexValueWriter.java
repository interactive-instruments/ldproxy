package de.ii.ldproxy.service;

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLAnalyzer;
import org.codehaus.staxmate.in.SMInputCursor;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.xml.stream.XMLStreamException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author zahnen
 */
public class IndexValueWriter implements GMLAnalyzer {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(IndexValueWriter.class);

    private final String index;
    private final SortedSet<String> values;
    private int numberMatched;
    private boolean failed;

    public IndexValueWriter(String index) {
        this.index = index;
        this.values = new TreeSet<>();
        this.numberMatched = 0;
    }

    @Override
    public void analyzeStart(Future<SMInputCursor> future) {
        SMInputCursor cursor = null;
        try {
            cursor = future.get();

            numberMatched = Integer.parseInt(cursor.getAttrValue("numberMatched"));

        } catch (InterruptedException | ExecutionException | XMLStreamException | NumberFormatException e) {
            analyzeFailed(e);
        }
    }

    @Override
    public void analyzeEnd() {

    }

    @Override
    public void analyzeFeatureStart(String s, String s1, String s2) {

    }

    @Override
    public void analyzeFeatureEnd() {

    }

    @Override
    public void analyzeAttribute(String s, String s1, String s2) {

    }

    @Override
    public void analyzePropertyStart(String nsuri, String localName, int depth, SMInputCursor feature, boolean nil) {

        if (localName.equals(index)) {
            try {
                String value = feature.collectDescendantText();

                if (!value.isEmpty()) {
                    values.add(value);
                }
            } catch (XMLStreamException e) {
                analyzeFailed(e);
            }
        }
    }

    @Override
    public void analyzePropertyEnd(String s, String s1, int i) {

    }

    @Override
    public void analyzeFailed(Exception e) {
        this.failed = true;
        LOGGER.getLogger().debug("", e);
    }

    public SortedSet<String> getValues() {
        return values;
    }

    public int getNumberMatched() {
        return numberMatched;
    }

    public boolean hasFailed() {
        return failed;
    }
}
