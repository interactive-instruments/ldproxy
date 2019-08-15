import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { Box, Heading } from 'grommet';
import Header from 'xtraplatform-manager/src/components/common/Header';
import { List, ListItem } from 'xtraplatform-manager/src/components/common/List';

import { LinkPrevious as LinkPreviousIcon } from 'grommet-icons';

import Anchor from 'xtraplatform-manager/src/components/common/AnchorLittleRouter';


export default class CodelistShow extends Component {

    render() {
        const { codelist, navControl } = this.props;

        return (
            codelist
                ? <Box>
                    <Header pad={{ horizontal: "small", between: 'small', vertical: "medium" }}
                        justify="start"
                        size="large"
                        colorIndex="light-2">
                        <Anchor icon={<LinkPreviousIcon />} path="/codelists" a11yTitle="Return" />
                        <Heading tag="h1"
                            margin="none"
                            strong={true}
                            truncate={true}>
                            {codelist.name}
                        </Heading>
                    </Header>
                    <Box as='section'>
                        <List>
                            {codelist && codelist.entries && Object.keys(codelist.entries).map((key, index) => (
                                <ListItem key={key} separator={index === 0 ? 'horizontal' : 'bottom'}>
                                    <Box direction='row' size='small'>
                                        <span>{key}</span>
                                        <span>{codelist.entries[key]}</span>
                                    </Box>
                                </ListItem>
                            ))}
                        </List>
                    </Box>
                </Box>
                : <span>not found</span>
        );
    }
}
