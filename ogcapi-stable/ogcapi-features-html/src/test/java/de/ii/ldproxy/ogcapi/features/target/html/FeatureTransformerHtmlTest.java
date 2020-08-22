/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import de.ii.xtraplatform.stringtemplates.domain.StringTemplateFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * @author zahnen
 */
public class FeatureTransformerHtmlTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerHtmlTest.class);

    @Test
    public void testWriteField() {
    String input = "Abc|\n---|\n1|";
    String actual = StringTemplateFilters.applyFilterMarkdown(input);

    LOGGER.info(actual);
    }
}