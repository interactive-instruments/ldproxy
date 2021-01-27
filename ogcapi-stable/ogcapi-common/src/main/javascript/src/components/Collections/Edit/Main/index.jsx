import React from "react";
import PropTypes from "prop-types";
import { useFassets } from "feature-u";
import { Tabs } from "@xtraplatform/core";
import { useTranslation } from "react-i18next";

import { collectionEditTabs } from "../../../constants";

const CollectionEditMain = ({
  serviceId,
  collection,
  defaults,
  debounce,
  onPending,
  onChange,
}) => {
  const extEditTabs = useFassets(collectionEditTabs());
  const { t } = useTranslation();

  const editTabs = [...extEditTabs];
  //TODO
  const token = null;

  return (
    <Tabs
      tabs={editTabs}
      tabProps={{
        ...collection,
        serviceId,
        defaults,
        token,
        isCollection: true,
        inheritedLabel: t("services/ogc_api:services.withId._label", {
          id: serviceId,
        }),
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
