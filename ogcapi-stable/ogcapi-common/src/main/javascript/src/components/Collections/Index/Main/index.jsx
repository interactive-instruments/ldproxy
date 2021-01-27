import React from "react";
import PropTypes from "prop-types";
import { useParams } from "react-router-dom";

import { Box } from "grommet";
import { List, ListItem } from "@xtraplatform/core";

const CollectionIndexMain = ({ collections = {}, isCompact, onSelect }) => {
  const { cid } = useParams();
  const collections2 = collections || {}; //TODO: why do neither the props nor the deconstruct defaults work?

  return (
    <Box
      pad={{ horizontal: "small", vertical: "medium" }}
      fill={true}
      overflow={{ vertical: "auto", horizontal: "hidden" }}
    >
      <List>
        {Object.keys(collections2).map((key, i) => (
          <ListItem
            key={key}
            selected={collections2[key].id === cid}
            separator={i === 0 ? "horizontal" : "bottom"}
            hover={true}
            onClick={(e) => {
              e.target.blur();
              onSelect(collections2[key].id, !isCompact);
            }}
          >
            {collections2[key].label}
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
