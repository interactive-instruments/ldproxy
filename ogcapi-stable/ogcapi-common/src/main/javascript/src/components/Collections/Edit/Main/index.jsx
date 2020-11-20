import React from "react";
import PropTypes from "prop-types";
import { useFassets } from "feature-u";
import { Tabs } from "@xtraplatform/core";

import { collectionEditTabs } from "../../../constants";

const CollectionEditMain = ({
  serviceId,
  collection,
  debounce,
  onPending,
  onChange,
}) => {
  const extEditTabs = useFassets(collectionEditTabs());

  const editTabs = [...extEditTabs];
  //TODO
  const token = null;

  return (
    <Tabs
      tabs={editTabs}
      tabProps={{
        ...collection,
        serviceId,
        token,
        isCollection: true,
        debounce,
        onPending,
        onChange,
      }}
    />
  );
};

CollectionEditMain.displayName = "CollectionEditMain";

CollectionEditMain.propTypes = {};

export default CollectionEditMain;
