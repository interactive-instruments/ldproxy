import React, { useState } from 'react';
import PropTypes from 'prop-types';

import {
    Button,
    Form,
    FormGroup,
    Input,
    FormText,
    Row,
    Col,
} from 'reactstrap/dist/reactstrap.es';

const FieldFilter = ({ fields, onAdd }) => {
    const [field, setField] = useState('');
    const [value, setValue] = useState('');

    const selectField = (event) =>
        setField(event.option ? event.option.value : event.target.value);

    const saveValue = (event) => setValue(event.target.value);

    const save = () => {
        onAdd(field, value);
    };

    return (
        <Form onSubmit={save}>
            <p className='text-muted text-uppercase'>field</p>
            <Row>
                <Col md='5'>
                    <FormGroup>
                        <Input
                            type='select'
                            size='sm'
                            name='field'
                            className={`mr-2${
                                field === '' ? ' text-muted' : ''
                            }`}
                            value={field}
                            onChange={selectField}>
                            <option value='' className='d-none'>
                                none
                            </option>
                            {Object.keys(fields).map((f) => (
                                <option value={f} key={f}>
                                    {fields[f]}
                                </option>
                            ))}
                        </Input>
                    </FormGroup>
                </Col>
                <Col md='5'>
                    <FormGroup>
                        <Input
                            type='text'
                            size='sm'
                            name='value'
                            placeholder='filter pattern'
                            className='mr-2'
                            value={value}
                            onChange={saveValue}
                        />
                        <FormText>Use * as wildcard</FormText>
                    </FormGroup>
                </Col>
                <Col md='2'>
                    <Button
                        color='primary'
                        size='sm'
                        disabled={field === '' || value === ''}
                        onClick={save}>
                        Add
                    </Button>
                </Col>
            </Row>
        </Form>
    );
};

FieldFilter.displayName = 'FieldFilter';

FieldFilter.propTypes = {
    fields: PropTypes.objectOf(PropTypes.string).isRequired,
    onAdd: PropTypes.func.isRequired,
};

FieldFilter.defaultProps = {};

export default FieldFilter;
