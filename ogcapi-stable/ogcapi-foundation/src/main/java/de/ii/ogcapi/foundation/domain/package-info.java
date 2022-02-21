@AutoModule(
    single = true,
    encapsulate = true,
    multiBindings = {
      ContainerRequestFilter.class,
      ContainerResponseFilter.class,
      Binder.class,
      ExceptionMapper.class
    })
@Value.Style(deepImmutablesDetection = true, attributeBuilderDetection = true, builder = "new")
@BuildableMapEncodingEnabled
package de.ii.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoModule;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.encoding.BuildableMapEncodingEnabled;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.ExceptionMapper;
import org.glassfish.jersey.internal.inject.Binder;
import org.immutables.value.Value;
