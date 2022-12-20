import React from "react";
import PropTypes from "prop-types";
import styled from "styled-components";

import { Tabs, Tab } from "grommet";
import { Content } from "@xtraplatform/core";

import Header from "./Header";
import Main from "./Main";

const StyledTab = styled(Tab)`
  text-align: center;
  flex-grow: 1;
  cursor: default;
`;

const CollectionIndex = ({
  serviceId,
  serviceLabel,
  collections,
  isCompact,
  onSelect,
}) => {
  if (isCompact) {
    return (
      <Content
        header={<Header serviceId={serviceId} serviceLabel={serviceLabel} />}
        main={
          <Tabs justify="stretch" margin={{ top: "small" }}>
            <StyledTab title="Collections">
              <Main
                collections={collections}
                isCompact={isCompact}
                onSelect={onSelect}
              />
            </StyledTab>
          </Tabs>
        }
      />
    );
  }

  return (
    <Main collections={collections} isCompact={isCompact} onSelect={onSelect} />
  );
};

CollectionIndex.displayName = "CollectionIndex";

CollectionIndex.propTypes = {
  onSelect: PropTypes.func.isRequired,
};

export default CollectionIndex;
