import merge from 'deepmerge'

import { app, render } from 'xtraplatform-manager/src/module'
import wfsProxyMdl from 'xtraplatform-manager-wfs-proxy/src/module'
import mdl from './module'

//import 'xtraplatform-manager/src/scss/default'
//import './scss/ldproxy'

//TODO: merge by path
const emptyTarget = value => Array.isArray(value) ? [] : {}
const clone = (value, options) => merge(emptyTarget(value), value, options)

function combineMerge(target, source, options) {
    const destination = target.slice()

    source.forEach(function (e, i) {
        if (typeof destination[i] === 'undefined') {
            const cloneRequested = options.clone !== false
            const shouldClone = cloneRequested && options.isMergeableObject(e)
            destination[i] = shouldClone ? clone(e, options) : e
        } else if (options.isMergeableObject(e)) {
            destination[i] = merge(target[i], e, options)
        } else if (target.indexOf(e) === -1) {
            destination.push(e)
        }
    })
    return destination
}

const cfg = merge.all([app, wfsProxyMdl, mdl], {
    arrayMerge: combineMerge
})

console.log('HELLO', cfg)
render(cfg);

// Hot Module Replacement API
if (module && module.hot) {
    module.hot.accept('./module.js', () => {
        render(cfg);
    });
}
