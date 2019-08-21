/*
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
//import { normalizeServices, normalizeServiceConfigs } from './ServiceNormalizer'
import { normalize, schema } from 'normalizr';

const userSchema = new schema.Entity('users', {}, {
    idAttribute: 'id'
})

export default {

    getUsersQuery: function () {
        return {
            url: `/rest/admin/users/`,
            transform: (userIds) => ({
                userIds: userIds
            }),
            update: {
                userIds: (prev, next) => next
            },
            force: true
        }
    },

    getUserQuery: function (id) {
        return {
            url: `/rest/admin/users/${encodeURIComponent(id)}/`,
            transform: (user) => normalize(user, userSchema).entities,
            update: {
                users: (prev, next) => {
                    return {
                        ...prev,
                        ...next
                    }
                }
            },
            //force: true
        }
    },

    addUserQuery: function (user) {
        return {
            url: `/rest/admin/users/`,
            body: JSON.stringify(user),
            options: {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            },
            transform: (user) => normalize(user, userSchema).entities,
            update: {
                users: (prev, next) => {
                    return {
                        ...prev,
                        ...next
                    }
                }
            },
        }
    },

    deleteUserQuery: function (id) {
        return {
            url: `/rest/admin/users/${encodeURIComponent(id)}`,
            options: {
                method: 'DELETE'
            },
            update: {
                userIds: (prev, next) => {
                    return prev.filter(el => el !== id)
                },
                users: (prev, next) => {
                    const { [id]: deletedItem, ...rest } = prev;
                    console.log('DEL', rest)
                    return rest
                }
            }
        }
    }
}
