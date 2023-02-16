import React, { useState } from "react";
import PropTypes from "prop-types";

import { Row, Col, Collapse } from "reactstrap";

import FieldFilter from "./Fields";
import TemporalFilter from "./Temporal";
import SpatialFilter, { MapSelect } from "./Spatial";

const EditorBody = ({
  isOpen,
  fields,
  backgroundUrl,
  attribution,
  spatial,
  temporal,
  filters,
  onAdd,
  deleteFilters,
  code,
  titleForFilter,
}) => {
  const [showMap, setShowMap] = useState(false);
  const [bounds, setBounds] = useState(spatial);

  return (
    <Collapse isOpen={isOpen} onEntered={() => setShowMap(true)}>
      <Row>
        <Col md="6">
          {Object.keys(fields).length > 0 && (
            <FieldFilter
              fields={Object.keys(fields)
                .filter((k) => !filters[k])
                .reduce(
                  (fs, k) => ({
                    ...fs,
                    [k]: fields[k],
                  }),
                  {}
                )}
              onAdd={onAdd}
              filters={filters}
              deleteFilters={deleteFilters}
              code={code}
              titleForFilter={titleForFilter}
            />
          )}
          {spatial && <SpatialFilter bounds={bounds} onChange={onAdd} />}
          {temporal && (
            <TemporalFilter
              start={temporal.start}
              end={temporal.end}
              filter={filters.datetime ? filters.datetime.value : null}
              onChange={onAdd}
            />
          )}
        </Col>
        <Col md="6">
          {showMap && spatial && (
            <MapSelect
              backgroundUrl={backgroundUrl}
              attribution={attribution}
              bounds={spatial}
              onChange={setBounds}
            />
          )}
        </Col>
      </Row>
    </Collapse>
  );
};

EditorBody.displayName = "EditorBody";

EditorBody.propTypes = {
  isOpen: PropTypes.bool,
  fields: PropTypes.objectOf(PropTypes.string),
  backgroundUrl: PropTypes.string.isRequired,
  attribution: PropTypes.string.isRequired,
  spatial: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)),
  temporal: PropTypes.objectOf(PropTypes.number),
  filters: PropTypes.objectOf(PropTypes.oneOfType([PropTypes.number, PropTypes.string])).isRequired,
  onAdd: PropTypes.func,
  deleteFilters: PropTypes.func.isRequired,
  code: PropTypes.objectOf(PropTypes.string).isRequired,
  titleForFilter: PropTypes.objectOf(PropTypes.string).isRequired,
};

EditorBody.defaultProps = {
  isOpen: false,
  fields: {},
  spatial: null,
  temporal: null,
  onAdd: () => {},
};

export default EditorBody;
