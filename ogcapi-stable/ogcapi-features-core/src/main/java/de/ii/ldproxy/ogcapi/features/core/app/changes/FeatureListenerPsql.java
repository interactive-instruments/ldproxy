/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app.changes;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import de.ii.ldproxy.ogcapi.domain.ApiExtension;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiBackgroundTask;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.TemporalExtent;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.Listener;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.ChangeContext;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.FeatureChangeAction;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.FeatureChangeActionsRegistry;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.ImmutableChangeContext;
import de.ii.xtraplatform.cql.domain.TemporalLiteral;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
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

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.dropwizard.domain.LambdaWithException.consumerMayThrow;

@Component
@Provides
@Instantiate
public class FeatureListenerPsql implements ApiExtension, OgcApiBackgroundTask {

    // TODO move to the feature provider

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureListenerPsql.class);
    private final ExtensionRegistry extensionRegistry;
    private final FeaturesCoreProviders providers;
    private final Map<Integer, Map<String, Connection>> connections;
    private final Map<Integer, Map<String, Listener>> listeners;
    private final ScheduledExecutorService executorService;
    private final FeatureChangeActionsRegistry featureChangeActionRegistry;

    // determine when needed; if already set in the constructor, the component fails to load
    private SortedSet<FeatureChangeAction> featureChangeActions = null;

    public FeatureListenerPsql(@Requires ExtensionRegistry extensionRegistry,
                               @Requires FeatureChangeActionsRegistry featureChangeActionRegistry,
                               @Requires FeaturesCoreProviders providers) {
        this.extensionRegistry = extensionRegistry;
        this.providers = providers;
        this.connections = new ConcurrentHashMap<>();
        this.listeners = new ConcurrentHashMap<>();
        this.executorService = new ScheduledThreadPoolExecutor(1);
        this.featureChangeActionRegistry = featureChangeActionRegistry;
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
        Map<String, Connection> apiConnections = new ConcurrentHashMap<>();
        connections.put(key, apiConnections);
        Map<String, Listener> apiListeners = new ConcurrentHashMap<>();
        listeners.put(key, apiListeners);
        for (Map.Entry<String, FeatureTypeConfigurationOgcApi> entry : apiData.getCollections().entrySet()) {
            getPsqlListeners(apiData, entry.getKey())
                    .stream()
                    .forEach(listener -> {
                        FeatureProviderDataV2 providerData = providers.getFeatureProvider(apiData, entry.getValue()).getData();
                        // TODO currently the dialect is not accessible via ProviderData or from other modules
                        // if (!providerData.getDialect().equals(ConnectionInfoSql.Dialect.PGIS)) {
                        //    resultBuilder.addErrors(String.format("Collection %s includes a PostgreSQL listener, but has an SQL feature provider that does not support the PostgreSQL dialect.", entry.getKey()));
                        // } else
                        if (providerData.getFeatureProviderType().equals("SQL")) {
                            try {
                                connections.get(key).put(entry.getKey(), getConnection(listener));
                                listeners.get(key).put(entry.getKey(), listener);
                                LOGGER.info("PostgreSQL listener active to retrieve notifications for collection '{}'.", entry.getKey());
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

    private Connection getConnection(Listener listener) throws ClassNotFoundException, SQLException {
        String host = listener.getHost();
        String database = listener.getDatabase();
        String user = listener.getUser();
        String password = listener.getPassword();
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://" + host + "/" + database;
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        stmt.execute("LISTEN " + listener.getChannel());
        stmt.close();
        return conn;
    }

    @Override
    public void run(OgcApi api, TaskContext taskContext) {
        executorService.scheduleWithFixedDelay(
                () -> {
                    LOGGER.trace("Executing PostgreSQL Listener for API " + api.getId());

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
                                                      LOGGER.trace("Feature change notification received: " + notifications[i].getName() + "; " + notifications[i].getParameter());
                                                      processFeatureChangeActions(api.getData(), entry.getKey(), notifications[i]);
                                                  }
                                              }
                                          } catch (SQLException e) {
                                              try {
                                                  if (entry.getValue().isValid(1)) {
                                                      // assume a temporary issue
                                                      LOGGER.debug("Temporary failure to retrieve notifications for collection '{}': {}", entry.getKey(), e.getMessage());
                                                  } else {
                                                      // try to establish a new connection
                                                      LOGGER.debug("Lost connection to retrieve notifications for collection '{}': {}", entry.getKey(), e.getMessage());
                                                      connections.get(key).put(entry.getKey(), getConnection(listeners.get(key).get(entry.getKey())));
                                                      LOGGER.info("PostgreSQL listener restarted to retrieve notifications for collection '{}'.", entry.getKey());
                                                  }
                                              } catch (SQLException | ClassNotFoundException e2) {
                                                  LOGGER.error("Removing listener after connection failures to retrieve notifications for collection '{}': {}", entry.getKey(), e2.getMessage());
                                                  if (LOGGER.isDebugEnabled()) {
                                                      LOGGER.debug("Stacktrace: ", e);
                                                  }
                                                  connections.get(key).remove(entry.getKey());
                                                  listeners.get(key).remove(entry.getKey());
                                              }
                                          }
                                      });
                    }
                },
                15,
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

    private Consumer<ChangeContext> executePipeline(final Iterator<FeatureChangeAction> actionsIterator) {
        return consumerMayThrow(nextChangeContext -> {
            if (actionsIterator.hasNext()) {
                actionsIterator.next()
                               .onEvent(nextChangeContext, this.executePipeline(actionsIterator));
            }
        });
    }

    private void processFeatureChangeActions(OgcApiDataV2 apiData, String collectionId, PGNotification notification) {

        if (Objects.isNull(featureChangeActions)) {
            featureChangeActions = featureChangeActionRegistry.getFeatureChangeActions()
                                                              .stream()
                                                              .map(FeatureChangeAction::create)
                                                              .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.comparingInt(FeatureChangeAction::getSortPriority)));
        }

        if (featureChangeActions.isEmpty())
            return;

        List<String> list = Splitter.on(",").trimResults().splitToList(notification.getParameter());
        if (list.size()<9) {
            // ignore incomplete notification
            LOGGER.warn("Could not parse a PostgreSQL change notification, the notification is incomplete and ignored: '{}'", notification.getParameter());
            return;
        }

        ChangeContext.Operation operation =  list.get(0).equalsIgnoreCase("insert")
                ? ChangeContext.Operation.INSERT
                : list.get(0).equalsIgnoreCase("update")
                ? ChangeContext.Operation.UPDATE
                : list.get(0).equalsIgnoreCase("delete")
                ? ChangeContext.Operation.DELETE
                : null;

        if (Objects.nonNull(operation)) {
            try {
                ChangeContext changeContext = new ImmutableChangeContext.Builder().operation(operation)
                                                                                  .apiData(apiData)
                                                                                  .collectionId(collectionId)
                                                                                  .featureIds(parseFeatureId(list.get(2)))
                                                                                  .interval(parseInterval(list.subList(3, 5)))
                                                                                  .boundingBox(parseBbox(list.subList(5, 9)))
                                                                                  .build();
                LOGGER.trace("Executing pipeline: {}, {}, {}, {}", changeContext.getOperation(), changeContext.getFeatureIds(), changeContext.getInterval(), changeContext.getBoundingBox());
                executePipeline(featureChangeActions.iterator()).accept(changeContext);
            } catch (Exception e) {
                LOGGER.warn("Could not parse a PostgreSQL change notification, the notification is ignored: {}", e.getMessage());
            }
        } else {
            LOGGER.warn("Could not parse a PostgreSQL change notification, the notification is ignored: unknown operation '{}'", list.get(0));
        }
    }

    @Nonnull
    private static List<String> parseFeatureId(String featureId) {
        if (featureId.isEmpty() || featureId.equalsIgnoreCase("NULL"))
            return ImmutableList.of();

        return ImmutableList.of(featureId);
    }

    @Nonnull
    private static Optional<TemporalExtent> parseInterval(List<String> interval) {
        if (interval.get(0).isEmpty()) {
        // ignore
        } else if (interval.get(1).isEmpty()) {
            try {
                Long instant = parseTimestamp(interval.get(0));
                if (Objects.nonNull(instant))
                    return Optional.of(TemporalExtent.of(instant,instant));
            } catch (Exception e) {
                // ignore
            }
        } else {
            try {
                Long begin = parseTimestamp(interval.get(0));
                Long end = parseTimestamp(interval.get(1));
                return Optional.of(TemporalExtent.of(begin,end));
            } catch (Exception e) {
                // ignore
            }
        }
        return Optional.empty();
    }

    private static Long parseTimestamp(String timestamp) {
        try {
            return Instant.parse(timestamp).toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }

    @Nonnull
    private static Optional<BoundingBox> parseBbox(List<String> bbox) {
        try {
            return Optional.of(BoundingBox.of(Double.parseDouble(bbox.get(0)),
                                              Double.parseDouble(bbox.get(1)),
                                              Double.parseDouble(bbox.get(2)),
                                              Double.parseDouble(bbox.get(3)),
                                              OgcCrs.CRS84));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
