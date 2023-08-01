/* eslint-disable jsx-a11y/no-static-element-interactions */
/* eslint-disable jsx-a11y/click-events-have-key-events */
import React from "react";
import PropTypes from "prop-types";
import { Container, Row, Col, Input, FormGroup, Label } from "reactstrap";
import Entries from "./EntriesLayer";
import CollapseButton from "./CollapseButton";

const ParentGroups = ({
  layerControlVisible,
  parents,
  isSubLayerOpen,
  selected,
  selectedBasemap,
  setSelected,
  setSelectedBasemap,
  allParentGroups,
  open,
  setOpen,
  subLayerIds,
  mapHeight,
  style,
}) => {
  const onSelectParent = () => {
    if (selected.every((id) => !subLayerIds.includes(id))) {
      setSelected([...selected, ...subLayerIds]);
    } else {
      setSelected(selected.filter((id) => !subLayerIds.includes(id)));
    }
  };

  const parentCheck = () => {
    return subLayerIds.every((id) => selected.includes(id));
  };

  const onOpenParent = (entry) => {
    if (entry.isBasemap !== true) {
      const entryIds = [entry.id, ...entry.entries.map((e) => e.id)];
      const subLayerIds2 = entry.entries.flatMap((e) => e.subLayers.map((subLayer) => subLayer.id));
      const idsToRemove = [...entryIds, ...subLayerIds2];

      const index = open.indexOf(entry.id);
      if (index < 0) {
        open.push(entry.id);
        setOpen([...open]);
      } else {
        setOpen(open.filter((ids) => !idsToRemove.includes(ids)));
      }
    } else {
      const index = open.indexOf(entry.id);
      if (index < 0) {
        open.push(entry.id);
        setOpen([...open]);
      } else {
        setOpen(open.filter((ids) => !entry.id.includes(ids)));
      }
    }
  };

  return (
    <>
      <Container
        fluid
        id="layer-control"
        className="maplibregl-ctrl maplibregl-ctrl-group"
        style={{
          display: layerControlVisible ? "block" : "none",
          minWidth: "275px",
          maxHeight: `${mapHeight * 0.75}px`,
          overflow: "auto",
          scrollbarWidth: "thin",
          scrollbarColor: "darkgrey #f1f1f1",
        }}
      >
        {parents.map((parent, i) =>
          parent.id ? (
            <React.Fragment key={`${parent.id}fragment`}>
              <Row
                key={parent.id}
                id={parent.id}
                style={{
                  paddingTop: "5px",
                  paddingBottom: isSubLayerOpen(parent.id) ? null : "5px",
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
                          checked={parentCheck()}
                          onChange={(e) => {
                            e.target.blur();
                            onSelectParent();
                          }}
                        />
                        {parent.id}
                      </Label>
                    </FormGroup>
                  </Col>
                )}
                <Col xs="2">
                  <CollapseButton
                    collapsed={!isSubLayerOpen(parent.id)}
                    toggleCollapsed={() => onOpenParent(parent)}
                  />
                </Col>
              </Row>
              <Entries
                key={`${parent.id}entries`}
                parent={parent}
                isSubLayerOpen={isSubLayerOpen}
                selected={selected}
                selectedBasemap={selectedBasemap}
                setSelected={setSelected}
                setSelectedBasemap={setSelectedBasemap}
                allParentGroups={allParentGroups}
                open={open}
                setOpen={setOpen}
                style={style}
              />
            </React.Fragment>
          ) : null
        )}
      </Container>
    </>
  );
};

ParentGroups.displayName = "ParentGroups";

ParentGroups.propTypes = {
  layerControlVisible: PropTypes.bool.isRequired,
  parents: PropTypes.arrayOf(PropTypes.object).isRequired,
  isSubLayerOpen: PropTypes.func.isRequired,
  selected: PropTypes.arrayOf(PropTypes.string).isRequired,
  selectedBasemap: PropTypes.arrayOf(PropTypes.string).isRequired,
  setSelected: PropTypes.func.isRequired,
  setSelectedBasemap: PropTypes.func.isRequired,
  allParentGroups: PropTypes.arrayOf(PropTypes.object).isRequired,
  open: PropTypes.arrayOf(PropTypes.string).isRequired,
  setOpen: PropTypes.func.isRequired,
  subLayerIds: PropTypes.arrayOf(PropTypes.string).isRequired,
  mapHeight: PropTypes.number.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  style: PropTypes.object.isRequired,
};

ParentGroups.defaultProps = {};

export default ParentGroups;
