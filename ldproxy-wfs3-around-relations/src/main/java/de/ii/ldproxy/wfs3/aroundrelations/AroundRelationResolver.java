package de.ii.ldproxy.wfs3.aroundrelations;

/**
 * @author zahnen
 */
public interface AroundRelationResolver {
    String resolve(AroundRelationsQuery.AroundRelationQuery aroundRelationQuery);

    String getUrl(AroundRelationsQuery.AroundRelationQuery aroundRelationQuery, String additionalParameters);

    String resolve(AroundRelationsQuery.AroundRelationQuery aroundRelationQuery, String additionalParameters);
}
