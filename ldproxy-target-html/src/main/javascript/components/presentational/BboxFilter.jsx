import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { Button, Form, FormGroup, Label, Input, Row, Col } from 'reactstrap/dist/reactstrap.es';

export default class BboxFilter extends Component {

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

        event.preventDefault();
        event.stopPropagation();

        onChange({
            field: 'bbox',
            value: this._getBboxString()
        });
    }

    _getBboxString = () => {
        const {bbox} = this.props;
        return [bbox.getWest(), bbox.getSouth(), bbox.getEast(), bbox.getNorth()]
    //return `${bbox.getWest().toFixed(2)},${bbox.getSouth().toFixed(2)},${bbox.getEast().toFixed(2)},${bbox.getNorth().toFixed(2)}`
    }

    render() {
        const {fields} = this.props;
        const {field, value} = this.state;
        const {_northEast, _southWest} = this.props.bbox

        return (
            <Form onSubmit={ this._apply }>
                <p className="text-muted text-uppercase">
                    bbox
                </p>
                <Row>
                    <Col md="5">
                    <FormGroup>
                        <Input type="text"
                            size="sm"
                            name="minLng"
                            id="minLng"
                            className="mr-2"
                            value={ _southWest.lng }
                            readOnly={ true } />
                    </FormGroup>
                    </Col>
                    <Col md="5">
                    <FormGroup>
                        <Input type="text"
                            size="sm"
                            name="minLat"
                            id="minLat"
                            className="mr-2"
                            value={ _southWest.lat }
                            readOnly={ true } />
                    </FormGroup>
                    </Col>
                </Row>
                <Row>
                    <Col md="5">
                    <FormGroup>
                        <Input type="text"
                            size="sm"
                            name="maxLng"
                            id="maxLng"
                            className="mr-2"
                            value={ _northEast.lng }
                            readOnly={ true } />
                    </FormGroup>
                    </Col>
                    <Col md="5">
                    <FormGroup>
                        <Input type="text"
                            size="sm"
                            name="maxLat"
                            id="maxLat"
                            className="mr-2"
                            value={ _northEast.lat }
                            readOnly={ true } />
                    </FormGroup>
                    </Col>
                    <Col md="2">
                    <Button color="primary" size="sm" onClick={ this._apply }>
                        Add
                    </Button>
                    </Col>
                </Row>
            </Form>
        );
    }
}

BboxFilter.propTypes = {
    bbox: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
};

BboxFilter.defaultProps = {
    bbox: {
        _southWest: {
            lat: 0,
            lng: 0
        },
        _northEast: {
            lat: 0,
            lng: 0
        }
    }
};