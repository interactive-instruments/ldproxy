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
import PropTypes from 'prop-types';
import ui from 'redux-ui';

import FormField from 'grommet/components/FormField';

import TextInputUi from 'xtraplatform-manager/src/components/common/TextInputUi';
import CheckboxUi from 'xtraplatform-manager/src/components/common/CheckboxUi';
import SelectUi from 'xtraplatform-manager/src/components/common/SelectUi';
import MappingEdit from 'xtraplatform-manager-wfs-proxy/src/components/presentational/MappingEdit'
import { shallowDiffers } from 'xtraplatform-manager/src/util';

const initState = {
    type: (props) => props.mapping.type || 'NONE',
    format: (props) => props.mapping.format || '',
    showInCollection: (props) => props.mapping.showInCollection || false,
    itemType: (props) => props.mapping.itemType || '',
    itemProp: (props) => props.mapping.itemProp || ''
}

@ui({
    state: initState
})

export default class MappingEditHtml extends Component {

    /*shouldComponentUpdate(nextProps) {
        const {ui, updateUI} = this.props;


        if (shallowDiffers(ui, nextProps.ui)) {
            //console.log('UP', ui, nextProps.ui)
            return true;
        } else if (shallowDiffers(ui, nextProps.mapping, true)) {
            //console.log('MAP', ui, nextProps.mapping)

            updateUI(
                Object.keys(initState).reduce((state, key) => {
                    state[key] = initState[key](nextProps);
                    return state;
                }, {})
            )
        }

        return false;
    }*/

    render() {
        let {ui, updateUI, mapping, baseMapping, mimeType, isFeatureType, isSaving, onChange} = this.props;

        return (
            <MappingEdit title={ mimeType }
                mimeType={ mimeType }
                mapping={ mapping }
                baseMapping={ baseMapping }
                isFeatureType={ isFeatureType }
                isSaving={ isSaving }
                onChange={ onChange }
                initStateExt={ initState }>
                { !isFeatureType && <FormField label="Format">
                                        <TextInputUi name="format"
                                            placeHolder={ baseMapping.format }
                                            value={ ui.format }
                                            onChange={ updateUI } />
                                    </FormField> }
                { !isFeatureType && <FormField label="LD Type">
                                        <TextInputUi name="itemProp" value={ ui.itemProp } onChange={ updateUI } />
                                    </FormField> }
                { isFeatureType && <FormField label="LD Type">
                                       <TextInputUi name="itemType" value={ ui.itemType } onChange={ updateUI } />
                                   </FormField> }
                { !isFeatureType && <FormField label="Show in collection">
                                        <CheckboxUi name="showInCollection"
                                            checked={ ui.showInCollection }
                                            toggle={ false }
                                            reverse={ false }
                                            onChange={ updateUI } />
                                    </FormField> }
            </MappingEdit>
        );
    }
}
