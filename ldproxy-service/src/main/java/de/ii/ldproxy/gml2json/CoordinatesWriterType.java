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

import de.ii.xsf.logging.XSFLogger;
import java.io.Writer;

import de.ii.xtraplatform.crs.api.CrsTransformer;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author zahnen
 */
public enum CoordinatesWriterType {

    DEFAULT {
        @Override
        Writer create(Builder builder) {
            //LOGGER.debug("creating GML2JsonCoordinatesWriter");
            return new DefaultCoordinatesWriter(builder.formatter, builder.srsDimension);
        }
    },
    SWAP {
        @Override
        Writer create(Builder builder) {
            //LOGGER.debug("creating GML2JsonFastXYSwapCoordinatesWriter");
            return new FastXYSwapCoordinatesWriter(builder.formatter, builder.srsDimension);
        }
    },
    TRANSFORM {
        @Override
        Writer create(Builder builder) {
            //LOGGER.debug("creating GML2JsonTransCoordinatesWriter");
            return new TransformingCoordinatesWriter(builder.formatter, builder.srsDimension, builder.transformer);
        }
    },
    BUFFER_TRANSFORM {
        @Override
        Writer create(Builder builder) {
            //LOGGER.debug("creating GML2JsonBufferedTransformingCoordinatesWriter");
            return new BufferedTransformingCoordinatesWriter(builder.formatter, builder.srsDimension, builder.transformer, builder.swap, builder.reversepolygon);
        }
    }/*,
    SIMPLIFY_BUFFER_TRANSFORM {
        @Override
        Writer create(Builder builder) {
            //LOGGER.debug("creating GML2JsonSimplifiyingBufferedTransformingCoordinatesWriter");
            return new SimplifiyingBufferedTransformingCoordinatesWriter(builder.jsonOut, builder.srsDimension, builder.transformer, builder.simplifier, builder.swap, builder.reversepolygon);
        }
    }*/;

    abstract Writer create(Builder builder);
    
    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(CoordinatesWriterType.class);
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(CoordinateFormatter formatter) {
        return new Builder().format(formatter);
    }
    
    public static class Builder {
        private CoordinateFormatter formatter;
        private int srsDimension;
        private boolean swap;
        private boolean transform;
        private boolean simplify;
        private boolean reversepolygon;
        private CrsTransformer transformer;
        //private DouglasPeuckerLineSimplifier simplifier;
        
        public Builder () {
            this.srsDimension = 2;
            this.swap = false;
            this.transform = false;
            this.simplify = false;
            this.reversepolygon = false;
        }
        
        public CoordinatesWriterType getType() {
            /*if (simplify) {
                return CoordinatesWriterType.SIMPLIFY_BUFFER_TRANSFORM;
            }
            else*/ if (reversepolygon) {
                return CoordinatesWriterType.BUFFER_TRANSFORM;
            }
            else if (transform) {
                return CoordinatesWriterType.TRANSFORM;
            }
            else if (swap) {
                return CoordinatesWriterType.SWAP;
            }
            return CoordinatesWriterType.DEFAULT;
        }
        
        public Writer build() {
            if (formatter == null) {
                return null;
            }
            return getType().create(this);
        }
        
        public Builder format(CoordinateFormatter formatter) {
            this.formatter = formatter;
            return this;
        }
        
        public Builder dimension(int dim) {
            this.srsDimension = dim;
            return this;
        }
        
        public Builder swap() {
            this.swap = true;
            return this;
        }
        
        public Builder reversepolygon() {
            this.reversepolygon = true;
            return this;
        }
        
        public Builder transformer(CrsTransformer transformer) {
            this.transformer = transformer;
            this.transform = true;
            return this;
        }
        
        /*public Builder simplifier(DouglasPeuckerLineSimplifier simplifier) {
            this.simplifier = simplifier;
            this.simplify = true;
            return this;
        }*/
    }
}