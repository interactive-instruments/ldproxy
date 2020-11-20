import React from "react";
import PropTypes from "prop-types";

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
        label="Single collection enabled"
        help="TODO"
      />
      <ToggleField
        name="multiCollectionEnabled"
        label="Multi collection enabled"
        help="TODO"
      />
      <TextField
        name="centerLon"
        label="Center longitude"
        help="TODO"
        type="number"
      />
      <TextField
        name="centerLat"
        label="Center latitude"
        help="TODO"
        type="number"
      />
      <TextField
        name="zoomLevelsMin"
        label="Minimum zoom level"
        help="TODO"
        type="number"
        min="0"
      />
      <TextField
        name="zoomLevelsMax"
        label="Maximum zoom level"
        help="TODO"
        type="number"
        min="0"
      />
      <TextField
        name="seedingMin"
        label="Minimum seeding level"
        help="TODO"
        type="number"
        min="0"
      />
      <TextField
        name="seedingMax"
        label="Maximum seeding level"
        help="TODO"
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
