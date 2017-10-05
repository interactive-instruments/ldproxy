/*
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
define([
        "dojo/_base/declare",
        "xsf/api/Module",
        //"xsf/WFS2GSFS/WFS2GSFSEditParametersWidget",
        //"xsf/WFS2GSFS/ServiceEditLayersWidget",
        //"xsf/WFS2GSFS/SettingsServicesWidget",
        //"xsf/WFS2GSFS/LayerTemplatesWidget",
        //"xsf/WFS2GSFS/ServiceEditGeneralWidget",
        "dojo/i18n"
    ],
    function(declare, Module,
        /*WFS2GSFSEditParametersWidget, ServiceEditLayersWidget,
               SettingsServicesWidget, LayerTemplatesWidget, ServiceEditGeneralWidget,*/
        i18n) {
        return declare([Module], {
            getMainMenuItems: function() {
                return [];
            },
            getSettingsItems: function() {
                return [];
            },
            getServiceSettingsItems: function() {
                //var userLang = navigator.userLanguage || navigator.language || navigator.language;
                //this.messages = i18n.getLocalization("xsf.WFS2GSFS", "Module", userLang);
                return [
                    /*{
                        id: "general",
                        label: this.messages.general,
                        selected: true,
                        widget: ServiceEditGeneralWidget
                    },
                    {
                        id: "parameters",
                        label: this.messages.parameters,
                        widget: WFS2GSFSEditParametersWidget
                    },
                    {
                        id: "layers",
                        label: this.messages.layers,
                        allwaysRefresh: true,
                        widget: ServiceEditLayersWidget
                    }*/
                ];
            },
            getServiceViews: function() {
                return [];
            },
            getServiceActions: function() {
                return [];
            }
        });
    });
