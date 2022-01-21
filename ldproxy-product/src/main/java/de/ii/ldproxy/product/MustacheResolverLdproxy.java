/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.product;

import de.ii.xtraplatform.dropwizard.domain.PartialMustacheResolver;
import de.ii.xtraplatform.dropwizard.domain.PerClassMustacheResolver;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.Reader;

@Component
@Provides
@Instantiate
public class MustacheResolverLdproxy extends PerClassMustacheResolver implements PartialMustacheResolver {

    @Override
    public int getSortPriority() {
        return 900;
    }

    @Override
    public boolean canResolve(String templateName, Class<?> viewClass) {
        return super.canResolve(templateName, this.getClass());
    }

    @Override
    public Reader getReader(String templateName, Class<?> viewClass) {
        return super.getReader(templateName, this.getClass());
    }

}
