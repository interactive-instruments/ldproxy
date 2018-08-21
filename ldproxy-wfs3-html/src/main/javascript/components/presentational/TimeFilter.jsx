import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { Button, ButtonGroup, Form, FormGroup, Label, Input, Row, Col } from 'reactstrap/dist/reactstrap.es';
import DatetimeRangePicker from 'react-datetime-range-picker';
import Datetime from 'react-datetime';

import moment from 'moment';

const fromFilterString = (filter) => {
    if (filter.indexOf('/') === -1) {
        return {
            start: filter,
            end: null
        };
    }

    return {
        start: filter.split('/')[0],
        end: filter.split('/')[1].indexOf('P') === 0 ? moment.utc(filter.split('/')[0]).add(moment.duration(filter.split('/')[1])).format() : filter.split('/')[1]
    }
}

export const toTimeLabel = (filter) => {
    const time = fromFilterString(filter);

    if (!time.end) {
        return `time=${moment.utc(time.start).format('DD.MM.YY HH:mm:ss')}`;
    }
    return `time=${moment.utc(time.start).format('DD.MM.YY HH:mm:ss')} - ${moment.utc(time.end).format('DD.MM.YY HH:mm:ss')}`;
}

export default class TimeFilter extends Component {

    constructor(props) {
        super(props);
        const {start, end, filter} = props;
        const extent = filter ? fromFilterString(filter) : {
            start,
            end
        }

        this.state = {
            instant: new Date(extent.start),
            period: {
                start: new Date(extent.start),
                end: new Date(extent.end ? extent.end : extent.start)
            },
            isInstant: extent.end === null
        };
    }

    _apply = (event) => {
        const {onChange} = this.props;

        event.preventDefault();
        event.stopPropagation();

        onChange({
            field: 'time',
            value: this._getTimeString()
        });
    }

    _getTimeString = () => {
        const {isInstant, instant, period} = this.state;

        return isInstant ? this._formatDate(instant) : `${this._formatDate(period.start)}/${this._formatDate(period.end)}`
    }

    _formatDate = (date) => {
        return moment.utc(date).format();
    }

    render() {
        const {fields} = this.props;
        const {isInstant, instant, period} = this.state;

        return (
            <Form onSubmit={ this._apply }>
                <p className="text-muted text-uppercase">
                    time
                </p>
                <ButtonGroup className="mb-3">
                    <Button color="primary"
                        outline={ isInstant }
                        size="sm"
                        className="py-0"
                        onClick={ () => this.setState({
                                      isInstant: false
                                  }) }>
                        Period
                    </Button>
                    <Button color="primary"
                        outline={ !isInstant }
                        size="sm"
                        className="py-0"
                        onClick={ () => this.setState({
                                      isInstant: true
                                  }) }>
                        Instant
                    </Button>
                </ButtonGroup>
                <Row>
                    { isInstant
                      ? <Col md="10">
                        <Datetime className=""
                            inputProps={ { className: 'form-control form-control-sm w-100 mb-3', readOnly: true, style: { backgroundColor: 'white', cursor: 'pointer' } } }
                            timeFormat='HH:mm:ss'
                            dateFormat='DD.MM.YYYY'
                            utc={ true }
                            value={ instant }
                            onChange={ instant => this.setState({
                                           instant: instant
                                       }) } />
                        <Input size="sm" className="mb-3" disabled={ true } />
                        </Col>
                      : <DatetimeRangePicker className="col-md-10"
                            inputProps={ { className: 'form-control form-control-sm w-100 mb-3', readOnly: true } }
                            timeFormat='HH:mm:ss'
                            dateFormat='DD.MM.YYYY'
                            utc={ true }
                            startDate={ period.start }
                            endDate={ period.end }
                            onChange={ period => this.setState({
                                           period: period
                                       }) } /> }
                    <Col md="2" className="d-flex align-items-end mb-3">
                    <Button color="primary" size="sm" onClick={ this._apply }>
                        Add
                    </Button>
                    </Col>
                </Row>
            </Form>
        );
    }
}

TimeFilter.propTypes = {
    start: PropTypes.number.isRequired,
    end: PropTypes.number.isRequired,
    filter: PropTypes.string,
    onChange: PropTypes.func.isRequired
};

TimeFilter.defaultProps = {
    start: new Date().getTime(),
    end: new Date().getTime()
};