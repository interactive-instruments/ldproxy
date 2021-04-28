import React from "react";
import PropTypes from "prop-types";

import { Analytics } from "grommet-icons";
import { Header, AsyncIcon } from "@xtraplatform/core";

const CollectionEditHeader = ({
  collection,
  mutationPending,
  mutationLoading,
  mutationError,
  mutationSuccess,
}) => {
  return (
    <Header
      icon={<Analytics />}
      label={collection.label}
      title={`${collection.label} [${collection.id}]`}
      actions2={
        <AsyncIcon
          size="list"
          pending={mutationPending}
          loading={mutationLoading}
          success={mutationSuccess}
          error={mutationError}
        />
      }
    />
  );
};

CollectionEditHeader.displayName = "CollectionEditHeader";

CollectionEditHeader.propTypes = {
  compact: PropTypes.bool,
  role: PropTypes.string,
};

export default CollectionEditHeader;
