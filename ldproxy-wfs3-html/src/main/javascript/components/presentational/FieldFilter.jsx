import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { Button, Form, FormGroup, Label, Input, FormText, Row, Col } from 'reactstrap/dist/reactstrap.es';

export default class FieldFilter extends Component {

    constructor(props) {
        super(props);
        this.state = {
            field: '',
            value: ''
        };
    }

    _handleInputChange = (event) => {
        if (event) {
            const target = event.target;
            const value = target.type === 'checkbox' ? target.checked : (event.option ? event.option.value : target.value);
            const field = target.name;

            this.setState({
                [field]: value
            })
        }
    }

    _apply = (event) => {
        const {onChange} = this.props;
        const {field, value} = this.state;

        event.preventDefault();
        event.stopPropagation();

        if (field !== '' && value !== '') {

            onChange(this.state)

            this.setState({
                field: '',
                value: ''
            })
        }
    }

    render() {
        const {fields} = this.props;
        const {field, value} = this.state;

        return (
            <Form onSubmit={ this._apply }>
                <p className="text-muted text-uppercase">
                    field
                </p>
                <Row>
                    <Col md="5">
                    <FormGroup>
                        <Input type="select"
                            size="sm"
                            name="field"
                            className={ "mr-2" + (field === '' ? ' text-muted' : '') }
                            value={ field }
                            onChange={ this._handleInputChange }>
                        <option value="" className="d-none">
                            none
                        </option>
                        { Object.keys(fields).map(f => <option value={ f } key={ f }>
                                                           { fields[f] }
                                                       </option>) }
                        </Input>
                    </FormGroup>
                    </Col>
                    <Col md="5">
                    <FormGroup>
                        <Input type="text"
                            size="sm"
                            name="value"
                            placeholder="filter pattern"
                            className="mr-2"
                            value={ value }
                            onChange={ this._handleInputChange } />
                        <FormText>
                            Use * as wildcard
                        </FormText>
                    </FormGroup>
                    </Col>
                    <Col md="2">
                    <Button color="primary"
                        size="sm"
                        disabled={ field === '' || value === '' }
                        onClick={ this._apply }>
                        Add
                    </Button>
                    </Col>
                </Row>
            </Form>
        );
    }
}

FieldFilter.propTypes = {
    fields: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
};