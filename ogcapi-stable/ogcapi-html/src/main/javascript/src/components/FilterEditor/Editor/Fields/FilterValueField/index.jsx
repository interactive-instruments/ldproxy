import React from "react";
import PropTypes from "prop-types";

import { Input } from "reactstrap";

const FilterValueField = ({
  code,
  filterKey,
  filters,
  changedValue,
  setChangedValue,
  enumKeys,
  integerKeys,
}) => {
  return (
    <>
      {
        // eslint-disable-next-line no-nested-ternary
        enumKeys.includes(filterKey) ? (
          <Input
            type="select"
            size="sm"
            name="value"
            className="mr-2"
            defaultValue={filters[filterKey].value}
            onChange={(e) =>
              setChangedValue({
                ...changedValue,
                [filterKey]: {
                  filterKey,
                  value: e.target.value,
                },
              })
            }
          >
            {Object.keys(code[filterKey]).map((item) => (
              <option value={code[filterKey][item]} key={item}>
                {code[filterKey][item]}
              </option>
            ))}
          </Input>
        ) : integerKeys.includes(filterKey) ? (
          <Input
            type="number"
            size="sm"
            name="value2"
            id={`input-${filterKey}`}
            placeholder={filters[filterKey].value}
            defaultValue={filters[filterKey].value}
            className="mr-2"
            onChange={(e) =>
              setChangedValue({
                ...changedValue,
                [filterKey]: {
                  filterKey,
                  value: e.target.value,
                },
              })
            }
          />
        ) : (
          <Input
            type="text"
            size="sm"
            name="selectedValue"
            id={`input-${filterKey}`}
            className="mr-2"
            placeholder={filters[filterKey].value}
            defaultValue={filters[filterKey].value}
            onChange={(e) =>
              setChangedValue({
                ...changedValue,
                [filterKey]: {
                  filterKey,
                  value: e.target.value,
                },
              })
            }
          />
        )
      }
    </>
  );
};

export default FilterValueField;

FilterValueField.propTypes = {
  filterKey: PropTypes.string.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  filters: PropTypes.object.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  code: PropTypes.object.isRequired,
  changedValue: PropTypes.oneOfType([PropTypes.object, PropTypes.string]).isRequired,
  setChangedValue: PropTypes.func.isRequired,
  enumKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
  integerKeys: PropTypes.arrayOf(PropTypes.string).isRequired,
};

FilterValueField.defaultProps = {};
