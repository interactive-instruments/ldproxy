import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";

import { Button, Row, Col } from "reactstrap";
import qs from "qs";

import Badge from "./Badge";
import Editor from "./Editor";

const toBounds = (filter) => {
  const a = filter.split(",");
  const b = [
    [parseFloat(a[0]), parseFloat(a[1])],
    [parseFloat(a[2]), parseFloat(a[3])],
  ];
  return b;
};

const FilterEditor = ({
  backgroundUrl,
  attribution,
  spatial,
  temporal,
  fields,
  code,
  start,
  end,
  bounds,
  titleForFilter,
}) => {
  const [isOpen, setOpen] = useState(false);

  const enabled = Object.keys(fields).length > 0 || spatial || temporal;

  // eslint-disable-next-line no-undef
  const query = qs.parse(window.location.search, {
    ignoreQueryPrefix: true,
  });

  const [filters, setFilters] = useState({});

  useEffect(() => {
    const parsedQuery = qs.parse(window.location.search, {
      ignoreQueryPrefix: true,
    });
    const filterFields = Object.keys(fields).concat(["bbox", "datetime"]);
    const existingFilters = filterFields.reduce((result, field) => {
      if (parsedQuery[field]) {
        result[field] = {
          value: parsedQuery[field],
          add: false,
          remove: false,
        };
      }
      return result;
    }, {});
    setFilters(existingFilters);
  }, [fields]);

  const toggle = (event) => {
    event.target.blur();

    setOpen(!isOpen);
  };

  const onAdd = (field, value) => {
    setFilters((prev) => ({
      ...prev,
      [field]: { value, add: true, remove: false },
    }));
  };

  const onRemove = (field) => {
    setFilters((prev) => ({
      ...prev,
      [field]: { value: prev[field].value, add: false, remove: true },
    }));
  };

  const save = (event) => {
    event.target.blur();

    const newFilters = Object.keys(filters).reduce((reduced, key) => {
      if (filters[key].add || !filters[key].remove) {
        // eslint-disable-next-line no-param-reassign
        reduced[key] = {
          ...filters[key],
          add: false,
          remove: false,
        };
      }
      return reduced;
    }, {});

    delete query.offset;

    Object.keys(fields)
      .concat(["bbox", "datetime"])
      .forEach((field) => {
        delete query[field];
        if (newFilters[field]) {
          query[field] = newFilters[field].value;
        }
      });

    // eslint-disable-next-line no-undef
    window.location.search = qs.stringify(query, {
      addQueryPrefix: true,
    });
  };

  const deleteFilters = (field) => () => {
    setFilters((current) => {
      const copy = { ...current };
      delete copy[field];
      return copy;
    });
  };

  const cancel = (event) => {
    event.target.blur();

    const newFilters = Object.keys(filters).reduce((reduced, key) => {
      if (!filters[key].add) {
        // eslint-disable-next-line no-param-reassign
        reduced[key] = {
          ...filters[key],
          add: false,
          remove: false,
        };
      }
      return reduced;
    }, {});

    setFilters(newFilters);
    setOpen(false);
  };

  return (
    <>
      <Row className="mb-3">
        <Col md="3" className="d-flex flex-row justify-content-start align-items-center flex-wrap">
          <span className="mr-2 font-weight-bold">Filter</span>
          {enabled && (
            <Button
              color={isOpen ? "primary" : "secondary"}
              outline={!isOpen}
              size="sm"
              className="py-0"
              onClick={isOpen ? save : toggle}
            >
              {isOpen ? "Apply" : "Edit"}
            </Button>
          )}
          {isOpen && (
            <Button color="danger" size="sm" className="ml-1 py-0" onClick={cancel}>
              Cancel
            </Button>
          )}
        </Col>
        <Col md="9" className="d-flex flex-row justify-content-start align-items-center flex-wrap">
          {Object.keys(filters).map((key) => (
            <Badge
              key={key}
              field={key}
              value={filters[key].value}
              isAdd={filters[key].add}
              isRemove={filters[key].remove}
              isEditable={isOpen}
              onRemove={onRemove}
            />
          ))}
        </Col>
      </Row>
      <Editor
        isOpen={isOpen}
        fields={fields}
        backgroundUrl={backgroundUrl}
        attribution={attribution}
        spatial={filters.bbox ? toBounds(filters.bbox.value) : spatial}
        temporal={temporal}
        filters={filters}
        onAdd={onAdd}
        deleteFilters={deleteFilters}
        code={code}
        titleForFilter={titleForFilter}
        start={start}
        end={end}
        bounds={bounds}
      />
    </>
  );
};

FilterEditor.displayName = "FilterEditor";

FilterEditor.propTypes = {
  fields: PropTypes.objectOf(PropTypes.string).isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  code: PropTypes.object.isRequired,
  start: PropTypes.string.isRequired,
  end: PropTypes.string.isRequired,
  bounds: PropTypes.arrayOf(PropTypes.number).isRequired,
  titleForFilter: PropTypes.objectOf(PropTypes.string).isRequired,
  backgroundUrl: PropTypes.string,
  attribution: PropTypes.string,
  spatial: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)),
  temporal: PropTypes.objectOf(PropTypes.number),
};

FilterEditor.defaultProps = {
  backgroundUrl: "https://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png",
  attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors',
  spatial: null,
  temporal: null,
};

export default FilterEditor;
