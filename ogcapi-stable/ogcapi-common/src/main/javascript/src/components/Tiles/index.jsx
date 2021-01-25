import React from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";

import {
  AutoForm,
  ToggleField,
  TextField,
  getFieldsDefault,
} from "@xtraplatform/core";

const fieldsTransformation = {
  // {center: [7.5, 51.5]} <--> {centerLon: 7.5, centerLat: 51.5]}
  center: {
    split: {
      centerLon: (center) => center[0],
      centerLat: (center) => center[1],
    },
    merge: (centerLon, centerLat) => [centerLon, centerLat],
  },
  centerLon: {
    to: (value) => parseFloat(value),
  },
  centerLat: {
    to: (value) => parseFloat(value),
  },
  zoomLevels: {
    split: {
      zoomLevelsMin: (zoomLevels) => zoomLevels.WebMercatorQuad.min,
      zoomLevelsMax: (zoomLevels) => zoomLevels.WebMercatorQuad.max,
    },
    merge: (zoomLevelsMin, zoomLevelsMax) => ({
      WebMercatorQuad: {
        min: zoomLevelsMin,
        max: zoomLevelsMax,
      },
    }),
  },
  zoomLevelsMin: {
    to: (value) => parseInt(value),
  },
  zoomLevelsMax: {
    to: (value) => parseInt(value),
  },
  seeding: {
    split: {
      seedingMin: (seeding) => seeding.WebMercatorQuad.min,
      seedingMax: (seeding) => seeding.WebMercatorQuad.max,
    },
    merge: (seedingMin, seedingMax) => ({
      WebMercatorQuad: { min: seedingMin, max: seedingMax },
    }),
  },
  seedingMin: {
    to: (value) => parseInt(value),
  },
  seedingMax: {
    to: (value) => parseInt(value),
  },
};

const Tiles = ({
  singleCollectionEnabled,
  multiCollectionEnabled,
  center,
  zoomLevels,
  seeding,
  defaults,
  inheritedLabel,
  debounce,
  onPending,
  onChange,
}) => {
  const fields = {
    singleCollectionEnabled,
    multiCollectionEnabled,
    center,
    zoomLevels,
    seeding,
  };
  const fieldsDefault = getFieldsDefault(fields, defaults);

  const { t } = useTranslation();

  return (
    <AutoForm
      fields={fields}
      fieldsDefault={fieldsDefault}
      fieldsTransformation={fieldsTransformation}
      inheritedLabel={inheritedLabel}
      debounce={debounce}
      onPending={onPending}
      onChange={onChange}
    >
      <ToggleField
        name="singleCollectionEnabled"
        label={t("building_blocks:TILES.singleCollectionEnabled._label")}
        help={t("building_blocks:TILES.singleCollectionEnabled._description")}
      />
      <ToggleField
        name="multiCollectionEnabled"
        label={t("building_blocks:TILES.multiCollectionEnabled._label")}
        help={t("building_blocks:TILES.multiCollectionEnabled._description")}
      />
      <TextField
        name="centerLon"
        label={t("building_blocks:TILES.center._label", {
          part: t("building_blocks:TILES.center.longitude"),
        })}
        help={t("building_blocks:TILES.center._description")}
        type="number"
      />
      <TextField
        name="centerLat"
        label={t("building_blocks:TILES.center._label", {
          part: t("building_blocks:TILES.center.latitude"),
        })}
        help={t("building_blocks:TILES.center._description")}
        type="number"
      />
      <TextField
        name="zoomLevelsMin"
        label={t("building_blocks:TILES.zoomLevels._label", {
          part: t("building_blocks:TILES.zoomLevels.min"),
        })}
        help={t("building_blocks:TILES.zoomLevels._description")}
        type="number"
        min="0"
      />
      <TextField
        name="zoomLevelsMax"
        label={t("building_blocks:TILES.zoomLevels._label", {
          part: t("building_blocks:TILES.zoomLevels.max"),
        })}
        help={t("building_blocks:TILES.zoomLevels._description")}
        type="number"
        min="0"
      />
      <TextField
        name="seedingMin"
        label={t("building_blocks:TILES.seeding._label", {
          part: t("building_blocks:TILES.zoomLevels.min"),
        })}
        help={t("building_blocks:TILES.seeding._description")}
        type="number"
        min="0"
      />
      <TextField
        name="seedingMax"
        label={t("building_blocks:TILES.seeding._label", {
          part: t("building_blocks:TILES.zoomLevels.max"),
        })}
        help={t("building_blocks:TILES.seeding._description")}
        type="number"
        min="0"
      />
    </AutoForm>
  );
};

Tiles.displayName = "Tiles";

Tiles.propTypes = {
  onChange: PropTypes.func.isRequired,
};

Tiles.defaultProps = {};

export default Tiles;
