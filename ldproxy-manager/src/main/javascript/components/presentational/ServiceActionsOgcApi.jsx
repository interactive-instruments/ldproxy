

import React from 'react';

import { Anchor } from 'grommet'
import { FolderOpen } from 'grommet-icons'

import ServiceApi from 'xtraplatform-manager/src/apis/ServiceApi'


export default props => {

    const { id, isOnline, parameters } = props;

    return (
        <>
            <Anchor
                icon={<FolderOpen />}
                title="Show landing page"
                href={`${ServiceApi.VIEW_URL}${id}/${parameters}`}
                target="_blank"
                disabled={!isOnline} />
        </>
    );
}
