/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.ldproxy.gml2json;

import com.fasterxml.jackson.core.JsonGenerator;
import de.ii.xtraplatform.crs.api.CrsTransformer;

/**
 *
 * @author zahnen
 */
public class SimplifiyingBufferedTransformingCoordinatesWriter extends BufferedTransformingCoordinatesWriter {

    //private DouglasPeuckerLineSimplifier simplifier;

    public SimplifiyingBufferedTransformingCoordinatesWriter(JsonGenerator json, int srsDimension, CrsTransformer transformer, /*DouglasPeuckerLineSimplifier simplifier,*/ boolean swap, boolean reversepolygon) {
        super(json, srsDimension, transformer, swap, reversepolygon);
        //this.simplifier = simplifier;
    }

    /*@Override
    protected double[] postProcessCoordinates(double[] in, int numPts) {
        double[] out;
        if (simplifier != null) {
            out = simplifier.simplify(in, numPts);
            return super.postProcessCoordinates(out, out.length / 2);
        }
        return super.postProcessCoordinates(in, numPts);
    }*/
}
