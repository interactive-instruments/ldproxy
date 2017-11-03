import merge from 'deepmerge'

import { app, render } from 'xtraplatform-manager/src/module'
import wfsProxyMdl from 'xtraplatform-manager-wfs-proxy/src/module'
import mdl from './module'

import 'xtraplatform-manager/src/scss/default'
//import './scss/ldproxy'

const cfg = merge.all([app, wfsProxyMdl, mdl])

console.log('HELLO', cfg)
render(cfg);

// Hot Module Replacement API
if (module && module.hot) {
    module.hot.accept('./module.js', () => {
        render(cfg);
    });
}
