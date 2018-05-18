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

import Codelists from '../container/Codelists'

import TextInputUi from 'xtraplatform-manager/src/components/common/TextInputUi';
import CheckboxUi from 'xtraplatform-manager/src/components/common/CheckboxUi';
import SelectUi from 'xtraplatform-manager/src/components/common/SelectUi';
import { shallowDiffers } from 'xtraplatform-manager/src/util';
import MappingEdit from 'xtraplatform-manager-wfs-proxy/src/components/presentational/MappingEdit'

const initState = {
    format: (props) => props.mapping.format || '',
    codelist: (props) => props.mapping.codelist || '',
    filterable: (props) => props.mapping.filterable || false
}

@ui({
    state: initState
})

class MappingEditGeneral extends Component {

    render() {
        let {ui, updateUI, title, mimeType, mapping, isFeatureType, isSaving, onChange, codelists} = this.props;
        const isSpatial = mapping.type === 'SPATIAL';
        const isTemporal = mapping.type === 'TEMPORAL';
        const isValue = mapping.type === 'VALUE';

        let options = [
            {
                value: '',
                label: 'None'
            }
        ];
        if (codelists) {
            options = options.concat(Object.values(codelists).map(cl => {
                return {
                    value: cl.resourceId,
                    label: cl.name
                }
            }))
        }
        const value = codelists && codelists[ui.codelist] ? {
            value: ui.codelist,
            label: codelists[ui.codelist].name
        } : undefined;

        const show = {
            name: !isFeatureType && !isSpatial,
            codelist: !isFeatureType && isValue,
            format: !isFeatureType && (isValue || isTemporal),
            filterable: !isFeatureType && (isValue || isTemporal || isSpatial)
        }

        return (
            <MappingEdit title={ title }
                heading="h3"
                smaller={ false }
                mimeType={ mimeType }
                mapping={ mapping }
                isFeatureType={ isFeatureType }
                showName={ show.name }
                isSaving={ isSaving }
                onChange={ onChange }
                initStateExt={ initState }>
                { show.codelist && <FormField label="Codelist">
                                       <SelectUi name="codelist"
                                           placeHolder='None'
                                           options={ options }
                                           value={ value }
                                           onChange={ updateUI } />
                                   </FormField> }
                { show.format && <FormField label="Format">
                                     <TextInputUi name="format" value={ ui.format } onChange={ updateUI } />
                                 </FormField> }
                { show.filterable && <FormField label={ isSpatial ? "Use for bbox filters" : isTemporal ? "Use for time filters" : "Usable in filters" }>
                                         <CheckboxUi name="filterable"
                                             checked={ ui.filterable }
                                             toggle={ false }
                                             reverse={ false }
                                             onChange={ updateUI } />
                                     </FormField> }
            </MappingEdit>
        );
    }
}

export default class MappingEditGeneralWrapper extends Component {

    render() {
        return (
            <Codelists>
                <div>
                    <MappingEditGeneral {...this.props} />
                </div>
            </Codelists>
        );
    }
}
