/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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

    public SimplifiyingBufferedTransformingCoordinatesWriter(CoordinateFormatter formatter, int srsDimension, CrsTransformer transformer, /*DouglasPeuckerLineSimplifier simplifier,*/ boolean swap, boolean reversepolygon) {
        super(formatter, srsDimension, transformer, swap, reversepolygon);
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
