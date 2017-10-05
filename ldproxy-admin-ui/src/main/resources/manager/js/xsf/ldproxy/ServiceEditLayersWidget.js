/*
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
define([
    "dojo/_base/declare",
    "dijit/_WidgetBase",
    "dojo/_base/lang",
    "dojo/when",
    "dojo/dom-construct",
    "dojo/_base/array",
    "dijit/TitlePane",
    'dijit/form/Form',
    "xsf/WFS2GSFS/ServiceEditLayerWidget",
    "dojox/layout/ScrollPane",
    "dojo/dom-style",
    "dojo/window",
    "dojo/on",
    "dojo/has",
    "dijit/form/CheckBox",
    "dojo/Deferred",
    "dojo/aspect",
    "dojo/i18n"
],
        function(declare, WidgetBase, lang, when, domConstruct, array, TitlePane,
                Form, ServiceEditLayerWidget, ScrollPane, domStyle, window,
                on, has, CheckBox, Deferred, aspect, i18n) {
            return declare([WidgetBase], {
                layerWidgets: null,
                service: null,
                selectedLayers: [],
                labelDiv: null,
                config: null,
                constructor: function() {
                    this.messages = i18n.getLocalization("xsf.WFS2GSFS", "Module", navigator.userLanguage || navigator.language);
                },
                buildRendering: function() {
                                      
                    if (has("chrome") >= 29) {
                        var w = window.getBox();
                        var wh = w.h - 250;
                        this.domNode = domConstruct.create("div", {style: "width:100%; margin:0px; overflow:auto; max-height:" + wh + ";"});
                    } else {
                        this.domNode = domConstruct.create("div", {style: "width:100%; margin:0px; max-height: 400px; overflow:auto;"});
                    }

                    this.containerNode = this.domNode;
                    this.layerWidgets = new Array(this.config.fullLayers.length);
                    this.selectedLayers = new Array(this.config.fullLayers.length);

                    var ul = domConstruct.create("ul", {style: "width:100%; margin:0px"}, this.domNode);

                    var li0 = domConstruct.create("li", null, ul);
                    var chkall = new CheckBox({
                        name: "checkAllBox",
                        value: false,
                        checked: false,
                        style: "float:left; margin-top:7px; margin-right: 20px; margin-bottom: 10px"
                    }, "checkBox");
                    chkall.placeAt(li0);
                    this.labelDiv = domConstruct.create("div", {innerHTML: this.messages.activatealllayers, style: "font-weight: bold; width:100%; margin-top: 7px;"}, li0);
                    
                    array.forEach(this.config.fullLayers, function(layer, i) {
                        var li = domConstruct.create("li", null, ul);

                        var chk = new CheckBox({
                            name: "checkBox",
                            value: layer.enabled,
                            checked: layer.enabled,
                            style: "float:left; margin-top:7px;"
                        }, "checkBox");
                        chk.placeAt(li);
                        this.selectedLayers[i] = chk;

                        var tp = new TitlePane({title: layer.name, open: false});
                        tp.placeAt(li);
                        tp.startup();
                        tp.hascontent = false;

                        on(chk, "change", lang.hitch(this, this.checkBoxChanged, layer, chk, i));
                        on(chkall, "change", lang.hitch(this, this.checkBoxAllChanged));
                        aspect.after(tp, "toggle", lang.hitch(this, this.initServiceEditLayerWidget, tp, layer, chk, i));

                    }, this);

                },
                checkBoxChanged: function(layer, chk, i) {

                },
                checkBoxAllChanged: function(val) {
                    if( val){
                        this.labelDiv.innerHTML = this.messages.deactivatealllayers;
                    } else {
                        this.labelDiv.innerHTML = this.messages.activatealllayers;
                    }
                    
                    array.forEach(this.config.fullLayers, function(layer, i) {
                        this.selectedLayers[i].set("checked", val);
                    }, this);
                },
                refresh: function() {
                },
                initServiceEditLayerWidget: function(tp, layer, chk, index) {

                    if (!tp.hascontent) {
                        tp.hascontent = true;

                        var sel = new ServiceEditLayerWidget({
                            service: this.service,
                            config: layer,
                            adminStore: this.adminStore,
                            pageWidget: this.pageWidget});

                        this.layerWidgets[index] = sel;

                        tp.set("content", sel.domNode);
                    }

                    //this.layerWidgets[index].enabledChanged(chk.get('checked'));
                },
                gatherFormValues: function() {
                    var layers = new Array(this.config.fullLayers.length);
                    var layersEnabled = new Array(this.config.fullLayers.length);

                    for (var i = 0; i < this.config.fullLayers.length; i++) {
                        if (this.layerWidgets[i]) {
                            layers[i] = this.layerWidgets[i].gatherFormValues();
                        }
                        else {
                            layers[i] = null;
                        }
                        layersEnabled[i] = this.selectedLayers[i].get('checked');
                    }

                    return {fullLayers: layers, layersEnabled: layersEnabled};
                }
            });
        });