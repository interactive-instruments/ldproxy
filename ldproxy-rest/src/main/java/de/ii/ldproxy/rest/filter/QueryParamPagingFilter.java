/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

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
    // TODO: get from config
    private static final int PAGE_SIZE = 25;

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        int page = 0;

        if (request.getQueryParameters().containsKey(PAGE_PARAMETER)) {
            try {
                page = Integer.parseInt(request.getQueryParameters().getFirst(PAGE_PARAMETER)) - 1;
            } catch (NumberFormatException ex) {
                // ignore
            }
        }

        int from = page * PAGE_SIZE;
        int to = from + PAGE_SIZE;

        request.getRequestHeaders().putSingle(RANGE_HEADER, getRange(from, to));

        return request;
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