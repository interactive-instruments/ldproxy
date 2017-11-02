const resolve = require('path').resolve;
const webpack = require('webpack');
const webpackMerge = require('webpack-merge');
const CleanWebpackPlugin = require('clean-webpack-plugin');
const commonConfig = require('./webpack.config.common');

module.exports = function(env) {
return webpackMerge(commonConfig(env), {
    output: {
        filename: '[name].[chunkhash].js',
    },

    devtool: 'eval',

    plugins: [
        new CleanWebpackPlugin(['build']),

        new webpack.HashedModuleIdsPlugin(),

        new webpack.LoaderOptionsPlugin({
            minimize: true,
            debug: false
        }),

        new webpack.DefinePlugin({
            'process.env': {
                'NODE_ENV': JSON.stringify('production')
            }
        }),

        new webpack.optimize.UglifyJsPlugin({
        })
    ]
})
}

