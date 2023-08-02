import React from "react";
import PropTypes from "prop-types";
import { Container, Row, Col, Input, FormGroup, Label } from "reactstrap";
import Group from "./Group";
import CollapseButton from "./CollapseButton";

const ControlPanel = ({
  parents,
  style,
  mapHeight,
  isVisible,
  isOpened,
  isSelected,
  onSelect,
  onOpen,
}) => {
  return (
    <>
      <Container
        fluid
        id="layer-control"
        className="maplibregl-ctrl maplibregl-ctrl-group"
        style={{
          display: isVisible ? "block" : "none",
          minWidth: "275px",
          maxHeight: `${mapHeight * 0.75}px`,
          overflow: "auto",
          scrollbarWidth: "thin",
          scrollbarColor: "darkgrey #f1f1f1",
        }}
      >
        {parents.map(
          (parent, i) =>
            parent.id && (
              <React.Fragment key={`${parent.id}fragment`}>
                <Row
                  key={parent.id}
                  id={parent.id}
                  style={{
                    paddingTop: "5px",
                    paddingBottom: isOpened(parent.id) ? null : "5px",
                    flexWrap: "nowrap",
                    borderTop: i > 0 ? "1px solid #ddd" : null,
                  }}
                >
                  {parent.isBasemap ? (
                    <Col xs="10" style={{ display: "flex", alignItems: "center" }}>
                      {parent.id}
                    </Col>
                  ) : (
                    <Col xs="10" style={{ display: "flex", alignItems: "center" }}>
                      <FormGroup check>
                        <Label check>
                          <Input
                            type="checkbox"
                            id={`checkbox-${parent.id}`}
                            checked={isSelected(parent.id)}
                            onChange={(e) => {
                              e.target.blur();
                              onSelect(parent.id);
                            }}
                          />
                          {parent.id}
                        </Label>
                      </FormGroup>
                    </Col>
                  )}
                  <Col xs="2">
                    <CollapseButton
                      collapsed={!isOpened(parent.id)}
                      toggleCollapsed={() => onOpen(parent.id)}
                    />
                  </Col>
                </Row>
                <Group
                  key={`${parent.id}entries`}
                  parent={parent}
                  style={style}
                  isOpened={isOpened}
                  isSelected={isSelected}
                  onSelect={onSelect}
                  onOpen={onOpen}
                />
              </React.Fragment>
            )
        )}
      </Container>
    </>
  );
};

ControlPanel.displayName = "ControlPanel";

ControlPanel.propTypes = {
  parents: PropTypes.arrayOf(PropTypes.object).isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  style: PropTypes.object.isRequired,
  mapHeight: PropTypes.number.isRequired,
  isVisible: PropTypes.bool.isRequired,
  isOpened: PropTypes.func.isRequired,
  isSelected: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  onOpen: PropTypes.func.isRequired,
};

ControlPanel.defaultProps = {};

export default ControlPanel;
