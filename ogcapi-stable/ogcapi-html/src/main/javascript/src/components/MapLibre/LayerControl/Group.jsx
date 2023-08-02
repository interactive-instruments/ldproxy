/* eslint-disable no-nested-ternary */
import React from "react";
import PropTypes from "prop-types";
import { Collapse, Row, Col, FormGroup, Label, Input } from "reactstrap";
import Layer from "./Layer";
import CollapseButton from "./CollapseButton";

const Group = ({ parent, style, isOpened, isSelected, onSelect, onOpen }) => {
  if (parent.type === "source-layer") {
    return (
      parent.subLayers &&
      parent.subLayers.map((subLayer, j) => (
        <Collapse
          key={subLayer.id}
          isOpen={isOpened(parent.id)}
          style={{ paddingBottom: j === parent.subLayers.length - 1 ? "5px" : null }}
        >
          <Row>
            <Layer
              layer={subLayer}
              isSelected={isSelected}
              onSelect={onSelect}
              style={style}
              level={2}
            />
          </Row>
        </Collapse>
      ))
    );
  }

  return parent.entries.map((entry, i) => {
    return (
      <Collapse
        key={entry.id}
        isOpen={isOpened(parent.id)}
        id={`collapse-${entry.id}`}
        style={{ paddingBottom: i === parent.entries.length - 1 ? "5px" : null }}
      >
        <Row key={entry.id}>
          {parent.isBasemap !== true && entry.type === "source-layer" ? (
            <>
              <Col xs="10" style={{ display: "flex", alignItems: "center" }}>
                <FormGroup check style={{ marginLeft: "20px" }}>
                  <Label check>
                    <Input
                      type="checkbox"
                      id={`checkbox-${parent.id}`}
                      checked={isSelected(entry.id)}
                      onChange={(e) => {
                        e.target.blur();
                        onSelect(entry.id);
                      }}
                    />
                    {entry.id}
                  </Label>
                </FormGroup>
              </Col>
              <Col xs="2">
                <CollapseButton
                  collapsed={!isOpened(entry.id)}
                  toggleCollapsed={() => onOpen(entry.id)}
                />
              </Col>
            </>
          ) : parent.type === "source-layer" ? (
            <Layer
              layer={entry}
              isSelected={isSelected}
              onSelect={onSelect}
              style={style}
              level={1}
            />
          ) : (
            <Layer
              layer={entry}
              isSelected={isSelected}
              onSelect={onSelect}
              style={style}
              level={1}
              radioGroup={parent.id}
            />
          )}
        </Row>
        {entry.subLayers &&
          entry.subLayers.map((subLayer, j) => (
            <Collapse
              key={subLayer.id}
              isOpen={isOpened(entry.id)}
              style={{ paddingBottom: j === entry.subLayers.length - 1 ? "5px" : null }}
            >
              <Row>
                <Layer
                  layer={subLayer}
                  isSelected={isSelected}
                  onSelect={onSelect}
                  style={style}
                  level={2}
                />
              </Row>
            </Collapse>
          ))}
      </Collapse>
    );
  });
};

Group.displayName = "Group";

Group.propTypes = {
  // eslint-disable-next-line react/forbid-prop-types
  parent: PropTypes.object.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  style: PropTypes.object.isRequired,
  isOpened: PropTypes.func.isRequired,
  isSelected: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  onOpen: PropTypes.func.isRequired,
};

Group.defaultProps = {};

export default Group;
