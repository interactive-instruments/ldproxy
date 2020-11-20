import React from "react";
import PropTypes from "prop-types";
import { createFeature, fassetValidations } from "feature-u";
import { validatePropTypes } from "@xtraplatform/core";
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

export default createFeature({
  name: "ogcapi-common",

  fassets: {
    // provided resources
    defineUse: {
      [serviceEditTabs("metadata")]: {
        id: "metadata",
        label: "Metadata",
        sortPriority: 20,
        component: Metadata,
      },
      [serviceEditTabs("extent")]: {
        id: "extent",
        label: "Extent",
        sortPriority: 30,
        component: Extent,
      },
      [serviceEditTabs("api")]: {
        id: "api",
        label: "Api",
        sortPriority: 40,
        component: Api,
      },
      [serviceEditTabs("collections")]: {
        id: "collections",
        label: "Collections",
        component: Collections,
        noDefaults: true,
      },
      [apiBuildingBlocks("foundation")]: {
        id: "FOUNDATION",
        label: "Foundation",
        sortPriority: 10,
        component: Foundation,
      },
      [apiBuildingBlocks("common")]: {
        id: "COMMON",
        label: "Common",
        sortPriority: 20,
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
      [apiBuildingBlocks("features_core")]: {
        id: "FEATURES_CORE",
        label: "Features Core",
        sortPriority: 60,
        component: FeaturesCore,
      },
      [apiBuildingBlocks("geo_json")]: {
        id: "GEO_JSON",
        label: "Features GeoJSON",
        sortPriority: 70,
        component: GeoJson,
      },
      [apiBuildingBlocks("features_html")]: {
        id: "FEATURES_HTML",
        label: "Features HTML",
        sortPriority: 80,
        component: FeaturesHtml,
      },
      [apiBuildingBlocks("crs")]: {
        id: "CRS",
        label: "Coordinate Reference Systems",
        sortPriority: 90,
        component: Crs,
      },
      [apiBuildingBlocks("tiles")]: {
        id: "TILES",
        label: "Vector Tiles",
        sortPriority: 100,
        component: Tiles,
      },
      [apiBuildingBlocks("styles")]: {
        id: "STYLES",
        label: "Styles",
        sortPriority: 110,
        component: Styles,
      },
      [apiBuildingBlocks("queryables")]: {
        id: "QUERYABLES",
        label: "Queryables",
        sortPriority: 120,
      },
      [apiBuildingBlocks("schema")]: {
        id: "SCHEMA",
        label: "Schema",
        sortPriority: 130,
      },
      [apiBuildingBlocks("filter")]: {
        id: "FILTER",
        label: "Filter (CQL)",
        sortPriority: 140,
      },
      [`ogcapicommon.routes`]: [
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
        label: "Schema",
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
