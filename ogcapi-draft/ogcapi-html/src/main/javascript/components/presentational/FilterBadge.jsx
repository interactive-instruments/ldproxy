import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { Button, ButtonDropdown, DropdownToggle } from 'reactstrap/dist/reactstrap.es';

export default class FilterBadge extends Component {

    render() {
        const {field, filter, showClose, onClose, add, remove} = this.props;

        let button = <Button color={ add ? 'success' : remove ? 'danger' : 'primary' }
                         disabled
                         size="sm"
                         className={ showClose ? 'py-0' : 'py-0 mr-1 my-1' }
                         style={ { opacity: '1' } }>
                         { filter }
                     </Button>

        // TODO: replace with ButtonGroup
        if (showClose) {
            button = <ButtonDropdown className="mr-1 my-1" isOpen={ false } toggle={ e => {
                                                                    e.target.blur();onClose(field);
                                                                } }>
                         { button }
                         <DropdownToggle color="danger" size="sm" className="py-0">
                             ×
                         </DropdownToggle>
                     </ButtonDropdown>
        }

        return button;
    }
}

/*
<Badge color={ filters[key].add ? 'success' : filters[key].remove ? 'danger' : 'primary' }
  className="mr-1"
  key={ key }
  style={ { fontSize: '60%' } }>
  { `${key}${key === 'bbox' ? '≈' : '='}${filters[key].value}` }
  { isOpen && <Badge color="light"
                  href=""
                  className="ml-2"
                  onClick={ e => this._clear(key) }>
                  ×
              </Badge> }
</Badge>
 */