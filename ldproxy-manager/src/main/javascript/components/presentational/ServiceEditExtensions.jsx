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
import ui from 'redux-ui';


import { Box } from 'grommet';

import CheckboxUi from 'xtraplatform-manager/src/components/common/CheckboxUi';


@ui({
    state: {
        capabilities: (props) => props.capabilities ? props.capabilities : null
    }
})

export default class ServiceEditExtensions extends Component {

    _save = () => {
        const { ui, onChange } = this.props;

        onChange({ capabilities: ui.capabilities });
    }


    render() {
        const { ui, updateUI } = this.props;

        const newCapabilities = (type, change) => ui.capabilities.map(ext => ext.extensionType === type ? { ...ext, ...change } : ext)

        return (

            ui.capabilities &&

            <Box pad={{ horizontal: 'small', vertical: 'medium' }} fill="horizontal">

                {ui.capabilities.map(ext => <Box pad={{ bottom: 'small' }} key={ext.extensionType}>
                    <CheckboxUi
                        toggle={true}
                        name={"enabled"}
                        label={ext.extensionType}
                        checked={ext.enabled}
                        onChange={(field, value) => updateUI("capabilities", newCapabilities(ext.extensionType, { [field]: value }))}
                        onDebounce={this._save}
                    />
                </Box>)}

            </Box>

        );
    }
}

