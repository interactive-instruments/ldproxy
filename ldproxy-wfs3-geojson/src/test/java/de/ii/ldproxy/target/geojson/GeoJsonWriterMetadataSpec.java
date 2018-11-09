/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.greghaskins.spectrum.Spectrum;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;

import static com.greghaskins.spectrum.dsl.specification.Specification.beforeEach;
import static com.greghaskins.spectrum.dsl.specification.Specification.context;
import static com.greghaskins.spectrum.dsl.specification.Specification.describe;

/**
 * @author zahnen
 */
@RunWith(Spectrum.class)
public class GeoJsonWriterMetadataSpec {

    {

        describe("GeoJsonWriterMetadata middleware", () -> {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            beforeEach(outputStream::reset);

            context("todo", () -> {

            });

        });

    }
}
