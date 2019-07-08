/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * @author zahnen
 */
public class RangeHeader {

    public static final String RANGE_ITEMS = "items";

    static public int[] parseRange(final String range) {
        // TODO: get from config
        int PAGE_SIZE = 10;
        int from = 0;
        int to = PAGE_SIZE;
        int page = 1;
        int pageSize = PAGE_SIZE;

        if (range != null) {
            try {
                String[] ranges = range.split("=")[1].split("-");
                from = Integer.parseInt(ranges[0]);
                to = Integer.parseInt(ranges[1]);
                pageSize = to-from;
                page = to / pageSize;
            } catch (NumberFormatException ex) {
                //ignore
            }
        }
        int[] countFrom = {to-from, from, page, pageSize};

        return countFrom;

        // TODO
        // Accept-Ranges: items
        // 206 Partial Content
        // 416 Range Not Satisfiable


    }

    static public ResponseBuilder writeRange(ResponseBuilder response, int[] range, int numberReturned, int numberMatched) {
        // TODO: get from wfs response
        String responseRange;
        if (numberMatched == -1) {
            responseRange = String.format("%s=%d-%d", RANGE_ITEMS, range[1], range[1] + numberReturned);
        } else {
            responseRange = String.format("%s=%d-%d/%d", RANGE_ITEMS, range[1], range[1] + numberReturned, numberMatched);
        }

        return response
                .header("Accept-Ranges", RANGE_ITEMS)
                .header("Content-Range", responseRange);

    }

    /*public static class RangeWriter implements GMLAnalyzer {

        private ResponseBuilder responseBuilder;
        private int[] range;

        public RangeWriter(ResponseBuilder responseBuilder, String range) {
            this.responseBuilder = responseBuilder;
            this.range = parseRange(range);
        }

        @Override
        public void analyzeStart(Future<SMInputCursor> rootFuture) {
            try {
                SMInputCursor cursor = rootFuture.get();

                int numberMatched = -1;
                try {
                    numberMatched = Integer.parseInt(cursor.getAttrValue("numberMatched"));
                } catch (NumberFormatException e) {
                    // ignore
                }
                int numberReturned = Integer.parseInt(cursor.getAttrValue("numberReturned"));

                writeRange(responseBuilder, range, numberReturned, numberMatched);

            } catch (InterruptedException | ExecutionException | NumberFormatException | XMLStreamException e) {
                throw new ServerErrorException("The WFS did not respond properly to the request.", Response.Status.BAD_GATEWAY);
            }
        }

        @Override
        public void analyzeEnd() {

        }

        @Override
        public void analyzeFeatureStart(String id, String nsuri, String localName) {

        }

        @Override
        public void analyzeFeatureEnd() {

        }

        @Override
        public void analyzeAttribute(String nsuri, String localName, String value) {

        }

        @Override
        public void analyzePropertyStart(String nsuri, String localName, int depth, SMInputCursor feature, boolean nil) {

        }

        @Override
        public void analyzePropertyText(String nsuri, String localName, int depth, String text) {

        }

        @Override
        public void analyzePropertyEnd(String nsuri, String localName, int depth) {

        }

        @Override
        public void analyzeFailed(Exception ex) {

        }

        @Override
        public boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName) {
            return false;
        }
    }*/
}
