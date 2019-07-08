/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class QueryParamPagingFilter implements ContainerRequestFilter {

    private static final String RANGE_HEADER = "Range";
    private static final String RANGE_UNIT = "items";
    private static final String PAGE_PARAMETER = "page";
    private static final String COUNT_PARAMETER = "count";
    private static final String LIMIT_PARAMETER = "limit";
    private static final String START_INDEX_PARAMETER = "startIndex";
    private static final String OFFSET_PARAMETER = "offset";
    // TODO: get from config
    private static final int PAGE_SIZE = 10;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        int page = 0;
        int pageSize = PAGE_SIZE;
        int from = page * pageSize;
        int to = from + pageSize;


        if (requestContext.getUriInfo().getQueryParameters().containsKey(PAGE_PARAMETER)) {
            try {
                page = Integer.parseInt(requestContext.getUriInfo().getQueryParameters().getFirst(PAGE_PARAMETER)) - 1;
                from = page * pageSize;
                to = from + pageSize;
            } catch (NumberFormatException ex) {
                // ignore
            }
        }

        if (requestContext.getUriInfo().getQueryParameters().containsKey(COUNT_PARAMETER)) {
            try {
                pageSize = Integer.parseInt(requestContext.getUriInfo().getQueryParameters().getFirst(COUNT_PARAMETER));
                from = page * pageSize;
                to = from + pageSize;
            } catch (NumberFormatException ex) {
                // ignore
            }
        }

        if (requestContext.getUriInfo().getQueryParameters().containsKey(LIMIT_PARAMETER)) {
            try {
                pageSize = Integer.parseInt(requestContext.getUriInfo().getQueryParameters().getFirst(LIMIT_PARAMETER));
                from = page * pageSize;
                to = from + pageSize;
            } catch (NumberFormatException ex) {
                // ignore
            }
        }

        if (requestContext.getUriInfo().getQueryParameters().containsKey(START_INDEX_PARAMETER)) {
            try {
                from = Integer.parseInt(requestContext.getUriInfo().getQueryParameters().getFirst(START_INDEX_PARAMETER));
                to = from + pageSize;
            } catch (NumberFormatException ex) {
                // ignore
            }
        }

        if (requestContext.getUriInfo().getQueryParameters().containsKey(OFFSET_PARAMETER)) {
            try {
                from = Integer.parseInt(requestContext.getUriInfo().getQueryParameters().getFirst(OFFSET_PARAMETER));
                to = from + pageSize;
            } catch (NumberFormatException ex) {
                // ignore
            }
        }

        requestContext.getHeaders().putSingle(RANGE_HEADER, getRange(from, to));
    }

    private static String getRange(int from, int to) {
        return new StringBuilder()
                .append(RANGE_UNIT)
                .append('=')
                .append(from)
                .append('-')
                .append(to)
                .toString();
    }
}