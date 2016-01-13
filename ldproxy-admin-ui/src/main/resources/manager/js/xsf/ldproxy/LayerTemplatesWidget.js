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
    "dijit/TitlePane",
    "xsf/WFS2GSFS/LayerTemplateWidget",
    "dojo/i18n"
],
        function(declare, lang, WidgetBase, dom, domConstruct, domClass, query, array, when, TextBox, Dialog, Select, 
        DropDownButton, Menu, MenuItem, JsonRest, TitlePane, LayerTemplateWidget, i18n) {
            return declare([WidgetBase], {
                baseClass: "expandableListForm",
                adminStore: null,
                pageWidget: null,
                ltStore: null,
                ul: null,
                buildRendering: function() {
                    this.messages = i18n.getLocalization("xsf.WFS2GSFS", "Module", navigator.userLanguage || navigator.language);
                    
                    this.domNode = domConstruct.create("div", {
                        "class": "widget-wrapper " + this.baseClass,
                        style: {
                            textAlign: "left"
                        }
                    });
                    
                    this.ul = domConstruct.create("ul", {style: "width:100%; margin:0px"}, this.domNode);

                    this.ltStore = this.adminStore.getSubStore("modules/WFS2GSFS/layertemplates");

                    this.refresh();
                },
                _render: function(item) {

                    domConstruct.empty(this.ul);
                    
                    console.log(item);

                    var empty = true;
                    for (var key in item) {      
                        var li = domConstruct.create("li", null, this.ul);
                        var div = domConstruct.create("div", null, li);
                        var ul1 = domConstruct.create("ul", {style: "width:100%; margin:0px"});
                        
                        console.log(key);
                        
                        var x = item[key];
                        console.log( x );
                        
                        for (var key2 in x) {
                        
                        console.log("key2 " + key2);
                        console.log("val22 " + x[key2]);
                        
                        //array.forEach(item[key], function(layer, i) {                           
                           var ltw = new LayerTemplateWidget({
                                templateid: key2,
                                layername: x[key2], 
                                namespace: key, 
                                ltwcontainer: this, 
                                adminStore: this.adminStore.getSubStore("modules/WFS2GSFS/layertemplates")
                            });
                            ltw.placeAt(ul1);  
                            
                        }

                        var tp = new TitlePane({title: key, content: ul1, open: false}); 
                        tp.placeAt(div);
                        
                        empty = false;
                    }
                    
                    if( empty) {                       
                        var li = domConstruct.create("li", null, this.ul);
                        var div = domConstruct.create("div", null, li);
                        div.innerHTML = this.messages.missinglayertemplates;
                    }

                },
                refresh: function() {
                    when(this.ltStore.get(""), lang.hitch(this, this._render));
                }
            });
        });