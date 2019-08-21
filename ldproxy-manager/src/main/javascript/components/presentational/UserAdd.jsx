import React, { Component } from 'react';
import { connect } from 'react-redux'
import { push } from 'redux-little-router'
import { mutateAsync, requestAsync } from 'redux-query';
import ui from 'redux-ui';

import { Box, Text, Button, Select, Form, FormField } from 'grommet';
import { UserAdd as UserAddIcon } from 'grommet-icons'
import Header from 'xtraplatform-manager/src/components/common/Header';

import UserApi from '../../apis/UserApi'
import TextInputUi from 'xtraplatform-manager/src/components/common/TextInputUi';
import SelectUi from 'xtraplatform-manager/src/components/common/SelectUi';
import Anchor from 'xtraplatform-manager/src/components/common/AnchorLittleRouter';



@ui({
    state: {
        id: '',
        role: 'USER',
        password: ''
    }
})

@connect(
    (state, props) => {
        return {}
    },
    (dispatch) => {
        return {
            addUser: (user) => {
                dispatch(mutateAsync(UserApi.addUserQuery(user)))
                    .then((result) => {
                        dispatch(push('/users'))
                    })
            }
        }
    })

export default class UserAdd extends Component {

    _addUser = (event) => {
        event.preventDefault();
        const { ui, addUser } = this.props;

        addUser(ui);
    }

    render() {
        const { ui, updateUI, children } = this.props;

        return (
            <Box fill={true}>
                <Header justify='start' border={{ side: 'bottom', size: 'small', color: 'light-4' }}
                    size="large">
                    <UserAddIcon />
                    <Text size='large' weight={500}>New User</Text>
                </Header>
                <Box pad={{ horizontal: 'small', vertical: 'medium' }}>
                    <Box fill="vertical" overflow={{ vertical: 'auto' }} flex={false}>
                        <FormField label="Name">
                            <TextInputUi name="id"
                                autoFocus
                                value={ui.id}
                                onChange={updateUI} />
                        </FormField>
                        <FormField label="Password">
                            <TextInputUi name="password"
                                type="password"
                                value={ui.password}
                                onChange={updateUI} />
                        </FormField>
                        <FormField label="Role">
                            <SelectUi name="role"
                                value={ui.role}
                                options={['USER', 'EDITOR', 'ADMIN']}
                                onChange={updateUI} />
                        </FormField>
                        {children}
                        <Box pad={{ "vertical": "medium" }}>
                            <Button label='Add' primary={true} onClick={(ui.id.length < 2) ? null : this._addUser} />
                        </Box>
                    </Box>
                </Box>
            </Box>
        );
    }
}
