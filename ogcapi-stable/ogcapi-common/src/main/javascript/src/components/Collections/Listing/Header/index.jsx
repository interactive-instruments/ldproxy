import React from "react";
import PropTypes from "prop-types";

import { Box, Text } from "grommet";
import { Menu, Revert } from "grommet-icons";
import { Header, NavLink, IconLink, AsyncIcon } from "@xtraplatform/core";
import { useView } from "@xtraplatform/manager";

const CollectionIndexHeader = ({ serviceId, serviceLabel }) => {
  const [{}, { toggleMenu }] = useView();

  return (
    <Header
      icon={<IconLink onClick={toggleMenu} icon={<Menu />} title="Show menu" />}
      label={
        <NavLink
          to={`/services/${serviceId}?tab=collections`}
          label={
            <Box flex={false} direction="row" gap="xxsmall" align="center">
              <Text truncate size="large" weight={500}>
                {serviceLabel}
              </Text>
              <Revert size="list" color="light-5" />
            </Box>
          }
          title={`Go back to service '${serviceLabel}'`}
          flex
        />
      }
    />
  );
};

CollectionIndexHeader.displayName = "CollectionIndexHeader";

CollectionIndexHeader.propTypes = {
  onSelect: PropTypes.func.isRequired,
};

export default CollectionIndexHeader;
