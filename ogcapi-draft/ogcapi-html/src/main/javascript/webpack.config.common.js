/*
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
const resolve = require('path').resolve;
const webpack = require('webpack');

module.exports = function(env) {

return {
    //    context: resolve(__dirname, 'src'),

    entry: [
        './index.jsx'
    ],
    output: {
        filename: '[name].js',
        path: resolve(__dirname, 'build')
    },

    devtool: 'eval',

    watchOptions: {
        ignored: /node_modules/,
    },

    resolve: {
        extensions: [".js", ".jsx", ".json", ".css"]
    },

    module: {
        rules: [
            {
                test: /\.jsx?$/,
                use: [{
                    loader: 'babel-loader',
                }],
                exclude: /node_modules(?!\/xtraplatform)/
            },
            {
                test: /\.css$/,
                use: [{
                    loader: "style-loader"
                }, {
                    loader: "css-loader"
                }]
            }
        ],
    },

    plugins: [

        new webpack.optimize.CommonsChunkPlugin({
            name: 'vendor',
            minChunks: function(module) {
                return module.context && (module.context.indexOf('node_modules') !== -1 || module.context.indexOf('vendor') !== -1);
            }
        }),
        new webpack.optimize.CommonsChunkPlugin({
            name: 'manifest'
        })

    ],
}
};
