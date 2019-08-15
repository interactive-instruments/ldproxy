import React, { Component } from 'react';
import { connect } from 'react-redux'
import { push } from 'redux-little-router'
import { mutateAsync, requestAsync } from 'redux-query';
import ui from 'redux-ui';

import {Box, Heading, Button, Select, Form, FormField} from 'grommet';
import Header from 'xtraplatform-manager/src/components/common/Header';

import {LinkPrevious as LinkPreviousIcon} from 'grommet-icons';

import CodelistApi from '../../apis/CodelistApi'
import TextInputUi from 'xtraplatform-manager/src/components/common/TextInputUi';
import Anchor from 'xtraplatform-manager/src/components/common/AnchorLittleRouter';



@ui({
    state: {
        sourceUrl: '',
        sourceType: 'GML_DICTIONARY'
    }
})

@connect(
    (state, props) => {
        return {}
    },
    (dispatch) => {
        return {
            addCodelist: (codelist) => {
                dispatch(mutateAsync(CodelistApi.addCodelistQuery(codelist)))
                    .then((result) => {
                        dispatch(push('/codelists'))
                    })
            }
        }
    })

export default class CodelistAdd extends Component {

    _addCodelist = (event) => {
        event.preventDefault();
        const {ui, addCodelist} = this.props;

        addCodelist(ui);
    }

    render() {
        const {ui, updateUI, children} = this.props;

        return (
            <div>
                <Header pad={ { horizontal: "small", vertical: "medium" } }
                    justify="between"
                    size="large"
                    colorIndex="light-2">
                    <Box direction="row"
                        align="center"
                        pad={ { between: 'small' } }
                        responsive={ false }>
                        <Anchor icon={ <LinkPreviousIcon /> } path={ '/codelists' } a11yTitle="Return" />
                        <Heading tag="h1" margin="none">
                            <strong>Import Codelist</strong>
                        </Heading>
                    </Box>
                    { /*sidebarControl*/ }
                </Header>
                <Form compact={ false } plain={ true } pad={ { horizontal: 'large', vertical: 'medium' } }>
                            <FormField label="URL" style={ { width: '100%' } }>
                                <TextInputUi name="sourceUrl"
                                    autoFocus
                                    value={ ui.sourceUrl }
                                    onChange={ updateUI } />
                            </FormField>
                            <FormField label="Format">
                                <Select name="type"
                                    value={ { value: 'GML_DICTIONARY', label: 'GML Dictionary' } }
                                    options={ [{ value: 'GML_DICTIONARY', label: 'GML Dictionary' }] }
                                    onChange={ () => {
                                               } } />
                            </FormField>
                            { children }
                    <Box as='footer' pad={ { "vertical": "medium" } }>
                        <Button label='Add' primary={ true } onClick={ (ui.sourceUrl.length < 11) ? null : this._addCodelist } />
                    </Box>
                </Form>
            </div>
        );
    }
}
