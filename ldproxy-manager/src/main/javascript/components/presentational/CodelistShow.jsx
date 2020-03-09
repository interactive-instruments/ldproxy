import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { Box, Text } from 'grommet';
import Header from 'xtraplatform-manager/src/components/common/Header';
import { List, ListItem } from 'xtraplatform-manager/src/components/common/List';

import { LinkPrevious as LinkPreviousIcon } from 'grommet-icons';

import Anchor from 'xtraplatform-manager/src/components/common/AnchorLittleRouter';


export default class CodelistShow extends Component {

    render() {
        const { codelist, navControl } = this.props;

        return (
            codelist && <Box fill={true}>
                <Header justify='start' border={{ side: 'bottom', size: 'small', color: 'light-4' }}
                    size="large">
                    <Text size='large' weight={500}>{codelist.label}</Text>
                </Header>
                <Box fill={true}>
                    <Box fill="vertical" overflow={{ vertical: 'auto' }} pad={{ horizontal: 'small', vertical: 'medium' }} flex={false}>
                        <List>
                            {codelist && codelist.entries && Object.keys(codelist.entries).map((key, index) => (
                                <ListItem key={key} separator={index === 0 ? 'horizontal' : 'bottom'}>
                                    <Box direction='row' size='small' gap="medium" justify="between" fill="horizontal">
                                        <span>{key}</span>
                                        <span>{codelist.entries[key]}</span>
                                    </Box>
                                </ListItem>
                            ))}
                        </List>
                    </Box>
                </Box>
            </Box >
        );
    }
}
