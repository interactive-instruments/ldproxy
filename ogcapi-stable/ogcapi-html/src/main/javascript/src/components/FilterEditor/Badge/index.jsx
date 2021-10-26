import React from 'react';
// import PropTypes from 'prop-types';

import {
    Button,
    ButtonDropdown,
    DropdownToggle,
} from 'reactstrap/dist/reactstrap.es';

const FilterBadge = ({
    field,
    value,
    isEditable,
    isAdd,
    isRemove,
    onRemove,
}) => {
    const label = `${field}=${value}`;

    let button = (
        <Button
            // eslint-disable-next-line no-nested-ternary
            color={isAdd ? 'success' : isRemove ? 'danger' : 'primary'}
            disabled
            size='sm'
            className={isEditable ? 'py-0' : 'py-0 mr-1 my-1'}
            style={{ opacity: '1' }}>
            {label}
        </Button>
    );

    // TODO: replace with ButtonGroup
    if (isEditable) {
        button = (
            <ButtonDropdown
                className='mr-1 my-1'
                isOpen={false}
                toggle={(e) => {
                    e.target.blur();
                    onRemove(field);
                }}>
                {button}
                <DropdownToggle color='danger' size='sm' className='py-0'>
                    ×
                </DropdownToggle>
            </ButtonDropdown>
        );
    }

    return button;
};

FilterBadge.displayName = 'FilterBadge';

FilterBadge.propTypes = {};

FilterBadge.defaultProps = {};

export default FilterBadge;
