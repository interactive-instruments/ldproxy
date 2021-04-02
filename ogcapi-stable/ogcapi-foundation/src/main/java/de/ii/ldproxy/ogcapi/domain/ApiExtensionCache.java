package de.ii.ldproxy.ogcapi.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ApiExtensionCache implements ApiExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiExtensionCache.class);

  private static final Map<String, Boolean> ENABLED_CACHE = new ConcurrentHashMap<>();
  private static final Map<String, Boolean> BOOLEAN_CACHE = new ConcurrentHashMap<>();
  private static final Map<String, String> STRING_CACHE = new ConcurrentHashMap<>();

  protected String computeStringIfAbsent(String key, Supplier<String> valueSupplier) {
    return STRING_CACHE.computeIfAbsent(key, ignore -> valueSupplier.get());
  }

  protected boolean computeIfAbsent(String key, Supplier<Boolean> valueSupplier) {
    return BOOLEAN_CACHE.computeIfAbsent(key, ignore -> valueSupplier.get());
  }

  @Override
  public <T extends ExtensionConfiguration> boolean isExtensionEnabled(
      ExtendableConfiguration extendableConfiguration, Class<T> clazz) {
    return ENABLED_CACHE
        .computeIfAbsent(extendableConfiguration.hashCode() + clazz.getName(), ignore -> {
      boolean extensionEnabled = ApiExtension.super
          .isExtensionEnabled(extendableConfiguration, clazz);
      LOGGER.debug("Caching {} {}", extensionEnabled, ignore);
      return extensionEnabled;
    });
  }

  @Override
  public <T extends ExtensionConfiguration> boolean isExtensionEnabled(
      ExtendableConfiguration extendableConfiguration, Class<T> clazz, Predicate<T> predicate) {
    return ENABLED_CACHE.computeIfAbsent(extendableConfiguration.hashCode() + clazz.getName() + predicate.hashCode(), ignore -> {
      boolean extensionEnabled = ApiExtension.super
          .isExtensionEnabled(extendableConfiguration, clazz, predicate);
      LOGGER.debug("Caching {} {}", extensionEnabled, ignore);
      return extensionEnabled;
    });
  }
}
