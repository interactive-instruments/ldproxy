import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { Button, Row, Col, Collapse } from 'reactstrap/dist/reactstrap.es';
import { Map, TileLayer } from 'react-leaflet'
import qs from 'qs';
import moment from 'moment';

import LeafletBboxSelect from '../presentational/LeafletBboxSelect';
import FieldFilter from '../presentational/FieldFilter';
import BboxFilter from '../presentational/BboxFilter';
import SpatialFilter from '../presentational/SpatialFilter';
import FilterBadge from '../presentational/FilterBadge';
import TimeFilter, { toTimeLabel } from '../presentational/TimeFilter';

//import "bootstrap/scss/bootstrap";

export default class App extends Component {

    constructor(props) {
        super(props);

        const {fields} = window['_ldproxy'];

        const spatial = {locality: '', street: '', number: ''}

        const query = qs.parse(window.location.search, {
            ignoreQueryPrefix: true
        });

        const filters = Object.keys(fields).concat(['bbox', 'datetime']).reduce((filters, field) => {
            if (query[field]) {
                filters[field] = {
                    value: field === 'bbox'
                        ? query[field].split(',')
                        : query[field]
                }
            }
            return filters;
        }, {})

        const spatialSearch = Object.keys(spatial).reduce((filters, field) => {
                    if (query[field]) {
                        filters[field] = query[field]
                    }
                    return filters;
                }, {})

        this.state = {
            isOpen: false,
            isShown: false,
            filters: filters,
            spatialSearch: spatialSearch,
            spatial: spatial
        };
    }

    _addFilter = (filter) => {
        const {filters} = this.state;

        if (filter.field && filter.field !== '' && filter.value && filter.value !== '') {
            this.setState({
                filters: {
                    ...filters,
                    [filter.field]: {
                        value: filter.value,
                        add: true
                    }
                }
            })
        }
    }

    _applySpatial = (spatial) => {
        const spatialFields = {locality: '', street: '', number: ''}

        const query = qs.parse(window.location.search, {
            ignoreQueryPrefix: true
        });
        delete query['offset'];
        Object.keys(spatialFields).forEach(field => {
            delete query[field];
            if (spatial[field] && spatial[field] !== '') {
                query[field] = spatial[field];
            }
        })

        window.location.search = qs.stringify(query, {
            addQueryPrefix: true
        });
    }

    _apply = () => {
        const {filters} = this.state;
        const {fields} = window['_ldproxy'];

        const newFilters = Object.keys(filters).reduce((newFilters, key) => {
            if (filters[key].add || !filters[key].remove) {
                newFilters[key] = {
                    ...filters[key],
                    add: false,
                    remove: false
                }
            }
            return newFilters;
        }, {})

        const query = qs.parse(window.location.search, {
            ignoreQueryPrefix: true
        });
        delete query['offset'];
        Object.keys(fields).concat(['bbox', 'datetime']).forEach(field => {
            delete query[field];
            if (newFilters[field]) {
                query[field] = field === 'bbox'
                    ? newFilters[field].value.join(',')
                    : newFilters[field].value;
            }
        })

        window.location.search = qs.stringify(query, {
            addQueryPrefix: true
        });
    }

    _cancel = (event) => {
        const {filters} = this.state;

        event.target.blur();

        const newFilters = Object.keys(filters).reduce((newFilters, key) => {
            if (!filters[key].add) {
                newFilters[key] = {
                    ...filters[key],
                    add: false,
                    remove: false
                }
            }
            return newFilters;
        }, {})

        this.setState({
            filters: newFilters,
            isOpen: false
        })
    }

    _clear = (field) => {
        const {[field]: clear, ...rest} = this.state.filters;

        const newFilters = rest;
        if (!clear.add)
            newFilters[field] = {
                ...clear,
                remove: !clear.remove
        }
        this.setState({
            filters: newFilters
        })
    }

    _toggle = (event) => {
        const {isOpen} = this.state;

        event.target.blur();

        this.setState({
            isOpen: !isOpen
        })
    }

