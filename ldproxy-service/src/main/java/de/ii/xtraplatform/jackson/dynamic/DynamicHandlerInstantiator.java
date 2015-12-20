package de.ii.xtraplatform.jackson.dynamic;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 * @author zahnen
 */
@Component
@Instantiate
public class DynamicHandlerInstantiator extends HandlerInstantiator {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(DynamicHandlerInstantiator.class);

    @Requires
    DynamicTypeIdResolver typeIdResolver;

    public DynamicHandlerInstantiator(@Requires Jackson jackson) {
        jackson.getDefaultObjectMapper().setHandlerInstantiator(this);
        LOGGER.getLogger().debug("REGISTERED DynamicHandlerInstantiator {}", this);
    }

    @Override
    public JsonDeserializer<?> deserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> deserClass) {
        return null;
    }

    @Override
    public KeyDeserializer keyDeserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> keyDeserClass) {
        return null;
    }

    @Override
    public JsonSerializer<?> serializerInstance(SerializationConfig config, Annotated annotated, Class<?> serClass) {
        return null;
    }

    @Override
    public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config, Annotated annotated, Class<?> builderClass) {
        return null;
    }

    @Override
    public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config, Annotated annotated, Class<?> resolverClass) {
        if (resolverClass.equals(DynamicTypeIdResolver.class)) {
            LOGGER.getLogger().debug("DynamicHandlerInstantiator typeIdResolverInstance {} {}", resolverClass, typeIdResolver);
            return typeIdResolver;
        }
        return null;
    }
}
