import React, { Component } from 'react';
import PropTypes from 'prop-types';
import ui from 'redux-ui';

import { Box, Button, Paragraph, Text } from 'grommet';
import Header from 'xtraplatform-manager/src/components/common/Header';
import { List, ListItem } from 'xtraplatform-manager/src/components/common/List';

import { Add as AddIcon, Trash as TrashIcon } from 'grommet-icons';

import Anchor from 'xtraplatform-manager/src/components/common/AnchorLittleRouter';
import LayerForm from 'xtraplatform-manager/src/components/common/LayerForm';

@ui({
    state: {
        layerOpened: false,
        toBeDeleted: null
    }
})

export default class CodelistIndex extends Component {

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
        const { ui, deleteCodelist } = this.props;

        deleteCodelist(ui.toBeDeleted);
        this._onLayerClose();
    }

    render() {
        const { codelists, showCodelist, navControl, ui } = this.props;

        let layer;
        if (ui.layerOpened) {
            layer = <LayerForm title="Remove"
                submitLabel="Yes, remove"
                compact={true}
                onClose={this._onLayerClose}
                onSubmit={this._onRemove}>
                <fieldset>
                    <Paragraph>
                        Are you sure you want to remove the codelist <strong>{codelists[ui.toBeDeleted].name}</strong>?
                            </Paragraph>
                </fieldset>
            </LayerForm>;
        }

        return (
            <Box fill={true}>
                <Header justify='start' border={{ side: 'bottom', size: 'small', color: 'light-4' }}
                    size="large">
                    <Text size="large" weight={500}>Codelists</Text>
                    <Anchor icon={<AddIcon />} path={{ pathname: '/codelists/add' }} title={`Add codelist`} />
                </Header>
                <Box fill={true}>
                    <Box fill="vertical" overflow={{ vertical: 'auto' }} pad={{ horizontal: 'small', vertical: 'medium' }} flex={false}>
                        <List>
                            {codelists && Object.keys(codelists).map((key, index) => (
                                <ListItem key={key}
                                    pad={{ vertical: 'none' }}
                                    separator={index === 0 ? 'horizontal' : 'bottom'}
                                    onClick={() => showCodelist(key)}
                                    style={{ cursor: 'pointer' }}>
                                    <Box direction='row' size="medium" justify="between" fill="horizontal">
                                        <Box pad='small'>
                                            {codelists[key].label || codelists[key].id}
                                        </Box>
                                        <Button plain={true} icon={<TrashIcon />} onClick={(event) => this._onLayerOpen(event, key)} title="Remove" />
                                    </Box>
                                </ListItem>
                            ))}
                        </List>
                    </Box>
                </Box>
                {layer}
            </Box>
        );
    }
}
