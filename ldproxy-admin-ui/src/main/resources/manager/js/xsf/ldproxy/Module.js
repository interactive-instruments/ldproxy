/*
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
