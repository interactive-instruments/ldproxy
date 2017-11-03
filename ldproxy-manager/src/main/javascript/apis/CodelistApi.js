/*
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
//import { normalizeServices, normalizeServiceConfigs } from './ServiceNormalizer'
import { normalize, schema } from 'normalizr';

const clSchema = new schema.Entity('codelists', {}, {
    idAttribute: 'resourceId'
})

export default {

    getCodelistsQuery: function() {
        return {
            url: `/rest/admin/codelists/`,
            transform: (codelistIds) => ({
                codelistIds: codelistIds
            }),
            update: {
                codelistIds: (prev, next) => next
            },
            force: true
        }
    },

    getCodelistQuery: function(id) {
        return {
            url: `/rest/admin/codelists/${encodeURIComponent(id)}/`,
            transform: (codelist) => normalize(codelist, clSchema).entities,
            update: {
                codelists: (prev, next) => {
                    return {
                        ...prev,
                        ...next
                    }
                }
            },
        //force: true
        }
    },

    addCodelistQuery: function(codelist) {
        return {
            url: `/rest/admin/codelists/`,
            body: JSON.stringify(codelist),
            options: {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            },
            transform: (codelist) => normalize(codelist, clSchema).entities,
            update: {
                codelists: (prev, next) => {
                    return {
                        ...prev,
                        ...next
                    }
                }
            },
        }
    },

    deleteCodelistQuery: function(id) {
        return {
            url: `/rest/admin/codelists/${encodeURIComponent(id)}/`,
            options: {
                method: 'DELETE'
            },
            update: {
                codelistIds: (prev, next) => {
                    return prev.filter(el => el !== id)
                },
                codelists: (prev, next) => {
                    const {[id]: deletedItem, ...rest} = prev;
                    console.log('DEL', rest)
                    return rest
                }
            }
        }
    }
}
