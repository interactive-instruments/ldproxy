import React, { Component } from 'react';
import { connect } from 'react-redux'
import { push } from 'redux-little-router'
import { mutateAsync, requestAsync } from 'redux-query';
import ui from 'redux-ui';
import uiValidator, { url } from 'xtraplatform-manager/src/components/common/ui-validator';

import { Box, Text, Button, Select, FormField } from 'grommet';
import Header from 'xtraplatform-manager/src/components/common/Header';

import CodelistApi from '../../apis/CodelistApi'
import TextInputUi from 'xtraplatform-manager/src/components/common/TextInputUi';
import { withAppConfig } from 'xtraplatform-manager/src/app-context';

@withAppConfig()

@ui({
    state: {
        sourceUrl: '',
        sourceType: 'GML_DICTIONARY'
    }
})

@uiValidator({
    sourceUrl: url()
})

@connect(
    (state, props) => {
        return {}
    },
    (dispatch, props) => {
        return {
            addCodelist: (codelist) => {
                dispatch(mutateAsync(CodelistApi.addCodelistQuery(codelist, { secured: props.appConfig.secured })))
                    .then((result) => {
                        dispatch(push('/codelists'))
                    })
            }
        }
    })

export default class CodelistAdd extends Component {

    _addCodelist = (event) => {
        event.preventDefault();
        const { ui, addCodelist } = this.props;

        addCodelist(ui);
    }

    render() {
        const { ui, updateUI, children, validator } = this.props;

        return (
            <Box fill={true}>
                <Header justify='start' border={{ side: 'bottom', size: 'small', color: 'light-4' }}
                    size="large">
                    <Text size='large' weight={500}>Import Codelist</Text>
                </Header>
                <Box pad={{ horizontal: 'small', vertical: 'medium' }}>
                    <Box fill="vertical" overflow={{ vertical: 'auto' }} flex={false}>
                        <FormField label="URL" style={{ width: '100%' }} error={validator.messages.sourceUrl}>
                            <TextInputUi name="sourceUrl"
                                autoFocus
                                value={ui.sourceUrl}
                                onChange={updateUI} />
                        </FormField>
                        <FormField label="Format">
                            <Select name="type"
                                labelKey="label"
                                valueKey="value"
                                value={{ value: 'GML_DICTIONARY', label: 'GML Dictionary' }}
                                options={[{ value: 'GML_DICTIONARY', label: 'GML Dictionary' }]}
                                onChange={() => {
                                }} />
                        </FormField>
                        {children}
                        <Box pad={{ "vertical": "medium" }}>
                            <Button label='Add' primary={true} onClick={validator.valid ? this._addCodelist : null} />
                        </Box>
                    </Box>
                </Box>
            </Box>
        );
    }
}
