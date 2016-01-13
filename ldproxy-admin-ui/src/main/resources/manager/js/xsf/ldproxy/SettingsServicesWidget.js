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
    "dojo/_base/lang",
    "dijit/_WidgetBase",
    "dojo/dom",
    "dojo/dom-construct",
    "dojo/dom-class",
    "dojo/query",
    "dojo/_base/array",
    "dojo/when",
    "dijit/form/TextBox",
    "dijit/Dialog",
    "dijit/form/Select",
    "dijit/form/DropDownButton",
    "dijit/Menu",
    "dijit/MenuItem",
    "dojo/store/JsonRest",
    "dojo/NodeList-dom",
    "dojo/i18n",
    "dojo/string"
],
        function(declare, lang, WidgetBase, dom, domConstruct, domClass, query,
                array, when, TextBox, Dialog, Select, DropDownButton,
                Menu, MenuItem, JsonRest, NodeListDom, i18n, string) {
            return declare([WidgetBase], {
                baseClass: "settingsModulesWidget",
                adminStore: null,
                pageWidget: null,
                buildRendering: function() {
                    this.domNode = domConstruct.create("div", {
                        "class": "widget-wrapper",
                        style: {
                            textAlign: "left"
                        }
                    });
                    
                    this.messages = i18n.getLocalization("xsf.WFS2GSFS", "Module", navigator.userLanguage || navigator.language);

                    domConstruct.create("label", {for : "maxfeatures", innerHTML: this.messages.defaultmaxfeatures+"&nbsp;&nbsp;"}, this.domNode);
                    this.level = new TextBox({
                        name: "maxfeatures",
                        value: "1000",
                        "class": "inverse",
                        style: "margin-right: 10px;",
                        disabled: true
                    }).placeAt(this.domNode);
                }
            });
        });