    _drawMap = () => {
        const {isShown} = this.state;
        if (!isShown) {
            this.setState({
                isShown: true
            })
        }
    }

    _getFilterLabel = (field) => {
        const {filters} = this.state;

        if (field === 'bbox') {
            return `bbox≈${parseFloat(filters[field].value[0]).toFixed(2)},${parseFloat(filters[field].value[1]).toFixed(2)},${parseFloat(filters[field].value[2]).toFixed(2)},${parseFloat(filters[field].value[3]).toFixed(2)}`
        }

        if (field === 'datetime') {
            return toTimeLabel(filters[field].value);
        }

        return `${field}=${filters[field].value}`
    }

    _getBounds = (bbox) => {
        return [[parseFloat(bbox[1]), parseFloat(bbox[0])], [parseFloat(bbox[3]), parseFloat(bbox[2])]]
    }

    render() {
        const {field, value, filters, isOpen, isShown, lat, lng, zoom, bbox} = this.state;
        const {fields, map, time, extensions} = window['_ldproxy'];

        const {spatial,spatialSearch} = this.state;

        return (
        <div>
            { extensions && extensions.spatialSearch && <div>
                <Row className="mb-1">
                    <Col md="12" className="d-flex flex-row justify-content-start align-items-center flex-wrap">
                    <span className="font-weight-bold">Räumliche Suche</span>
                    </Col>
                </Row>
                <Row className="mb-3">
                    <Col md="12" className="d-flex flex-row justify-content-start align-items-center flex-wrap">
                    <SpatialFilter onChange={this._applySpatial} {...spatialSearch}/>
                    </Col>
                </Row>
            </div>}
            <div>
                <Row className="mb-3">
                    <Col md="3" className="d-flex flex-row justify-content-start align-items-center flex-wrap">
                    <span className="mr-2 font-weight-bold" onClick={ this._toggle }>Filter</span>
                    <Button color={ isOpen ? 'primary' : 'secondary' }
                        outline={ !isOpen }
                        size="sm"
                        className="py-0"
                        onClick={ isOpen ? this._apply : this._toggle }>
                        { isOpen ? 'Apply' : 'Edit' }
                    </Button>
                    { isOpen && <Button color="danger"
                                    size="sm"
                                    className="ml-1 py-0"
                                    onClick={ this._cancel }>
                                    Cancel
                                </Button> }
                    </Col>
                    <Col md="9" className="d-flex flex-row justify-content-start align-items-center flex-wrap">
                    { Object.keys(filters).map(key => <FilterBadge key={ key }
                                                          field={ key }
                                                          filter={ this._getFilterLabel(key) }
                                                          add={ filters[key].add }
                                                          remove={ filters[key].remove }
                                                          showClose={ isOpen }
                                                          onClose={ this._clear } />) }
                    </Col>
                </Row>
                <Collapse isOpen={ isOpen } onEntered={ this._drawMap }>
                    <Row>
                        <Col md="6">
                        <FieldFilter fields={ Object.keys(fields).filter(k => !filters[k]).reduce((fs, k) => ({
                                                  ...fs,
                                                  [k]: fields[k]
                                              }), {}) } onChange={ this._addFilter } />
                        <BboxFilter bbox={ bbox } onChange={ this._addFilter } />
                        <TimeFilter start={ datetime.start }
                            end={ datetime.end }
                            filter={ filters.datetime ? filters.datetime.value : null }
                            onChange={ this._addFilter } />
                        </Col>
                        <Col md="6">
                        { isShown && <Map bounds={ filters.bbox ? this._getBounds(filters.bbox.value) : map.bounds } selectArea={ true } className="w-100 h-100">
                                         { /*TODO: get from template*/ }
                                         <TileLayer attribution={ map.attribution } url={ map.url } />
                                         <LeafletBboxSelect bounds={ filters.bbox ? this._getBounds(filters.bbox.value) : map.bounds } onChange={ bbox => this.setState({
                                                                                                                                                      bbox: bbox
                                                                                                                                                  }) } />
                                     </Map> }
                        </Col>
                    </Row>
                </Collapse>
            </div>
            </div>
        );
    }
}

