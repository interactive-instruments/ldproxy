/*
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
import Codelists from './components/container/Codelists'
import CodelistIndex from './components/presentational/CodelistIndex'
import CodelistShow from './components/presentational/CodelistShow'
import CodelistAdd from './components/presentational/CodelistAdd'
import MappingEditGeneral from './components/presentational/MappingEditGeneral'
import MappingEditGeoJson from './components/presentational/MappingEditGeoJson'
import MappingEditHtml from './components/presentational/MappingEditHtml'

export default {
    applicationName: 'ldproxy',
    typeLabels: {
        ldproxy: 'ldproxy'
    },
    routes: {
        path: '/',
        routes: [
            {},
            {
                path: '/codelists',
                component: Codelists,
                title: 'Codelists',
                menu: true,
                routes: [
                    {
                        path: '/add',
                        component: CodelistAdd
                    },
                    {
                        //TODO: workaround because named patterns do not match dots in url-pattern
                        //path: '/:id',
                        path: '/*',
                        component: CodelistShow
                    },
                    {
                        path: '/',
                        component: CodelistIndex
                    }
                ]
            }
        ]
    },
    typedComponents: {
        MappingEdit: {
            general: MappingEditGeneral,
            'application/geo+json': MappingEditGeoJson,
            'application/ld+json': MappingEditHtml,
            'text/html': MappingEditHtml
        }
    }
};

