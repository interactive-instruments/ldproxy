package de.ii.xtraplatform.jackson.dynamic;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author zahnen
 */
// TODO: move to global bundle

@Component
@Provides(specifications = {DynamicTypeIdResolver.class})
@Instantiate
@Wbp(
    filter="(objectClass=de.ii.xtraplatform.jackson.dynamic.JacksonSubTypeIds)",
    onArrival="onArrival",
    onDeparture="onDeparture")
public class DynamicTypeIdResolver implements TypeIdResolver {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(DynamicTypeIdResolver.class);

    private final BundleContext context;
    private JavaType mBaseType;
    private final BiMap<Class<?>, String> mapping;

    public DynamicTypeIdResolver(@Context BundleContext context) {
        this.mapping = HashBiMap.create();
        this.context = context;
    }

    public synchronized void onArrival(ServiceReference<JacksonSubTypeIds> ref) {
        JacksonSubTypeIds ids = context.getService(ref);
        if (ids != null) {
            LOGGER.getLogger().debug("REGISTERING SUBTYPE IDS {}", ids.getMapping());
            mapping.putAll(ids.getMapping());
        }
    }
    public synchronized void onDeparture(ServiceReference<JacksonSubTypeIds> ref) {
        JacksonSubTypeIds ids = context.getService(ref);
        if (ids != null) {
            for (Class<?> clazz : ids.getMapping().keySet()) {
                mapping.remove(clazz);
            }
        }
    }

    @Override
    public void init(JavaType baseType) {
        mBaseType = baseType;
    }

    @Override
    public String idFromValue(Object value) {
        return idFromValueAndType(value, value.getClass());
    }

    @Override
    public String idFromBaseType() {
        return idFromValueAndType(null, mBaseType.getRawClass());
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        if (mapping.containsKey(suggestedType)) {
            return mapping.get(suggestedType);
        }

        return null;
    }

    @Override
    public JavaType typeFromId(String id) {
        if (mapping.inverse().containsKey(id)) {
            Class<?> clazz = mapping.inverse().get(id);
            return TypeFactory.defaultInstance().constructSpecializedType(mBaseType, clazz);
        }

        return null;
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }
}
