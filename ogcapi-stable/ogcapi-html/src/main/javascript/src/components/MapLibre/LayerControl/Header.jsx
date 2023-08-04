import React from "react";
import PropTypes from "prop-types";
import { Row, Col, FormGroup, Label, Input } from "reactstrap";
import CollapseButton from "./CollapseButton";

// eslint-disable-next-line react/prop-types
const HeaderPlain = ({ label, level = 0 }) => (
  <span style={{ whiteSpace: "nowrap", marginLeft: `${level * 20}px` }}>{label}</span>
);

// eslint-disable-next-line react/prop-types
export const HeaderCheck = ({ id, level, radioGroup, isSelected, onSelect, children }) => (
  <FormGroup check style={{ marginLeft: `${level * 20}px` }}>
    <Label check style={{ display: "flex", alignItems: "center" }}>
      <Input
        style={{
          position: "relative",
          marginRight: "5px",
          marginTop: "0",
        }}
        type={radioGroup ? "radio" : "checkbox"}
        name={radioGroup}
        checked={isSelected(id, radioGroup)}
        onChange={(e) => {
          e.target.blur();
          onSelect(id, radioGroup);
        }}
      />
      {children}
    </Label>
  </FormGroup>
);

HeaderCheck.displayName = "HeaderCheck";

HeaderCheck.propTypes = {
  id: PropTypes.string.isRequired,
  level: PropTypes.number,
  radioGroup: PropTypes.string,
  isSelected: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
};

HeaderCheck.defaultProps = {
  level: 0,
  radioGroup: undefined,
};

const Header = ({ id, label, level, check, isOpened, onOpen, isSelected, onSelect }) => {
  return (
    <Row
      key={id}
      style={{
        flexWrap: "nowrap",
      }}
    >
      <Col xs="10" style={{ display: "flex", alignItems: "center" }}>
        {check ? (
          <HeaderCheck id={id} level={level} isSelected={isSelected} onSelect={onSelect}>
            <HeaderPlain label={label || id} />
          </HeaderCheck>
        ) : (
          <HeaderPlain label={label || id} level={level} />
        )}
      </Col>
      <Col xs="2">
        <CollapseButton collapsed={!isOpened(id)} toggleCollapsed={() => onOpen(id)} />
      </Col>
    </Row>
  );
};

Header.displayName = "Header";

Header.propTypes = {
  id: PropTypes.string.isRequired,
  label: PropTypes.string,
  level: PropTypes.number,
  check: PropTypes.bool,
  isOpened: PropTypes.func.isRequired,
  onOpen: PropTypes.func.isRequired,
  isSelected: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
};

Header.defaultProps = {
  label: undefined,
  level: 0,
  check: false,
};

export default Header;
