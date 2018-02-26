/*
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
const resolve = require('path').resolve;
const webpackMerge = require('webpack-merge');
const CleanWebpackPlugin = require('clean-webpack-plugin');

module.exports = function(env) {
//console.log(env)
const config = require('xtraplatform-manager/webpack.config.' + (env || 'development'));

let newConfig = webpackMerge(config(env), {
    context: resolve(__dirname),
    output: {
        path: resolve('../resources/manager')
    },
    resolve: {
        alias: {
            react: resolve('./node_modules/react'),
        },
    },
})

if (env === 'production') {
    newConfig = webpackMerge(newConfig, {
        plugins: [
            new CleanWebpackPlugin(['resources/manager'], {
                root: resolve('../')
            })
        ]
    })
}

//console.log(newConfig)
return newConfig

}

