import React, { Component } from 'react';
import PropTypes from 'prop-types';

import Section from 'grommet/components/Section';
import Box from 'grommet/components/Box';
import Header from 'grommet/components/Header';
import Heading from 'grommet/components/Heading';
import List from 'grommet/components/List';
import ListItem from 'grommet/components/ListItem';
import Columns from 'grommet/components/Columns';
import LinkPreviousIcon from 'grommet/components/icons/base/LinkPrevious';

import Anchor from 'xtraplatform-manager/src/components/common/AnchorLittleRouter';


export default class CodelistShow extends Component {

    render() {
        const {codelist, navControl} = this.props;

        return (
        codelist
            ? <Box>
                  <Header pad={ { horizontal: "small", between: 'small', vertical: "medium" } }
                      justify="start"
                      size="large"
                      colorIndex="light-2">
                      <Anchor icon={ <LinkPreviousIcon /> } path="/codelists" a11yTitle="Return" />
                      <Heading tag="h1"
                          margin="none"
                          strong={ true }
                          truncate={ true }>
                          { codelist.name }
                      </Heading>
                  </Header>
                  <Section>
                      <List>
                          { codelist && Object.keys(codelist.entries).map((key, index) => (
                                <ListItem key={ key } separator={ index === 0 ? 'horizontal' : 'bottom' }>
                                    <Columns size='small'>
                                        <span>{ key }</span>
                                        <span>{ codelist.entries[key] }</span>
                                    </Columns>
                                </ListItem>
                            )) }
                      </List>
                  </Section>
              </Box>
            : <span>not found</span>
        );
    }
}
