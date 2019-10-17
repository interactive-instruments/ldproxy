/*
 * Copyright 2017 European Union
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * This work was supported by the EU Interoperability Solutions for
 * European Public Administrations Programme (https://ec.europa.eu/isa2)
 * through the ELISE action (European Location Interoperability Solutions 
 * for e-Government).
 */

import React, { Component } from 'react';
import { Text, Box } from 'grommet';
import PropTypes from 'prop-types';
import ui from 'redux-ui';
import EditTiles from './EditTiles'

const tilesExt = props => props.capabilities && props.capabilities.find(ext => ext.extensionType === 'TILES')

@ui({
    state: {
        extensions: (props) => props.capabilities || null,
        tiles: (props) => tilesExt(props) || null,
        formats: () => [],
        formatJsonArray: (props) => tilesExt(props) && tilesExt(props).formats ?
            Object.entries(tilesExt(props).formats).map(([key, value]) => {
                if (value.toString() === "application/json") {
                    return new Map([[value, true]]);
                }
                else {
                    return new Map([[value, false]])
                }
            }
            ) : true,
        formatJsonEnabled: () => null,
        formatMvtArray: (props) => tilesExt(props) && tilesExt(props).formats ?
            Object.entries(tilesExt(props).formats).map(([key, value]) => {
                if (value.toString() === "application/vnd.mapbox-vector-tile") {
                    return new Map([[value, true]])
                }
                else {
                    return new Map([[value, false]])
                }
            }
            ) : true,
        formatMvtEnabled: () => null,

        maxZoomLevel: (props) => tilesExt(props) && tilesExt(props).enabled && tilesExt(props).zoomLevels ? tilesExt(props).zoomLevels.WebMercatorQuad.max : 22,
        minZoomLevel: (props) => tilesExt(props) && tilesExt(props).enabled && tilesExt(props).zoomLevels ? tilesExt(props).zoomLevels.WebMercatorQuad.min : 0,
        maxSeeding: (props) => tilesExt(props) && tilesExt(props).enabled && tilesExt(props).seeding ? tilesExt(props).seeding.WebMercatorQuad.max : 0,
        minSeeding: (props) => tilesExt(props) && tilesExt(props).enabled && tilesExt(props).seeding ? tilesExt(props).seeding.WebMercatorQuad.min : 0,
    }
})





export default class ServiceEditTiles extends Component {
    render() {
        const { ui, updateUI } = this.props;

        return (
            ui.tiles && ui.tiles.enabled
                ? <EditTiles onChange={this.props.onChange} ui={ui} updateUI={updateUI} tilesEnabled={ui.tiles && ui.tiles.enabled} />
                : <Box pad={{ horizontal: 'small', vertical: 'medium' }} fill="horizontal"><Text>Disabled</Text></Box>
        );
    }






}

ServiceEditTiles.propTypes = {
    onChange: PropTypes.func.isRequired
};

ServiceEditTiles.defaultProps = {
};
