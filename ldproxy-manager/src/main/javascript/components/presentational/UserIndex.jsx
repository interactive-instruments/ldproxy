import React, { Component } from 'react';
import PropTypes from 'prop-types';
import ui from 'redux-ui';

import { Box, Button, Paragraph, Text } from 'grommet';
import Header from 'xtraplatform-manager/src/components/common/Header';
import { List, ListItem } from 'xtraplatform-manager/src/components/common/List';

import { Add as AddIcon, Trash as TrashIcon, Group } from 'grommet-icons';

import Anchor from 'xtraplatform-manager/src/components/common/AnchorLittleRouter';
import LayerForm from 'xtraplatform-manager/src/components/common/LayerForm';

@ui({
    state: {
        layerOpened: false,
        toBeDeleted: null
    }
})

export default class UserIndex extends Component {

    _onLayerOpen = (event, id) => {
        event.preventDefault();
        event.stopPropagation();
        const { updateUI } = this.props;

        updateUI({
            layerOpened: true,
            toBeDeleted: id
        });
    }

    _onLayerClose = () => {
        const { updateUI } = this.props;

        updateUI({
            layerOpened: false,
            toBeDeleted: null
        });
    }

    _onRemove = () => {
        const { ui, deleteUser } = this.props;

        deleteUser(ui.toBeDeleted);
        this._onLayerClose();
    }

    render() {
        const { users, showUser, navControl, ui } = this.props;

        let layer;
        if (ui.layerOpened) {
            layer = <LayerForm title="Remove"
                submitLabel="Yes, remove"
                compact={true}
                onClose={this._onLayerClose}
                onSubmit={this._onRemove}>
                <Paragraph>
                    Are you sure you want to remove the user <strong>{ui.toBeDeleted}</strong>?
                            </Paragraph>
            </LayerForm>;
        }

        return (
            <div>
                <Box>
                    <Header justify="start" border={{ side: 'bottom', size: 'small', color: 'light-4' }}>
                        <Group />
                        <Text size="large" weight={500}>Users</Text>
                        <Anchor icon={<AddIcon />} path={{ pathname: '/users/add' }} title={`Add user`} />
                    </Header>
                    <Box margin={{ horizontal: 'small', vertical: 'medium' }}>
                        <List>
                            {users && Object.keys(users).map((key, index) => (
                                <ListItem key={key}>
                                    <Box>
                                        {users[key].label || users[key].id}
                                    </Box>
                                    <Box>
                                        {users[key].role}
                                    </Box>
                                    {users[key].role !== 'SUPERADMIN' ? <Button plain={true} icon={<TrashIcon />} onClick={(event) => this._onLayerOpen(event, key)} /> : <Box></Box>}
                                </ListItem>
                            ))}
                        </List>
                    </Box>
                </Box>
                {layer}
            </div>
        );
    }
}
