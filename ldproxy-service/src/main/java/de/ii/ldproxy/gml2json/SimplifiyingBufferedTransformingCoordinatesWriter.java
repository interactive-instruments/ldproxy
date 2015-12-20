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
