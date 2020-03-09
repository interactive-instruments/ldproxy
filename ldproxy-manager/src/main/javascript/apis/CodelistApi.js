/*
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
//import { normalizeServices, normalizeServiceConfigs } from './ServiceNormalizer'
import { normalize, schema } from 'normalizr';
import { secureQuery } from 'xtraplatform-manager/src/apis/AuthApi'
import { DEFAULT_OPTIONS } from 'xtraplatform-manager/src/apis/ServiceApi'

const clSchema = new schema.Entity('codelists', {}, {
    idAttribute: 'id'
})

export default {

    getCodelistsQuery: function (options = DEFAULT_OPTIONS) {
        const query = {
            url: `/rest/admin/codelists/`,
            transform: (codelistIds) => ({
                codelistIds: codelistIds
            }),
            update: {
                codelistIds: (prev, next) => next
            },
            force: options.forceReload
        }

        return options.secured ? secureQuery(query) : query
    },

    getCodelistQuery: function (id, options = DEFAULT_OPTIONS) {
        const query = {
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

        return options.secured ? secureQuery(query) : query
    },

    addCodelistQuery: function (codelist, options = DEFAULT_OPTIONS) {
        const query = {
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

        return options.secured ? secureQuery(query) : query
    },

    deleteCodelistQuery: function (id, options = DEFAULT_OPTIONS) {
        const query = {
            url: `/rest/admin/codelists/${encodeURIComponent(id)}/`,
            options: {
                method: 'DELETE'
            },
            update: {
                codelistIds: (prev, next) => {
                    return prev.filter(el => el !== id)
                },
                codelists: (prev, next) => {
                    const { [id]: deletedItem, ...rest } = prev;
                    console.log('DEL', rest)
                    return rest
                }
            }
        }

        return options.secured ? secureQuery(query) : query
    }
}
