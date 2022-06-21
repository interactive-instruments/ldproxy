/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.cityjson.app;

import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.cityjson.domain.CityJsonConfiguration;
import de.ii.ogcapi.features.cityjson.domain.CityJsonWriter;
import de.ii.ogcapi.features.cityjson.domain.EncodingAwareContextCityJson;
import de.ii.ogcapi.features.cityjson.domain.FeatureTransformationContextCityJson;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Singleton
@AutoBind
public class CityJsonWriterAddress implements CityJsonWriter {

    private boolean addressSeen;
    private boolean onlyFirstAddress;
    private boolean inMultiPoint;

    @Inject
    CityJsonWriterAddress() {
    }

    @Override
    public CityJsonWriterAddress create() {
        return new CityJsonWriterAddress();
    }

    @Override
    public int getSortPriority() {
        return 50;
    }

    @Override
    public void onFeatureEnd(EncodingAwareContextCityJson context,
                             Consumer<EncodingAwareContextCityJson> next) throws IOException {
        context.encoding().flushAddress();

        next.accept(context);
    }

    @Override
    public void onArrayStart(EncodingAwareContextCityJson context,
                              Consumer<EncodingAwareContextCityJson> next) throws IOException {
        if (context.schema().map(FeatureSchema::isArray).orElse(false)
            && context.getState().atTopLevel()
            && context.schema().map(FeatureSchema::getName).filter(CityJsonWriter.ADDRESS::equals).isPresent()) {
            context.encoding()
                .changeSection(FeatureTransformationContextCityJson.StateCityJson.Section.IN_ADDRESS);
            context.encoding()
                .startAddress();
            inMultiPoint = false;
            addressSeen = false;
            onlyFirstAddress = context.encoding().getVersion().equals(CityJsonConfiguration.Version.V10);
        }

        next.accept(context);
    }

    @Override
    public void onArrayEnd(EncodingAwareContextCityJson context,
                           Consumer<EncodingAwareContextCityJson> next) throws IOException {
        next.accept(context);

        if (context.schema().map(FeatureSchema::isArray).orElse(false)
            && context.getState().inSection()==FeatureTransformationContextCityJson.StateCityJson.Section.IN_ADDRESS) {
            context.encoding()
                .stopAddress();
            context.encoding()
                .returnToPreviousSection();
        }
    }

    @Override
    public void onObjectStart(EncodingAwareContextCityJson context, Consumer<EncodingAwareContextCityJson> next) throws IOException {
        if (context.getState().inSection()==FeatureTransformationContextCityJson.StateCityJson.Section.IN_ADDRESS
            && context.schema().map(FeatureSchema::getName).filter(CityJsonWriter.ADDRESS::equals).isPresent()) {
            if (addressSeen) {
                context.encoding().nextAddress();
            } else {
                addressSeen = true;
            }
        }
        else if (context.getState().inSection()==FeatureTransformationContextCityJson.StateCityJson.Section.IN_ADDRESS
            && context.schema().map(SchemaBase::isSpatial).orElse(false)
            && context.geometryType().filter(type -> type==SimpleFeatureGeometry.MULTI_POINT).isPresent()
            && context.getState().getAddressBuffer().isPresent()
            && !(addressSeen && onlyFirstAddress)) {
            TokenBuffer addressBuffer = context.getState().getAddressBuffer().get();
            addressBuffer.writeObjectFieldStart(CityJsonWriter.LOCATION);
            addressBuffer.writeStringField(TYPE, CityJsonWriter.MULTI_POINT);
            addressBuffer.writeStringField(LOD, "1");
            addressBuffer.writeArrayFieldStart(BOUNDARIES);
            inMultiPoint = true;
        }

        next.accept(context);
    }

    @Override
    public void onObjectEnd(EncodingAwareContextCityJson context, Consumer<EncodingAwareContextCityJson> next) throws IOException {
        next.accept(context);

        if (context.schema().map(FeatureSchema::isObject).orElse(false)) {
            if (context.getState().inBuildingPart()
                && context.schema().map(FeatureSchema::getName).filter(CONSISTS_OF_BUILDING_PART::equals).isPresent()
                && context.encoding().buildingPartHasAddress()) {
                context.encoding().flushAddress();
            }
        } else if (inMultiPoint
            && context.schema().map(FeatureSchema::isSpatial).orElse(false)
            && context.getState().getAddressBuffer().isPresent()) {
            TokenBuffer addressBuffer = context.getState().getAddressBuffer().get();
            addressBuffer.writeEndArray();
            addressBuffer.writeEndObject();
            inMultiPoint = false;
        }
    }

    @Override
    public void onValue(EncodingAwareContextCityJson context,
                        Consumer<EncodingAwareContextCityJson> next) throws IOException {

        if (inMultiPoint) {
            Optional<Integer> optionalIndex = context.encoding().processOrdinate(context.value());
            if (context.getState().getAddressBuffer().isPresent() && optionalIndex.isPresent()) {
                context.getState()
                       .getAddressBuffer().get()
                       .writeNumber(optionalIndex.get());
            }

        } else if (context.getState().inSection()==FeatureTransformationContextCityJson.StateCityJson.Section.IN_ADDRESS
            && hasMappingAndValue(context)
            && context.schema().isPresent()
            && context.getState().getAddressBuffer().isPresent()
            && !(addressSeen && onlyFirstAddress)) {
            FeatureSchema schema = context.schema().get();
            if (schema.getName().matches("^(?:"+String.join("|",ADDRESS_ATTRIBUTES)+")$")) {
                context.getState().getAddressBuffer().get()
                    .writeStringField(schema.getName(), context.value());
            }
        }

        next.accept(context);
    }

    private boolean hasMappingAndValue(EncodingAwareContextCityJson context) {
        return context.schema().map(FeatureSchema::isValue).orElse(false)
            && Objects.nonNull(context.value());
    }
}