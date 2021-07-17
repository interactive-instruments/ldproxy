/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiExtension;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiBackgroundTask;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.Listener;
import de.ii.xtraplatform.feature.provider.sql.app.FeatureProviderSql;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.services.domain.TaskContext;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class FeatureListenerPsql implements ApiExtension, OgcApiBackgroundTask {

    // TODO move to the feature provider

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureListenerPsql.class);
    private final ExtensionRegistry extensionRegistry;
    private final FeaturesCoreProviders providers;
    private final Map<Integer, Map<String, Connection>> connections;
    private final ScheduledExecutorService executorService;

    public FeatureListenerPsql(@Requires ExtensionRegistry extensionRegistry,
                               @Requires FeaturesCoreProviders providers) {
        this.extensionRegistry = extensionRegistry;
        this.providers = providers;
        this.connections = new HashMap<>();
        this.executorService = new ScheduledThreadPoolExecutor(1);
    }

    @Override
    public Class<OgcApi> getServiceType()  { return OgcApi.class; }

    @Override
    public String getLabel() { return "Listener for feature changes in PostgreSQL databases"; }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return OgcApiBackgroundTask.super.isEnabledForApi(apiData) &&
                apiData.getCollections()
                       .values()
                       .stream()
                       .filter(FeatureTypeConfigurationOgcApi::getEnabled)
                       .anyMatch(featureType -> isEnabledForApi(apiData, featureType.getId()));
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        FeatureTypeConfigurationOgcApi featureType = apiData.getCollections()
                                                            .get(collectionId);
        return OgcApiBackgroundTask.super.isEnabledForApi(apiData, collectionId)
                && featureType.getEnabled()
                && !getPsqlListeners(apiData, collectionId).isEmpty();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }

    @Override
    public boolean runOnStart(OgcApi api) {
        return isEnabledForApi(api.getData());
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, ValidationResult.MODE apiValidation) {
        ImmutableValidationResult.Builder resultBuilder = ImmutableValidationResult.builder()
                                                                                   .mode(apiValidation);
        int key = apiData.hashCode();
        Map<String, Connection> apiConnections = new HashMap<>();
        connections.put(key, apiConnections);
        for (Map.Entry<String, FeatureTypeConfigurationOgcApi> entry : apiData.getCollections().entrySet()) {
            List<Listener> listeners = getPsqlListeners(apiData, entry.getKey());
            listeners.stream()
                     .forEach(listener -> {
                         FeatureProviderDataV2 providerData = providers.getFeatureProvider(apiData, entry.getValue()).getData();
                         if (providerData.getFeatureProviderType().equals("SQL")) {
                             try {
                                 String host = listener.getHost();
                                 String database = listener.getDatabase();
                                 String user = listener.getUser();
                                 String password = listener.getPassword();
                                 // TODO currently the dialect is not accessible via ProviderData or from other modules
                                 // if (providerData.getDialect().equals(ConnectionInfoSql.Dialect.PGIS)) {
                                     Class.forName("org.postgresql.Driver");
                                     String url = "jdbc:postgresql://" + host + "/" + database;
                                     Connection conn = DriverManager.getConnection(url, user, password);
                                     Statement stmt = conn.createStatement();
                                     stmt.execute("LISTEN " + listener.getChannel());
                                     stmt.close();
                                     connections.get(key).put(entry.getKey(), conn);
                                 // } else {
                                 //    resultBuilder.addErrors(String.format("Collection %s includes a PostgreSQL listener, but has an SQL feature provider that does not support the PostgreSQL dialect.", entry.getKey()));
                                 // }
                             } catch (ClassNotFoundException e) {
                                 resultBuilder.addErrors(String.format("Collection %s includes a PostgreSQL listener, but the PostgreSQL driver was not found.", entry.getKey()));
                             } catch (SQLException e) {
                                 resultBuilder.addErrors(String.format("Collection %s includes a PostgreSQL listener, but an SQL error occurred while initializing: %s", entry.getKey(), e.getMessage()));
                             }
                         } else {
                             resultBuilder.addErrors(String.format("Collection %s includes a PostgreSQL listener, but has an incompatible feature provider type '%s'.", entry.getKey(), providerData.getFeatureProviderType()));
                         }
                     });
        }
        return resultBuilder.build();
    }

    @Override
    public void run(OgcApi api, TaskContext taskContext) {
        executorService.scheduleWithFixedDelay(
                () -> {
                    LOGGER.debug("Executing PostgreSQL Listener for API " + api.getId());

                    int key = api.getData().hashCode();
                    Map<String, Connection> apiConnections = connections.get(key);
                    if (!taskContext.isStopped()) {
                        apiConnections.entrySet()
                                      .stream()
                                      .forEach(entry -> {
                                          try {
                                              // need to poll the notification queue using a dummy query
                                              Connection conn = entry.getValue();
                                              Statement stmt = conn.createStatement();
                                              ResultSet rs = stmt.executeQuery("SELECT 1");
                                              rs.close();
                                              stmt.close();

                                              PGNotification notifications[] = ((PGConnection) conn).getNotifications();
                                              if (notifications != null) {
                                                  for (int i=0; i<notifications.length; i++) {
                                                      // TODO start chain
                                                      LOGGER.debug("Feature change notification received: " + notifications[i].getName() + "; " + notifications[i].getParameter());
                                                  }
                                              }
                                          } catch (SQLException e) {
                                              LOGGER.error("Failure to retrieve notifications for collection {}: {}", entry.getKey(), e.getMessage());
                                              if (LOGGER.isDebugEnabled()) {
                                                  LOGGER.debug("Stacktrace: ", e);
                                              }
                                          }
                                      });
                    }


                },
                20,
                5,
                TimeUnit.SECONDS);
    }

    // TODO call
    public void close() {
        connections.values()
                   .stream()
                   .forEach(map -> map.entrySet()
                                      .stream()
                                      .forEach(entry -> {
                                           try {
                                               entry.getValue().close();
                                           } catch (SQLException e) {
                                                  LOGGER.warn("Error while closing listener connection for collection {}: {}", entry.getKey(), e.getMessage());
                                                  if (LOGGER.isDebugEnabled()) {
                                                      LOGGER.debug("Stacktrace: ", e);
                                                  }
                                              }
                                          }));
    }

    private List<Listener> getPsqlListeners(OgcApiDataV2 apiData, String collectionId) {
        return Optional.ofNullable(apiData.getCollections()
                                          .get(collectionId))
                       .flatMap(featureType -> featureType.getExtension(FeaturesCoreConfiguration.class))
                       .filter(FeaturesCoreConfiguration::isEnabled)
                       .map(FeaturesCoreConfiguration::getListeners)
                       .orElse(ImmutableList.of())
                       .stream()
                       .filter(listener -> listener.getType().equals("PSQL"))
                       .collect(Collectors.toUnmodifiableList());
    }
}
