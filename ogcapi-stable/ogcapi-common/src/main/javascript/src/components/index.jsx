import React from "react";
import PropTypes from "prop-types";
import { createFeature, fassetValidations } from "feature-u";
import { validatePropTypes } from "@xtraplatform/core";
import { routes, i18n } from "@xtraplatform/manager";
import { serviceViewActions, serviceEditTabs } from "@xtraplatform/services";

import { apiBuildingBlocks, collectionEditTabs } from "./constants";
import Api from "./Api";
import Collections from "./Collections";
import CollectionsStandalone from "./Collections/Standalone";
import CollectionEdit from "./Collections/Edit";
import CollectionEditGeneral from "./Collections/Edit/Main/General";
import Metadata from "./Metadata";
import Extent from "./Extent";
import Foundation from "./Foundation";
import Html from "./Html";
import FeaturesCore from "./FeaturesCore";
import GeoJson from "./GeoJson";
import FeaturesHtml from "./FeaturesHtml";
import Crs from "./Crs";
import Tiles from "./Tiles";
import Styles from "./Styles";
import CollectionTabSchema from "./CollectionTabSchema";

import i18nService from "../i18n/services_ogc_api.json";
import i18nApi from "../i18n/building-blocks.json";

export default createFeature({
  name: "ogcapi-common",

  fassets: {
    // provided resources
    defineUse: {
      [i18n("ogcapicommon")]: {
        namespace: "services/ogc_api",
        translations: i18nService,
      },
      [i18n("buildingblocks")]: {
        namespace: "building_blocks",
        translations: i18nApi,
      },
      [serviceEditTabs("metadata")]: {
        id: "metadata",
        label: "services/ogc_api:metadata._label",
        sortPriority: 20,
        component: Metadata,
      },
      [serviceEditTabs("extent")]: {
        id: "extent",
        label: "services/ogc_api:extent._label",
        sortPriority: 30,
        component: Extent,
      },
      [serviceEditTabs("api")]: {
        id: "api",
        label: "services/ogc_api:api._label",
        sortPriority: 40,
        component: Api,
      },
      [serviceEditTabs("collections")]: {
        id: "collections",
        label: "services/ogc_api:collections._label",
        component: Collections,
        noDefaults: true,
      },
      [apiBuildingBlocks("foundation")]: {
        id: "FOUNDATION",
        label: "Foundation",
        sortPriority: 10,
        noDisable: true,
        component: Foundation,
      },
      [apiBuildingBlocks("common")]: {
        id: "COMMON",
        label: "Common Core",
        sortPriority: 20,
        noDisable: true,
      },
      [apiBuildingBlocks("json")]: {
        id: "JSON",
        label: "JSON",
        sortPriority: 30,
      },
      [apiBuildingBlocks("html")]: {
        id: "HTML",
        label: "HTML",
        sortPriority: 40,
        component: Html,
      },
      [apiBuildingBlocks("oas30")]: {
        id: "OAS30",
        label: "OpenAPI 3.0",
        sortPriority: 50,
      },
      [apiBuildingBlocks("collections")]: {
        id: "COLLECTIONS",
        label: "Collections",
        sortPriority: 55,
      },
      [apiBuildingBlocks("features_core")]: {
        id: "FEATURES_CORE",
        label: "Features Core",
        sortPriority: 60,
        dependencies: ["collections"],
        component: FeaturesCore,
      },
      [apiBuildingBlocks("geo_json")]: {
        id: "GEO_JSON",
        label: "Features GeoJSON",
        sortPriority: 70,
        dependencies: ["features_core"],
        component: GeoJson,
      },
      [apiBuildingBlocks("features_html")]: {
        id: "FEATURES_HTML",
        label: "Features HTML",
        sortPriority: 80,
        dependencies: ["features_core", "geo_json"],
        component: FeaturesHtml,
      },
      [apiBuildingBlocks("queryables")]: {
        id: "QUERYABLES",
        label: "Queryables",
        sortPriority: 90,
        dependencies: ["collections"],
      },
      [apiBuildingBlocks("filter")]: {
        id: "FILTER",
        label: "Filter (CQL)",
        sortPriority: 100,
        dependencies: ["queryables"],
      },
      [apiBuildingBlocks("schema")]: {
        id: "SCHEMA",
        label: "Schema",
        sortPriority: 110,
        dependencies: ["collections"],
      },
      [apiBuildingBlocks("crs")]: {
        id: "CRS",
        label: "Coordinate Reference Systems",
        sortPriority: 120,
        dependencies: ["features_core"],
        component: Crs,
      },
      [apiBuildingBlocks("tiles")]: {
        id: "TILES",
        label: "Vector Tiles",
        sortPriority: 130,
        dependencies: ["collections"],
        component: Tiles,
      },
      [apiBuildingBlocks("styles")]: {
        id: "STYLES",
        label: "Styles",
        sortPriority: 140,
        component: Styles,
      },
      [routes("ogcapicommon")]: [
        {
          path: "/services/:id/:cid",
          content: <CollectionEdit />,
          sidebar: <CollectionsStandalone isCompact />,
        },
      ],
      [collectionEditTabs("general")]: {
        id: "general",
        label: "General",
        sortPriority: 10,
        component: CollectionEditGeneral,
      },
      [collectionEditTabs("extent")]: {
        id: "extent",
        label: "Extent",
        sortPriority: 20,
        component: Extent,
      },
      [collectionEditTabs("schema")]: {
        id: "schema",
        label: "Data",
        sortPriority: 30,
        component: CollectionTabSchema,
      },
    },
    // consumed resources
    use: [
      [
        apiBuildingBlocks(),
        {
          required: false,
          type: validatePropTypes({
            id: PropTypes.string.isRequired,
            label: PropTypes.string.isRequired,
            sortPriority: PropTypes.number.isRequired,
            component: PropTypes.elementType,
          }),
        },
      ],
      [
        collectionEditTabs(),
        {
          required: false,
          type: validatePropTypes({
            id: PropTypes.string.isRequired,
            label: PropTypes.string.isRequired,
            sortPriority: PropTypes.number.isRequired,
            component: PropTypes.elementType.isRequired,
          }),
        },
      ],
    ],
  },
});
