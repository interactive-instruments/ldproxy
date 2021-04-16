import React from "react";
import PropTypes from "prop-types";
import { useLocation, useHistory } from "react-router-dom";
import { useFassets } from "feature-u";

import { collectionEditTabs } from "../constants";
import Index from "./Index";

const ServiceEditCollections = ({
  serviceId,
  serviceLabel,
  collections,
  isCompact,
}) => {
  const location = useLocation();
  const history = useHistory();

  const onSelect = (collectionId, urlIsService) => {
    const prefix = urlIsService
      ? location.pathname
      : location.pathname.substr(0, location.pathname.lastIndexOf("/"));
    const search = urlIsService ? "" : location.search;
    history.push({
      pathname: `${prefix}/${collectionId}`,
      search: search,
    });
  };
  //console.log("COLL", collections);

  const tabs = useFassets(collectionEditTabs());
  //console.log("COLLTABS", tabs);

  return (
    <Index
      serviceId={serviceId}
      serviceLabel={serviceLabel}
      collections={collections}
      isCompact={isCompact}
      onSelect={onSelect}
    />
  );
};

ServiceEditCollections.displayName = "ServiceEditCollections";

ServiceEditCollections.propTypes = {
  onChange: PropTypes.func.isRequired,
};

export default ServiceEditCollections;
