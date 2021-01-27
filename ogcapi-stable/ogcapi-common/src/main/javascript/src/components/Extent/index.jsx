import React, { useCallback, useState } from "react";
import PropTypes from "prop-types";
import moment from "moment";
import { useTranslation } from "react-i18next";

import { Box } from "grommet";
import {
  AutoForm,
  TextField,
  ToggleField,
  InfoLabel,
  getFieldsDefault,
  mergedFields,
  isFloat,
  isInt,
  bounds,
  lessThan,
  greaterThan,
} from "@xtraplatform/core";

const fieldsTransformation = {
  spatial: {
    split: {
      xmin: (spatial) => spatial.xmin,
      ymin: (spatial) => spatial.ymin,
      xmax: (spatial) => spatial.xmax,
      ymax: (spatial) => spatial.ymax,
    },
    merge: (xmin, ymin, xmax, ymax) => ({
      xmin,
      ymin,
      xmax,
      ymax,
    }),
  },
  xmin: {
    to: (value) => parseFloat(value),
  },
  ymin: {
    to: (value) => parseFloat(value),
  },
  xmax: {
    to: (value) => parseFloat(value),
  },
  ymax: {
    to: (value) => parseFloat(value),
  },
  temporal: {
    split: {
      start: (temporal) =>
        temporal.start ? moment.utc(temporal.start).format() : "",
      end: (temporal) =>
        temporal.end ? moment.utc(temporal.end).format() : "",
    },
    merge: (start, end) => ({
      start,
      end,
    }),
  },
  start: {
    to: (value) => (value === "" ? null : moment.utc(value).valueOf() / 1000),
  },
  end: {
    to: (value) => (value === "" ? null : moment.utc(value).valueOf() / 1000),
  },
};

const fieldsValidation = {
  xmin: [
    isFloat(),
    bounds(-180, 180),
    lessThan("xmax", "X coordinate upper right corner"),
  ],
  ymin: [
    isFloat(),
    bounds(-90, 90),
    lessThan("ymax", "Y coordinate upper right corner"),
  ],
  xmax: [
    isFloat(),
    bounds(-180, 180),
    greaterThan("xmin", "X coordinate lower left corner"),
  ],
  ymax: [
    isFloat(),
    bounds(-90, 90),
    greaterThan("ymin", "Y coordinate lower left corner"),
  ],
  start: [
    (value) => (value && !moment.utc(value).isValid() ? "d" : null),
    //lessThan("end", "End time"),
  ],
  end: [
    (value) => (value && !moment.utc(value).isValid() ? "d" : null),
    //greaterThan("start", "Start time"),
  ],
};

const postProcess = (change, state) => {
  if (change.spatialComputed) {
    delete change.spatial;
  } else if (
    !change.spatial &&
    state.xmin &&
    state.ymin &&
    state.xmax &&
    state.ymax
  ) {
    change.spatial = {
      xmin: state.xmin,
      ymin: state.ymin,
      xmax: state.xmax,
      ymax: state.ymax,
    };
  }
  if (change.temporalComputed) {
    delete change.temporal;
  } else if (!change.temporal && state.start && state.end) {
    change.temporal = {
      start: state.start,
      end: state.end,
    };
  }
  return change;
};

const Extent = ({
  extent,
  defaultExtent,
  inheritedLabel,
  defaults,
  isDefaults,
  isCollection,
  debounce,
  onPending,
  onChange,
}) => {
  const { spatialComputed, temporalComputed, spatial, temporal } = {
    spatial: {},
    temporal: {},
    ...(isCollection ? extent : defaultExtent),
  };
  const fields = {
    spatialComputed,
    temporalComputed,
    spatial,
    temporal,
  };
  const fieldsDefault = getFieldsDefault(fields, defaults);

  const [state, setState] = useState(
    mergedFields(fields, fieldsDefault, fieldsTransformation)
  );

  const onExtentChange = useCallback(
    (change) => {
      const key = isCollection ? "extent" : "defaultExtent";

      onChange({ [key]: postProcess(change, state) });
    },
    [isCollection, onChange, state]
  );

  const { t } = useTranslation();

  return (
    <Box pad={{ horizontal: "small", vertical: "medium" }} fill="horizontal">
      <AutoForm
        fields={fields}
        fieldsDefault={fieldsDefault}
        fieldsTransformation={fieldsTransformation}
        fieldsValidation={fieldsValidation}
        inheritedLabel={inheritedLabel}
        values={state}
        setValues={setState}
        debounce={debounce}
        onPending={onPending}
        onChange={onExtentChange}
      >
        <Box
          pad={{ bottom: "small" }}
          margin={{ bottom: "small" }}
          border="bottom"
        >
          <InfoLabel
            label={t("services/ogc_api:Extent.spatial._label")}
            help={t("services/ogc_api:Extent.spatial._description")}
            mono={false}
            iconSize="list"
          />
        </Box>
        <ToggleField
          name="spatialComputed"
          label={t("services/ogc_api:Extent.spatialComputed._label")}
          help={t("services/ogc_api:Extent.spatialComputed._description")}
        />
        <TextField
          name="xmin"
          label={t("services/ogc_api:Extent.spatial.xmin._label")}
          disabled={state.spatialComputed}
          type="number"
        />
        <TextField
          name="ymin"
          label={t("services/ogc_api:Extent.spatial.ymin._label")}
          disabled={state.spatialComputed}
          type="number"
        />
        <TextField
          name="xmax"
          label={t("services/ogc_api:Extent.spatial.xmax._label")}
          disabled={state.spatialComputed}
          type="number"
        />
        <TextField
          name="ymax"
          label={t("services/ogc_api:Extent.spatial.ymax._label")}
          disabled={state.spatialComputed}
          type="number"
        />
        <Box
          pad={{ vertical: "small" }}
          margin={{ bottom: "small" }}
          border="bottom"
        >
          <InfoLabel
            label={t("services/ogc_api:Extent.temporal._label")}
            help={t("services/ogc_api:Extent.temporal._description")}
            mono={false}
            iconSize="list"
          />
        </Box>
        <ToggleField
          name="temporalComputed"
          label={t("services/ogc_api:Extent.temporalComputed._label")}
          help={t("services/ogc_api:Extent.temporalComputed._description")}
        />
        <TextField
          name="start"
          label={t("services/ogc_api:Extent.temporal.start._label")}
          help={t("services/ogc_api:Extent.temporal.start._description")}
          disabled={state.temporalComputed}
        />
        <TextField
          name="end"
          label={t("services/ogc_api:Extent.temporal.end._label")}
          help={t("services/ogc_api:Extent.temporal.end._description")}
          disabled={state.temporalComputed}
        />
      </AutoForm>
    </Box>
  );
};

Extent.displayName = "Extent";

Extent.propTypes = {
  extent: PropTypes.object,
  defaultExtent: PropTypes.object,
  onChange: PropTypes.func.isRequired,
};

Extent.defaultProps = {
  extent: { spatial: {}, temporal: {} },
  defaultExtent: { spatial: {}, temporal: {} },
};

export default Extent;
