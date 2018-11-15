import React, { Component } from 'react';
import PropTypes from 'prop-types';
import ui from 'redux-ui';

import Section from 'grommet/components/Section';
import Box from 'grommet/components/Box';
import Header from 'grommet/components/Header';
import Title from 'grommet/components/Title';
import List from 'grommet/components/List';
import ListItem from 'grommet/components/ListItem';
import Button from 'grommet/components/Button';
import Paragraph from 'grommet/components/Paragraph';
import Columns from 'grommet/components/Columns';
import AddIcon from 'grommet/components/icons/base/Add';
import TrashIcon from 'grommet/components/icons/base/Trash';

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
        const {updateUI} = this.props;

        updateUI({
            layerOpened: true,
            toBeDeleted: id
        });
    }

    _onLayerClose = () => {
        const {updateUI} = this.props;

        updateUI({
            layerOpened: false,
            toBeDeleted: null
        });
    }

    _onRemove = () => {
        const {ui, deleteCodelist} = this.props;

        deleteCodelist(ui.toBeDeleted);
        this._onLayerClose();
    }

    render() {
        const {codelists, showCodelist, navControl, ui} = this.props;

        let layer;
        if (ui.layerOpened) {
            layer = <LayerForm title="Remove"
                        submitLabel="Yes, remove"
                        compact={ true }
                        onClose={ this._onLayerClose }
                        onSubmit={ this._onRemove }>
                        <fieldset>
                            <Paragraph>
                                Are you sure you want to remove the codelist <strong>{ codelists[ui.toBeDeleted].name }</strong>?
                            </Paragraph>
                        </fieldset>
                    </LayerForm>;
        }

        return (
            <div>
                <Box>
                    <Header size='large' pad={ { horizontal: 'medium' } }>
                        <Title responsive={ false }>
                            { navControl }
                            <span>Codelists</span>
                        </Title>
                        <Anchor icon={ <AddIcon /> } path={ { pathname: '/codelists/add' } } title={ `Add codelist` } />
                    </Header>
                    <Section>
                        <List>
                            { codelists && Object.keys(codelists).map((key, index) => (
                                  <ListItem key={ key }
                                      pad={ { vertical: 'none' } }
                                      separator={ index === 0 ? 'horizontal' : 'bottom' }
                                      onClick={ () => showCodelist(key) }>
                                      <Columns size="medium">
                                          <Box pad='small'>
                                              { codelists[key].label || codelists[key].id }
                                          </Box>
                                          <Button plain={ true } icon={ <TrashIcon /> } onClick={ (event) => this._onLayerOpen(event, key) } />
                                      </Columns>
                                  </ListItem>
                              )) }
                        </List>
                    </Section>
                </Box>
                { layer }
            </div>
        );
    }
}
