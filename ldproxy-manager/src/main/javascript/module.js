/*
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
import Codelists from './components/container/Codelists'
import CodelistIndex from './components/presentational/CodelistIndex'
import CodelistShow from './components/presentational/CodelistShow'
import CodelistAdd from './components/presentational/CodelistAdd'
import Console from './components/container/Console'
import MappingEditGeneral from './components/presentational/MappingEditGeneral'
import MappingEditGeoJson from './components/presentational/MappingEditGeoJson'
import MappingEditHtml from './components/presentational/MappingEditHtml'
import { customTheme } from './theme'
import ServiceEditExtensions from './components/presentational/ServiceEditExtensions'
import ServiceEditTiles from './components/presentational/ServiceEditTiles'
import ServiceActionsOgcApi from './components/presentational/ServiceActionsOgcApi'
import FeatureTypeEditTiles from './components/presentational/FeatureTypeEditTiles'

export default {
    applicationName: 'ldproxy',
    typeLabels: {
        WFS3: 'WFS3'
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
                roles: ['SUPERADMIN', 'ADMIN', 'EDITOR'],
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
            },
            {},
            {},
            /*{
                path: '/console',
                component: Console,
                title: 'Console',
                menu: true,
                roles: ['SUPERADMIN', 'ADMIN']
            }*/
        ]
    },
    typedComponents: {
        MappingEdit: {
            general: MappingEditGeneral,
            'application/geo+json': MappingEditGeoJson,
            'application/ld+json': MappingEditHtml,
            'text/html': MappingEditHtml
        },
        ServiceActionsView: {
            default: ServiceActionsOgcApi
        }
    },
    extendableComponents: {
        ServiceEdit: {
            Api: ServiceEditExtensions,
            Tiles: ServiceEditTiles
        },
        FeatureTypeEdit: {
            //Tiles: FeatureTypeEditTiles
        }
    },
    theme: customTheme
};

