import React from "react";
import PropTypes from "prop-types";
import { useParams } from "react-router-dom";

import Collections from "../";

import { Async } from "@xtraplatform/core";
import { useService } from "@xtraplatform/services";

const CollectionsStandalone = ({ isCompact }) => {
  const { id } = useParams();
  const { loading, error, data } = useService(id);

  const service = data ? data.service : {};

  return (
    <Async loading={loading} error={error}>
      <Collections
        serviceId={service.id}
        serviceLabel={service.label}
        collections={service.collections}
        isCompact={isCompact}
      />
    </Async>
  );
};

CollectionsStandalone.displayName = "CollectionsStandalone";

CollectionsStandalone.propTypes = {};

export default CollectionsStandalone;
