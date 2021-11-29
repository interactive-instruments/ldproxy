import React, { useState } from 'react';
import PropTypes from 'prop-types';

import { Button, Row, Col } from 'reactstrap/dist/reactstrap.es';
import qs from 'qs';

import Badge from './Badge';
import Editor from './Editor';

const toBounds = (filter) => {
    const a = filter.split(',');
    const b = [
        [parseFloat(a[0]), parseFloat(a[1])],
        [parseFloat(a[2]), parseFloat(a[3])],
    ];
    return b;
};

const FilterEditor = ({
    fields,
    backgroundUrl,
    attribution,
    spatial,
    temporal,
}) => {
    const [isOpen, setOpen] = useState(false);

    const enabled = Object.keys(fields).length > 0 || spatial || temporal;

    // eslint-disable-next-line no-undef
    const query = qs.parse(window.location.search, {
        ignoreQueryPrefix: true,
    });

    const [filters, setFilters] = useState(
        Object.keys(fields)
            .concat(['bbox', 'datetime'])
            .reduce((reduced, field) => {
                if (query[field]) {
                    // eslint-disable-next-line no-param-reassign
                    reduced[field] = {
                        value: query[field],
                        add: false,
                        remove: false,
                    };
                }
                return reduced;
            }, {})
    );

    const toggle = (event) => {
        event.target.blur();

        setOpen(!isOpen);
    };

    const onAdd = (field, value) => {
        setFilters((prev) => ({
            ...prev,
            [field]: { value, add: true, remove: false },
        }));
    };

    const onRemove = (field) => {
        setFilters((prev) => ({
            ...prev,
            [field]: { value: prev[field].value, add: false, remove: true },
        }));
    };

    const save = (event) => {
        event.target.blur();

        const newFilters = Object.keys(filters).reduce((reduced, key) => {
            if (filters[key].add || !filters[key].remove) {
                // eslint-disable-next-line no-param-reassign
                reduced[key] = {
                    ...filters[key],
                    add: false,
                    remove: false,
                };
            }
            return reduced;
        }, {});

        delete query.offset;

        Object.keys(fields)
            .concat(['bbox', 'datetime'])
            .forEach((field) => {
                delete query[field];
                if (newFilters[field]) {
                    query[field] = newFilters[field].value;
                }
            });

        // eslint-disable-next-line no-undef
        window.location.search = qs.stringify(query, {
            addQueryPrefix: true,
        });
    };

    const cancel = (event) => {
        event.target.blur();

        const newFilters = Object.keys(filters).reduce((reduced, key) => {
            if (!filters[key].add) {
                // eslint-disable-next-line no-param-reassign
                reduced[key] = {
                    ...filters[key],
                    add: false,
                    remove: false,
                };
            }
            return reduced;
        }, {});

        setFilters(newFilters);
        setOpen(false);
    };

    return (
        <>
            <Row className='mb-3'>
                <Col
                    md='3'
                    className='d-flex flex-row justify-content-start align-items-center flex-wrap'>
                    <span className='mr-2 font-weight-bold'>Filter</span>
                    {enabled && (
                        <Button
                            color={isOpen ? 'primary' : 'secondary'}
                            outline={!isOpen}
                            size='sm'
                            className='py-0'
                            onClick={isOpen ? save : toggle}>
                            {isOpen ? 'Apply' : 'Edit'}
                        </Button>
                    )}
                    {isOpen && (
                        <Button
                            color='danger'
                            size='sm'
                            className='ml-1 py-0'
                            onClick={cancel}>
                            Cancel
                        </Button>
                    )}
                </Col>
                <Col
                    md='9'
                    className='d-flex flex-row justify-content-start align-items-center flex-wrap'>
                    {Object.keys(filters).map((key) => (
                        <Badge
                            key={key}
                            field={key}
                            value={filters[key].value}
                            isAdd={filters[key].add}
                            isRemove={filters[key].remove}
                            isEditable={isOpen}
                            onRemove={onRemove}
                        />
                    ))}
                </Col>
            </Row>
            <Editor
                isOpen={isOpen}
                fields={fields}
                backgroundUrl={backgroundUrl}
                attribution={attribution}
                spatial={filters.bbox ? toBounds(filters.bbox.value) : spatial}
                temporal={temporal}
                filters={filters}
                onAdd={onAdd}
            />
        </>
    );
};

FilterEditor.displayName = 'FilterEditor';

FilterEditor.propTypes = {
    fields: PropTypes.objectOf(PropTypes.string),
    backgroundUrl: PropTypes.string,
    attribution: PropTypes.string,
    spatial: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)),
    temporal: PropTypes.objectOf(PropTypes.number),
};

FilterEditor.defaultProps = {
    fields: {},
    backgroundUrl: 'https://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution:
        '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors',
    spatial: null,
    temporal: null,
};

export default FilterEditor;
