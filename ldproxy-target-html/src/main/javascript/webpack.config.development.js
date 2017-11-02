/*
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
const resolve = require('path').resolve;
const webpack = require('webpack');
const webpackMerge = require('webpack-merge');
const commonConfig = require('./webpack.config.common');
const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = function(env) {
return webpackMerge.strategy({
    entry: 'prepend'
}
)(commonConfig(env), {
    entry: [
        'react-hot-loader/patch'
    ],
    output: {
        publicPath: '/'
    },

    devtool: 'eval',

    plugins: [
        new webpack.HotModuleReplacementPlugin(),

        new webpack.NamedModulesPlugin(),

        new HtmlWebpackPlugin({
            //title: 'XtraPlatform Manager',
            //favicon: 'assets/img/favicon.png',
            template: resolve(__dirname, 'index.html')
        })
    ],

    devServer: {
        port: 7090,
        hot: true,
        stats: 'normal',
        contentBase: resolve('../resources/manager'),
        publicPath: '/',
        overlay: {
            warnings: true,
            errors: true
        },
        historyApiFallback: true
    }
})
}
