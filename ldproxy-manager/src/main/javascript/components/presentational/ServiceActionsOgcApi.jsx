

import React from 'react';

import { Anchor } from 'grommet'
import { Home } from 'grommet-icons'

import ServiceApi from 'xtraplatform-manager/src/apis/ServiceApi'


export default props => {

    const { id, isOnline, parameters } = props;

    return (
        <>
            <Anchor
                icon={<Home />}
                title="Show landing page"
                href={`${ServiceApi.VIEW_URL}${id}/${parameters}`}
                target="_blank"
                disabled={!isOnline} />
        </>
    );
}
