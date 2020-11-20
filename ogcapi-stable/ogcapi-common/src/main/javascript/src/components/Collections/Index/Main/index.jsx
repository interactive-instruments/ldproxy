import React from "react";
import PropTypes from "prop-types";
import { useParams } from "react-router-dom";

import { Box } from "grommet";
import { List, ListItem } from "@xtraplatform/core";

const CollectionIndexMain = ({ collections, isCompact, onSelect }) => {
  const { cid } = useParams();

  return (
    <Box pad={{ horizontal: "small", vertical: "medium" }} fill="horizontal">
      <List>
        {Object.keys(collections).map((key, i) => (
          <ListItem
            key={key}
            selected={collections[key].id === cid}
            separator={i === 0 ? "horizontal" : "bottom"}
            hover={true}
            onClick={(e) => {
              e.target.blur();
              onSelect(collections[key].id, !isCompact);
            }}
          >
            {collections[key].label}
          </ListItem>
        ))}
      </List>
    </Box>
  );
};

CollectionIndexMain.displayName = "CollectionIndexMain";

CollectionIndexMain.propTypes = {
  onSelect: PropTypes.func.isRequired,
};

export default CollectionIndexMain;
