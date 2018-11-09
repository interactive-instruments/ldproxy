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
public class GeoJsonWriterIntegrationSpec {

    {

        describe("GeoJsonWriterIntegration middleware", () -> {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            beforeEach(outputStream::reset);

            context("todo", () -> {

            });

        });

    }
}
