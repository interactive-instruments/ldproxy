import React, { useCallback, useState } from "react";
import PropTypes from "prop-types";
import { useParams } from "react-router-dom";

import { Content, Async } from "@xtraplatform/core";
import {
  useService,
  useServiceStatus,
  useServicePatch,
  patchDebounce,
} from "@xtraplatform/services";
import Header from "./Header";
import Main from "./Main";

const CollectionEdit = () => {
  const { id, cid } = useParams();
  const { loading, error, data } = useServiceStatus(id);
  const { loading: loading2, error: error2, data: data2 } = useService(id);
  const [
    patchService,
    { loading: mutationLoading, error: mutationError, data: mutationSuccess },
  ] = useServicePatch(id);
  const [mutationPending, setPending] = useState(false);

  const status = data ? data.status : {};
  const service = data2 ? data2.service : {};
  const collection = service.collections ? service.collections[cid] : {};

  const onPending = () => setPending(true);

  const onChange = useCallback(
    (finalChanges) => {
      if (Object.keys(finalChanges).length > 0) {
        patchService({ collections: { [collection.id]: finalChanges } });
        setPending(false);
      }
    },
    [patchService, collection]
  );

  return (
    <Async loading={loading} error={error} noSpinner>
      <Content
        header={
          <Header
            collection={collection}
            mutationPending={mutationPending}
            mutationLoading={mutationLoading}
            mutationError={mutationError}
            mutationSuccess={mutationSuccess}
          />
        }
        main={
          <Async loading={loading2} error={error2}>
            <Main
              serviceId={service.id}
              collection={collection}
              defaults={{ ...service.defaultExtent }}
              debounce={patchDebounce}
              onPending={onPending}
              onChange={onChange}
            />
          </Async>
        }
      />
    </Async>
  );
};

CollectionEdit.displayName = "CollectionEdit";

CollectionEdit.propTypes = {
  isCompact: PropTypes.bool,
};

export default CollectionEdit;
