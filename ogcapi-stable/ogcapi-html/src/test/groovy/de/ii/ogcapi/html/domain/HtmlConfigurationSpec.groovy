/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.html.domain

import de.ii.ogcapi.foundation.domain.AbstractExtensionConfigurationSpec
import de.ii.ogcapi.foundation.domain.MergeBase
import de.ii.ogcapi.foundation.domain.MergeMinimal
import de.ii.ogcapi.foundation.domain.MergeSimple

@SuppressWarnings('ClashingTraitMethods')
class HtmlConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<HtmlConfiguration>, MergeMinimal<HtmlConfiguration>, MergeSimple<HtmlConfiguration> {
    @Override
    HtmlConfiguration getFull() {
        return new ImmutableHtmlConfiguration.Builder()
                .enabled(true)
                .noIndexEnabled(true)
                .schemaOrgEnabled(true)
                .sendEtags(false)
                .collectionDescriptionsInOverview(true)
                .legalName("foo")
                .legalUrl("foo")
                .privacyName("foo")
                .privacyUrl("foo")
                .basemapUrl("foo")
                .basemapAttribution("foo")
                .footerText("foo")
                .build()
    }

    @Override
    HtmlConfiguration getMinimal() {
        return new ImmutableHtmlConfiguration.Builder()
                .build()
    }

    @Override
    HtmlConfiguration getMinimalFullMerged() {
        return getFull()
    }

    @Override
    HtmlConfiguration getSimple() {
        return new ImmutableHtmlConfiguration.Builder()
                .enabled(false)
                .noIndexEnabled(false)
                .schemaOrgEnabled(false)
                .sendEtags(false)
                .collectionDescriptionsInOverview(false)
                .legalName("bar")
                .legalUrl("bar")
                .privacyName("bar")
                .privacyUrl("bar")
                .basemapUrl("bar")
                .basemapAttribution("bar")
                .footerText("bar")
                .build()
    }

    @Override
    HtmlConfiguration getSimpleFullMerged() {
        return new ImmutableHtmlConfiguration.Builder()
                .from(getFull())
                .from(getSimple())
                .build()
    }
}
