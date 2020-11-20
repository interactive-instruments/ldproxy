import React, { useState } from "react";

import { Box, Anchor, Paragraph } from "grommet";
import {
  MapLocation,
  Power as PowerIcon,
  Trash as TrashIcon,
  FolderOpen,
} from "grommet-icons";
import styled from "styled-components";

// import LayerForm from '../common/LayerForm';
const LayerForm = Box;

const Power = styled(Box)`
  & a {
    &:hover {
      & svg {
        stroke: ${(props) => props.theme.global.colors[props.hoverColor]};
      }
    }
  }
`;

const CollectionActions = (props) => {
  const [layerOpened, setLayerOpened] = useState(false);

  const _onLayerOpen = () => {
    setLayerOpened(true);
  };

  const _onLayerClose = () => {
    setLayerOpened(false);
  };

  const _onPower = (start) => {
    const { updateService } = props;

    updateService({
      shouldStart: start,
    });
  };

  const _onRemove = () => {
    const { id, removeService } = props;

    removeService({
      id,
    });
  };

  const { id, status, shouldStart, secured, token, viewActions } = props;
  const isOnline = status === "STARTED";
  const isDisabled = !isOnline && shouldStart;
  // not needed anymore, handled by cookies
  const parameters = ""; // secured ? `?token=${token}` : ''

  return (
    <Box flex={false}>
      <Box direction="row" justify="end">
        <Power
          hoverColor={
            isOnline
              ? "status-critical"
              : isDisabled
              ? "status-critical"
              : "status-ok"
          }
        >
          <Anchor
            icon={<PowerIcon />}
            title={`${
              isOnline ? "Hide" : isDisabled ? "Defective" : "Publish"
            }`}
            color={
              isOnline
                ? "status-ok"
                : isDisabled
                ? "status-critical"
                : "status-disabled"
            }
            onClick={() => _onPower(!isOnline)}
            disabled={isDisabled}
          />
        </Power>
        {viewActions.map((ViewAction) => (
          <ViewAction
            key={ViewAction.displayName}
            id={id}
            isOnline={isOnline}
            parameters={parameters}
          />
        ))}
        <Anchor icon={<TrashIcon />} title="Remove" onClick={_onLayerOpen} />
      </Box>
      {layerOpened && (
        <LayerForm
          title="Remove"
          submitLabel="Yes, remove"
          compact
          onClose={_onLayerClose}
          onSubmit={_onRemove}
        >
          <Paragraph>
            Are you sure you want to remove the service with id{" "}
            <strong>{id}</strong>?
          </Paragraph>
        </LayerForm>
      )}
    </Box>
  );
};

CollectionActions.displayName = "CollectionActions";

export default CollectionActions;
