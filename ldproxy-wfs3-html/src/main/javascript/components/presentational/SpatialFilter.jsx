import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { Button, Form, FormGroup, Label, Input, Row, Col } from 'reactstrap/dist/reactstrap.es';

export default class SpatialFilter extends Component {

    constructor(props) {
        super(props);
        const {locality, street, number} = this.props;
        this.state = {
            locality: locality,
            street: street,
            number: number
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

        event.preventDefault();
        event.stopPropagation();

        onChange(this.state);
    }

    render() {
        const {locality, street, number} = this.state;

        return (
            <Form onSubmit={ this._apply } className="w-100">
                <Row>
                    <Col md="3">
                    <FormGroup>
                        <Input type="text"
                            size="sm"
                            name="locality"
                            id="locality"
                            className="mr-1"
                            value={ locality }
                            placeholder={ 'Ort' }
                             onChange={ this._handleInputChange }/>
                    </FormGroup>
                    </Col>
                    <Col md="3">
                    <FormGroup>
                        <Input type="text"
                            size="sm"
                            name="street"
                            id="street"
                            className="mr-1"
                            value={ street }
                            placeholder={ 'Strasse' }
                             onChange={ this._handleInputChange }/>
                    </FormGroup>
                    </Col>
                    <Col md="3">
                    <FormGroup>
                        <Input type="text"
                            size="sm"
                            name="number"
                            id="number"
                            className="mr-1"
                            value={ number }
                            placeholder={ 'Hausnummer' }
                             onChange={ this._handleInputChange }/>
                    </FormGroup>
                    </Col>
                    <Col md="3">
                    <FormGroup>
                        <Button color='primary'
                            size="sm"
                            onClick={ this._apply }>
                            { 'Suchen' }
                        </Button>
                    </FormGroup>
                    </Col>
                </Row>
            </Form>
        );
    }
}

SpatialFilter.propTypes = {
    locality: PropTypes.string,
    street: PropTypes.string,
    number: PropTypes.string,
    onChange: PropTypes.func.isRequired
};

SpatialFilter.defaultProps = {
    locality: undefined,
    street: undefined,
    number: undefined,
};