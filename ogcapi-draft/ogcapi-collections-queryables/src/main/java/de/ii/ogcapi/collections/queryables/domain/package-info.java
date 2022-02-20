@AutoModule(single = true, encapsulate = true)
@Value.Style(deepImmutablesDetection = true, attributeBuilderDetection = true, builder = "new")
@BuildableMapEncodingEnabled
package de.ii.ogcapi.collections.queryables.domain;

import com.github.azahnen.dagger.annotations.AutoModule;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.encoding.BuildableMapEncodingEnabled;
import org.immutables.value.Value;
