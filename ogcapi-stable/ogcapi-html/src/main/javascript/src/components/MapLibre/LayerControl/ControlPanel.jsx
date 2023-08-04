import React from "react";
import PropTypes from "prop-types";
import { Container, Row } from "reactstrap";
import Group from "./Group";
import Layer from "./Layer";
import Header from "./Header";
import Separator from "./Separator";

const ControlPanel = ({
  entries,
  style,
  maxHeight,
  isVisible,
  isOpened,
  isSelected,
  onSelect,
  onOpen,
}) => {
  let lastWasSingle = true;

  return (
    <>
      <Container
        fluid
        id="layer-control"
        className="maplibregl-ctrl maplibregl-ctrl-group"
        style={{
          display: isVisible ? "block" : "none",
          minWidth: "275px",
          maxHeight: `${maxHeight}px`,
          paddingBottom: "5px",
          overflow: "auto",
          scrollbarWidth: "thin",
          scrollbarColor: "darkgrey #f1f1f1",
        }}
      >
        {entries.map((entry, i) => {
          const isSingle = entry.isLayer || entry.type === "merge-group";
          const newSection = i === 0 || !lastWasSingle || !isSingle;
          lastWasSingle = isSingle;

          return entry.isLayer ? (
            <React.Fragment key={entry.id}>
              <Separator section={newSection} first={i === 0} />
              <Row
                key={entry.id}
                style={{
                  flexWrap: "nowrap",
                }}
              >
                <Layer
                  id={entry.id}
                  label={entry.label}
                  icons={[entry]}
                  isSelected={isSelected}
                  onSelect={onSelect}
                  style={style}
                />
              </Row>
            </React.Fragment>
          ) : (
            <React.Fragment key={entry.id}>
              <Separator section={newSection} first={i === 0} />
              {entry.type !== "merge-group" && (
                <Header
                  id={entry.id}
                  label={entry.label}
                  check={entry.type !== "radio-group"}
                  isOpened={isOpened}
                  isSelected={isSelected}
                  onOpen={onOpen}
                  onSelect={onSelect}
                />
              )}
              <Group
                parent={entry}
                style={style}
                isOpened={isOpened}
                isSelected={isSelected}
                onSelect={onSelect}
                onOpen={onOpen}
              />
            </React.Fragment>
          );
        })}
      </Container>
    </>
  );
};

ControlPanel.displayName = "ControlPanel";

ControlPanel.propTypes = {
  entries: PropTypes.arrayOf(PropTypes.object).isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  style: PropTypes.object.isRequired,
  maxHeight: PropTypes.number.isRequired,
  isVisible: PropTypes.bool.isRequired,
  isOpened: PropTypes.func.isRequired,
  isSelected: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  onOpen: PropTypes.func.isRequired,
};

ControlPanel.defaultProps = {};

export default ControlPanel;
