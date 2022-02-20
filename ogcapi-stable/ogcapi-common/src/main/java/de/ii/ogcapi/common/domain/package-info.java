@AutoModule(single = true, encapsulate = true, multiBindings = {MessageBodyWriter.class})
@Value.Style(deepImmutablesDetection = true, attributeBuilderDetection = true, builder = "new")
@BuildableMapEncodingEnabled
package de.ii.ogcapi.common.domain;

import com.github.azahnen.dagger.annotations.AutoModule;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.encoding.BuildableMapEncodingEnabled;
import javax.ws.rs.ext.MessageBodyWriter;
import org.immutables.value.Value;
