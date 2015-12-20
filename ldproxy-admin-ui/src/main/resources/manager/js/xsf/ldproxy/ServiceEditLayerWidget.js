define([
    "dojo/_base/declare",
    "dijit/_WidgetBase",
    "dijit/_TemplatedMixin",
    "dijit/_WidgetsInTemplateMixin",
    "dojo/text!./ServiceEditLayerWidget/template.html",
    "dojo/_base/lang",
    'dijit/form/Form',
    'dojox/form/manager/_Mixin',
    'dojox/form/manager/_FormMixin',
    'dojox/form/manager/_DisplayMixin',
    'dojox/form/manager/_ValueMixin',
    'dijit/form/ValidationTextBox',
    'dijit/form/CheckBox',
    'dojox/validate',
    "xsf/MappingEditorWidget",
    "dojo/_base/Color",
    "dojo/dom-style",
    "dojo/on",
    "dojo/_base/lang",
    "dojo/dom-class",
    "dijit/ColorPalette",
    "xsf/ConfirmationDialogWidget",
    "dijit/popup",
    "dojo/dom-construct",
    "dojo/dom",
    "dijit/form/HorizontalSlider",
    "dojox/gfx",
    "dojox/gfx/matrix",
    "dojo/store/Memory",
    "dijit/form/Select",
    "dijit/form/Textarea",
    "dojo/_base/json",
    "dijit/focus",
    "dojo/when",
    "dojo/store/JsonRest",
    "xsf/FlashMessageMixin",
    "dijit/Dialog",
    "dojo/query",
    "dojo/i18n",
    "dojo/string"
],
        function(declare, WidgetBase, TemplatedMixin, WidgetsInTemplateMixin,
                template, lang, Form, FormMgrMixin, FormMgrFormMixin,
                FormMgrDisplayMixin, FormMgrValueMixin,
                ValidationTextBox, CheckBox, Validate, MappingEditorWidget,
                Color, domStyle, on, lang, domClass, ColorPalette,
                ConfirmationDialogWidget,
                popup, domConstruct, dom, HorizontalSlider, gfx, matrix, Memory,
                Select, Textarea, json, focusUtil, when, JsonRest,
                FlashMessageMixin, Dialog, query, i18n, string) {
            return declare([WidgetBase, Form, TemplatedMixin,
                WidgetsInTemplateMixin, FormMgrMixin, FormMgrFormMixin,
                FormMgrDisplayMixin, FormMgrValueMixin, FlashMessageMixin], {
                templateString: template,
                config: null,
                service: null,
                myColor: null,
                myOutlineColor: null,
                myAlpha: null,
                myOutlineAlpha: null,
                mySize: null,
                myWidth: null,
                myOutlineWidth: null,
                colorPalette: null,
                colorOpen: false,
                outlinecolorPalette: null,
                outlinecolorOpen: false,
                previewSymbol: null,
                surface: null,
                sizeSlider: null,
                widthSlider: null,
                outlineWidthSlider: null,
                colorSlider: null,
                outlineColorSlider: null,
                symbolSelect: null,
                outlineSelect: null,
                geometrySelect: null,
                customRenderer: null,
                mappingEditorChanges: null,
                useAsId: null,
                enabled: false,
                dialog: null,
                isDialog: false,
                style: "width:510px; height: 80%",
                map: null,
                postMixInProperties: function()
                {
                    this.messages = i18n.getLocalization("xsf.WFS2GSFS", "Module", navigator.userLanguage || navigator.language || navigator.language);

                    this.dialog = new Dialog({
                        title: dojo.string.substitute(this.messages.editTemplateFor, {name: this.config.name}),
                        parseOnLoad: false,
                        draggable: false
                    });

                    query(".closeText", this.dialog.domNode).addClass("icon-remove-circle").empty();
                    //query(".dijitDialogTitleBar", this.addServiceDialog.domNode).addClass("dijitButtonNode");
                    //domClass.add(this.addServiceDialog.domNode, "inverse");

                    this.initValues();
                    //this.inherited(arguments);
                },
                postCreate: function() {
                    if (this.config.enabled) {
                        this.enabled = true;
                    }

                    this.hideNode(this.dialog_buttons);

                    this.dialog.set("content", this.domNode);

                    this.flashMessageNode = this.messageNode;

                    this.mappingButton.onClick = lang.hitch(this, this._loadMappingEditor);
                    this.templateButton.onClick = lang.hitch(this, this._showSaveAsTemplateConfirm);

                    this.cancelButton.onClick = lang.hitch(this, this.hide);
                    this.saveButton.onClick = lang.hitch(this, this.save);

                    customRenderer = false;
                    this.initDrawingInfo();

                    //this.inherited(arguments);
                },
                show: function()
                {
                    domStyle.set(this.contentArea, "height", "300px");

                    this.map = {};
                    this.map.mapping = this.mappings;
                    this.map.namespaces = this.namespaces;

                    this.isDialog = true;
                    this.showNode(this.dialog_buttons);
                    this.hideNode(this.templateButtonSpan);
                    lang.hitch(this.dialog, this.dialog.show)();

                    if (typeof this.config.extent === "undefined") {
                        this.config.extent = {};
                        this.config.extent.xmin = 0;
                        this.config.extent.ymin = 0;
                        this.config.extent.xmax = 0;
                        this.config.extent.ymax = 0;
                        this.config.extent.spatialReference = {};
                        this.config.extent.spatialReference.wkid = 4711;
                    }

                    this.hideNode(this.extent_li);
                    this.hideNode(this.extent_xmin_li);
                    this.hideNode(this.extent_ymin_li);
                    this.hideNode(this.extent_xmax_li);
                    this.hideNode(this.extent_ymax_li);
                    this.hideNode(this.extent_srs_li);

                },
                hide: function()
                {
                    lang.hitch(this.dialog, this.dialog.hide)();
                },
                save: function() {

                    var layer = this.gatherFormValues();

                    console.log(layer);

                    var unflatten = function unflatten(target) {
                        var delimiter = '.';
                        var result = {};
                        if (Object.prototype.toString.call(target) !== '[object Object]' && Object.prototype.toString.call(target) !== '[object Array]') {
                            return target;
                        }
                        Object.keys(target).forEach(function(key) {
                            var split = key.split(delimiter)
                                    , firstNibble
                                    , secondNibble
                                    , recipient = result;

                            function getkey(key) {
                                var parsedKey = parseInt(key);
                                return (isNaN(parsedKey) ? key : parsedKey);
                            }
                            ;

                            firstNibble = getkey(split.shift());
                            secondNibble = getkey(split[0]);

                            while (secondNibble !== undefined) {
                                if (recipient[firstNibble] === undefined) {
                                    recipient[firstNibble] = ((typeof secondNibble === 'number') ? [] : {});
                                }

                                recipient = recipient[firstNibble];
                                if (split.length > 0) {
                                    firstNibble = getkey(split.shift());
                                    secondNibble = getkey(split[0]);
                                }
                            }

                            // handle the colorstring
                            if (firstNibble === "color") {
                                if (target[key] !== null && target[key].length > 0) {
                                    var arr = [];
                                    var spl = target[key].split(',');
                                    for (var i = 0; i < spl.length; i++) {
                                        arr[i] = parseInt(spl[i], 10);
                                    }
                                    target[key] = arr;
                                }
                            }

                            if (Object.prototype.toString.call(target[key]) === '[object Array]') {
                                recipient[firstNibble] = [];
                                for (var i in target[key]) {
                                    recipient[firstNibble][i] = unflatten(target[key][i]);
                                }
                            } else {
                                // unflatten again for 'messy objects'
                                recipient[firstNibble] = unflatten(target[key]);
                            }
                        });
                        return result;
                    }; // function unflatten(target) 

                    layer = unflatten(layer);
                    console.log("layerunflat:");

                    layer.gmlFeatureTypeName = this.config.gmlFeatureTypeName;
                    layer.gmlFeatureTypeNamespace = this.config.gmlFeatureTypeNamespace;

                    console.log(layer);

                    //TODO: use resource endpoint instead of 'update', needs new ResourceIdGenerator module
                    when(this.adminStore.put(
                            layer, {incremental: true}
                    ), lang.hitch(this, this._onSuccess), lang.hitch(this, this._onFailure));

                    this.hide();
                },
                _showSaveAsTemplateConfirm: function() {
                    var confirm = new ConfirmationDialogWidget({
                        title: string.substitute(this.messages.saveLayerTemplateConfirmationTitle, {name: this.config.name, id: this.config.id}),
                        content: string.substitute(this.messages.saveLayerTemplateConfirmationContent, {name: this.config.name}),
                        action: lang.hitch(this, function() {
                            this._saveAsTemplate();
                        })
                    });
                    confirm.onHide = lang.hitch(confirm, function() {
                        this.destroyRecursive();
                    });
                    confirm.show();
                },
                _saveAsTemplate: function() {
                    var adminStore = this.adminStore.getSubStore(this.service.id + "/");

                    when(adminStore.put({
                        _operation_: "saveLayerTemplate",
                        _parameter_: "" + this.config.id + ""
                    }, {
                    }), lang.hitch(this, this._onSuccess), lang.hitch(this, this._onFailure));

                },
                _loadMappingEditor: function() {

                    var editor = new MappingEditorWidget({
                        title: string.substitute(this.messages.editFieldMappingTitle, {name: this.config.name}),
                        config: this.config,
                        service: this.service,
                        adminStore: this.adminStore,
                        pageWidget: this.pageWidget,
                        parent: this,
                        mappings: this.map
                    });
                    editor.onHide = lang.hitch(editor, function() {
                        this.destroyRecursive();
                    });
                    editor.show();
                },
                _onSuccess: function(params, response) {
                    lang.hitch(this, this.success, string.substitute(this.messages.saveLayerTemplateSuccess, {name: this.config.name}), 5000)();
                },
                _onFailure: function(params, response) {
                    lang.hitch(this, this.error, string.substitute(this.messages.saveLayerTemplateFail, {name: this.config.name}), 0)();
                },
                initValues: function() {

                    if (typeof this.config.drawingInfo === "undefined") {
                        this.config.drawingInfo = {};
                    }
                    if (typeof this.config.drawingInfo.renderer === "undefined") {
                        this.config.drawingInfo.renderer = {};
                    }
                    if (typeof this.config.drawingInfo.renderer.symbol === "undefined") {
                        this.config.drawingInfo.renderer.symbol = {};
                    }
                    if (typeof this.config.drawingInfo.renderer.symbol.width === "undefined") {
                        this.config.drawingInfo.renderer.symbol.width = 1;
                    }
                    if (typeof this.config.drawingInfo.renderer.symbol.size === "undefined") {
                        this.config.drawingInfo.renderer.symbol.size = 5;
                    }
                    if (typeof this.config.drawingInfo.renderer.symbol.color === "undefined") {
                        this.config.drawingInfo.renderer.symbol.color = [255, 0, 0, 100];
                    }
                    if (typeof this.config.drawingInfo.renderer.symbol.outline === "undefined") {
                        this.config.drawingInfo.renderer.symbol.outline = {};
                        this.config.drawingInfo.renderer.symbol.outline.color = [0, 0, 0, 255];
                        this.config.drawingInfo.renderer.symbol.outline.width = 2;
                    }
                    if (typeof this.config.extent === "undefined") {
                        this.config.extent = {};
                        this.config.extent.xmin = 0;
                        this.config.extent.ymin = 0;
                        this.config.extent.xmax = 0;
                        this.config.extent.ymax = 0;
                        this.config.extent.spatialReference = {};
                        this.config.extent.spatialReference.wkid = 4711;
                    }

                },
                getLinestyle: function(esri) {
                    if (esri === "esriSLSDash") {
                        return "ShortDash";
                    } else if (esri === "esriSLSDashDotDot") {
                        return "ShortDashDotDot";
                    } else if (esri === "esriSLSDot") {
                        return "ShortDot";
                    } else if (esri === "esriSLSSolid") {
                        return "Solid";
                    } else {
                        return "Solid";
                    }
                },
                updateSymbol: function() {

                    if (this.surface === null) {
                        this.surface = gfx.createSurface(this.previewSymbolSpan, 200, 40);
                    }
                    this.surface.clear();

                    if (this.config.geometryType === "esriGeometryPoint") {
                        var sz = this.drawingInfo_renderer_symbol_size.get('value');
                        if (this.symbolSelect.get('value') === "esriSMSCircle") {
                            this.previewSymbol = this.surface.createCircle({cx: 100, cy: 20, r: sz / 2});
                        } else if (this.symbolSelect.get('value') === "esriSMSCross") {
                            this.surface.createLine({x1: 100 - sz / 2, y1: 20, x2: 100 + sz / 2, y2: 20})
                                    .setStroke({color: this.myOutlineColor, width: this.drawingInfo_renderer_symbol_outline_width.get('value'), cap: "round"});
                            this.surface.createLine({x1: 100, y1: 20 - sz / 2, x2: 100, y2: 20 + sz / 2})
                                    .setStroke({color: this.myOutlineColor, width: this.drawingInfo_renderer_symbol_outline_width.get('value'), cap: "round"});
                            this.hideNode(this.drawingInfo_renderer_symbol_color_li);
                        } else if (this.symbolSelect.get('value') === "esriSMSX") {
                            this.surface.createLine({x1: 100 - sz / 2, y1: 20 + sz / 2, x2: 100 + sz / 2, y2: 20 - sz / 2})
                                    .setStroke({color: this.myOutlineColor, width: this.drawingInfo_renderer_symbol_outline_width.get('value'), cap: "round"});
                            this.surface.createLine({x1: 100 - sz / 2, y1: 20 - sz / 2, x2: 100 + sz / 2, y2: 20 + sz / 2})
                                    .setStroke({color: this.myOutlineColor, width: this.drawingInfo_renderer_symbol_outline_width.get('value'), cap: "round"});
                            this.hideNode(this.drawingInfo_renderer_symbol_color_li);
                        } else if (this.symbolSelect.get('value') === "esriSMSSquare") {
                            this.previewSymbol = this.surface.createRect({x: 100 - sz / 2, y: 20 - sz / 2, width: sz, height: sz});
                        } else if (this.symbolSelect.get('value') === "esriSMSDiamond") {
                            this.previewSymbol = this.surface.createRect({x: 100 - sz / 2, y: 20 - sz / 2, width: sz, height: sz});
                            this.previewSymbol.setTransform(gfx.matrix.rotategAt(45, 100, 20));
                        }
                        if (this.previewSymbol !== null) {
                            this.previewSymbol.setFill(this.myColor);
                            this.previewSymbol.setStroke({color: this.myOutlineColor, width: this.drawingInfo_renderer_symbol_outline_width.get('value'), cap: "round"});
                        }
                    } else if (this.config.geometryType === "esriGeometryPolygon") {
                        //this.surface.createPolyline([{x: 100, y: 5}, {x: 125, y: 2}, {x: 125, y: 25}, {x: 100, y: 25}, {x: 100, y: 5}])
                        this.surface.createPolyline([{x: 25, y: 25}, {x: 25, y: 10}, {x: 175, y: 5}, {x: 175, y: 25}, {x: 25, y: 25}])
                                .setStroke({
                                    color: this.myOutlineColor,
                                    width: this.drawingInfo_renderer_symbol_outline_width.get('value'),
                                    style: this.getLinestyle(this.outlineSelect.get("value")),
                                    cap: "round"})
                                .setFill(this.myColor);
                    } else if (this.config.geometryType === "esriGeometryPolyline") {
                        this.surface.createLine({x1: 30, y1: 20, x2: 170, y2: 20})
                                .setStroke({
                                    color: this.myColor,
                                    width: this.drawingInfo_renderer_symbol_width.get('value'),
                                    style: this.getLinestyle(this.symbolSelect.get("value"))
                                });
                    }
                },
                initDrawingInfo: function() {

                    //this.initValues();

                    this.drawingInfo_renderer_symbol_size.set('value', this.config.drawingInfo.renderer.symbol.size);
                    this.drawingInfo_renderer_symbol_width.set('value', this.config.drawingInfo.renderer.symbol.width);
                    this.drawingInfo_renderer_symbol_outline_width.set('value', this.config.drawingInfo.renderer.symbol.outline.width);
                    this.updateColor(this.config.drawingInfo.renderer.symbol.color, this.config.drawingInfo.renderer.symbol.color[3]);
                    this.updateOutlineColor(this.config.drawingInfo.renderer.symbol.outline.color, this.config.drawingInfo.renderer.symbol.outline.color[3]);

                    this.hideNode(this.drawingInfo_li);
                    this.hideNode(this.drawingInfo_renderer_symbol_color_li);
                    this.hideNode(this.drawingInfo_renderer_symbol_size_li);
                    this.hideNode(this.drawingInfo_renderer_symbol_width_li);
                    this.hideNode(this.drawingInfo_renderer_symbol_outline_color_li);
                    this.hideNode(this.drawingInfo_renderer_symbol_outline_width_li);
                    this.hideNode(this.drawingInfo_renderer_symbol_style_li);
                    this.hideNode(this.drawingInfo_renderer_symbol_outline_style_li);
                    this.hideNode(this.customDrawingInfo_li);
                    //this.hideNode(this.geometrySelect_li);

                    if (this.config.type === "Feature Layer") {

                        this.showNode(this.drawingInfo_li);
                        //this.showNode(this.geometrySelect_li);

                        if (this.config.drawingInfo.renderer.type === "simple" && !this.customRenderer) {

                            this.showNode(this.drawingInfo_renderer_symbol_color_li);
                            this.showNode(this.drawingInfo_li);
                            this.showNode(this.switchToCustomDrawingInfo_li);

                            /*
                             if (this.geometrySelect === null) {
                             this.geometrySelect = new Select({
                             name: "geometryType",
                             style: "width:100%;",
                             value: this.config.geometryType,
                             options: [
                             {label: "Polyline", value: "esriGeometryPolyline"},
                             {label: "Point", value: "esriGeometryPoint"},
                             {label: "Polygon", value: "esriGeometryPolygon"}
                             ],
                             onChange: lang.hitch(this, function(val) {
                             console.log(val);
                             this.config.geometryType = val;
                             this.symbolSelect = null;
                             this.initDrawingInfo();
                             })
                             }, this.geometry_Select);
                             }
                             */

                            domClass.add(this.color, "icon-tint");
                            domClass.add(this.outlinecolor, "icon-tint");
                            domStyle.set(this.color, 'font-size', '180%');
                            domStyle.set(this.outlinecolor, 'font-size', '180%');

                            domClass.add(this.switchToCustomDrawingInfo, "icon-edit");
                            domStyle.set(this.switchToCustomDrawingInfo, 'font-size', '180%');

                            if (this.config.geometryType === "esriGeometryPoint") {

                                this.showNode(this.drawingInfo_renderer_symbol_size_li);
                                this.showNode(this.drawingInfo_renderer_symbol_outline_color_li);
                                this.showNode(this.drawingInfo_renderer_symbol_outline_width_li);
                                this.showNode(this.drawingInfo_renderer_symbol_style_li);

                                if (this.symbolSelect === null) {
                                    this.symbolSelect = new Select({
                                        name: "drawingInfo.renderer.symbol.style",
                                        style: "width:100%;",
                                        value: this.config.drawingInfo.renderer.symbol.style,
                                        options: [
                                            {label: "Circle", value: "esriSMSCircle"},
                                            {label: "Cross", value: "esriSMSCross"},
                                            {label: "Diamond", value: "esriSMSDiamond"},
                                            {label: "Square", value: "esriSMSSquare"},
                                            {label: "X", value: "esriSMSX"}
                                        ],
                                        onChange: lang.hitch(this, function(val) {
                                            this.showNode(this.drawingInfo_renderer_symbol_color_li);
                                            this.updateSymbol();
                                        })
                                    }, this.drawingInfo_renderer_symbol_style);
                                }
                            } else if (this.config.geometryType === "esriGeometryPolygon") {

                                this.showNode(this.drawingInfo_renderer_symbol_outline_color_li);
                                this.showNode(this.drawingInfo_renderer_symbol_outline_width_li);
                                this.showNode(this.drawingInfo_renderer_symbol_outline_style_li);

                                if (this.outlineSelect === null) {
                                    this.outlineSelect = new Select({
                                        name: "drawingInfo.renderer.symbol.outline.style",
                                        style: "width:100%;",
                                        value: this.config.drawingInfo.renderer.symbol.outline.style,
                                        options: [
                                            {label: "Dash", value: "esriSLSDash"},
                                            {label: "DashDotDot", value: "esriSLSDashDotDot"},
                                            {label: "Dot", value: "esriSLSDot"},
                                            {label: "Solid", value: "esriSLSSolid"}
                                        ],
                                        onChange: lang.hitch(this, function(val) {
                                            this.showNode(this.drawingInfo_renderer_symbol_color_li);
                                            this.updateSymbol();
                                        })
                                    }, this.drawingInfo_renderer_symbol_outline_style);
                                }
                            } else if (this.config.geometryType === "esriGeometryPolyline") {

                                this.showNode(this.drawingInfo_renderer_symbol_width_li);
                                this.showNode(this.drawingInfo_renderer_symbol_style_li);

                                if (this.symbolSelect === null) {
                                    this.symbolSelect = new Select({
                                        name: "drawingInfo.renderer.symbol.style",
                                        style: "width:100%;",
                                        value: this.config.drawingInfo.renderer.symbol.style,
                                        options: [
                                            {label: "Dash", value: "esriSLSDash"},
                                            {label: "DashDotDot", value: "esriSLSDashDotDot"},
                                            {label: "Dot", value: "esriSLSDot"},
                                            {label: "Solid", value: "esriSLSSolid"}
                                        ],
                                        onChange: lang.hitch(this, function(val) {
                                            this.updateSymbol();
                                        })
                                    }, this.drawingInfo_renderer_symbol_style);
                                }
                            }

                            on(this.color, "click", lang.hitch(this, this.openColorPalette));
                            on(this.outlinecolor, "click", lang.hitch(this, this.openOutlineColorPalette));

                            on(this.switchToCustomDrawingInfo, "click", lang.hitch(this, this.onSwitchToCustomDrawingInfo));

                            on(this.drawingInfo_renderer_symbol_size, "change", lang.hitch(this, this.sizeChanged));
                            on(this.drawingInfo_renderer_symbol_width, "change", lang.hitch(this, this.widthChanged));
                            on(this.drawingInfo_renderer_symbol_outline_width, "change", lang.hitch(this, this.outlineWidthChanged));

                            this.updateColor(this.config.drawingInfo.renderer.symbol.color, this.config.drawingInfo.renderer.symbol.color[3]);
                            this.updateOutlineColor(this.config.drawingInfo.renderer.symbol.outline.color, this.config.drawingInfo.renderer.symbol.outline.color[3]);
                            this.updateSymbol();

                            if (this.colorPalette === null) {
                                this.colorPalette = new ColorPalette({
                                    palette: "7x10",
                                    onChange: lang.hitch(this, function(val) {
                                        this.colorOpen = false;
                                        this.updateColor(val, this.myAlpha);
                                        this.updateSymbol();
                                        focusUtil.focus(this.colorSlider);
                                    })
                                });
                            }
                            if (this.outlinecolorPalette === null) {
                                this.outlinecolorPalette = new ColorPalette({
                                    palette: "7x10",
                                    onChange: lang.hitch(this, function(val) {
                                        this.outlinecolorOpen = false;
                                        this.updateOutlineColor(val, this.myOutlineAlpha);
                                        this.updateSymbol();
                                        focusUtil.focus(this.outlineColorSlider);
                                    })
                                });
                            }

                            this.sizeSlider = new HorizontalSlider({
                                name: "slider",
                                value: this.config.drawingInfo.renderer.symbol.size,
                                minimum: 1,
                                maximum: 30,
                                discreteValues: 30,
                                intermediateChanges: true,
                                style: "width:150px;float:right;margin-top:5px;",
                                onChange: lang.hitch(this, function(val) {
                                    this.drawingInfo_renderer_symbol_size.set('value', val);

                                    this.updateSymbol();

                                })
                            }, this.drawingInfo_renderer_symbol_size_slider);

                            this.widthSlider = new HorizontalSlider({
                                name: "slider",
                                value: this.config.drawingInfo.renderer.symbol.width,
                                minimum: 1,
                                maximum: 30,
                                discreteValues: 30,
                                intermediateChanges: true,
                                style: "width:150px;float:right;margin-top:5px;",
                                onChange: lang.hitch(this, function(val) {
                                    this.drawingInfo_renderer_symbol_width.set('value', val);
                                    this.updateSymbol();
                                })
                            }, this.drawingInfo_renderer_symbol_width_slider);

                            this.outlineWidthSlider = new HorizontalSlider({
                                name: "slider",
                                value: this.config.drawingInfo.renderer.symbol.outline.width,
                                minimum: 0,
                                maximum: 30,
                                discreteValues: 31,
                                intermediateChanges: true,
                                style: "width:150px;float:right;margin-top:5px;",
                                onChange: lang.hitch(this, function(val) {
                                    this.drawingInfo_renderer_symbol_outline_width.set('value', val);
                                    this.updateSymbol();
                                })
                            }, this.drawingInfo_renderer_symbol_outline_width_slider);

                            this.colorSlider = new HorizontalSlider({
                                name: "slider",
                                value: 255 - this.myAlpha,
                                minimum: 0,
                                maximum: 255,
                                intermediateChanges: true,
                                style: "width:150px;float:right;margin-top:5px;",
                                onChange: lang.hitch(this, function(val) {
                                    this.updateColor(this.myColor, 255 - val);

                                    this.updateSymbol();
                                })
                            }, this.drawingInfo_renderer_symbol_alpha);

                            this.outlineColorSlider = new HorizontalSlider({
                                name: "slider",
                                value: 255 - this.myOutlineAlpha,
                                minimum: 0,
                                maximum: 255,
                                intermediateChanges: true,
                                style: "width:150px;float:right;margin-top:5px",
                                onChange: lang.hitch(this, function(val) {
                                    this.updateOutlineColor(this.myOutlineColor, 255 - val);

                                    this.updateSymbol();
                                })
                            }, this.drawingInfo_renderer_symbol_outline_alpha);
                        } else {
                            this.showNode(this.customDrawingInfo_li);
                            this.customDrawingInfo.set('value', json.toJson(this.config.drawingInfo));

                            this.hideNode(this.switchToCustomDrawingInfo_li);
                            this.hideNode(this.drawingInfo_li);

                            this.hideNode(this.drawingInfo_renderer_symbol_color_li);
                        }

                    }
                },
                onSwitchToCustomDrawingInfo: function() {

                    this.customRenderer = true;
                    this.initDrawingInfo();
                },
                openColorPalette: function() {
                    if (!this.colorOpen) {
                        this.colorOpen = true;
                        popup.open({
                            parent: this,
                            popup: this.colorPalette,
                            around: this.color,
                            orient: ["below-centered", "above-centered"],
                            onExecute: function() {
                                popup.close(this.colorPalette);
                            },
                            onCancel: function() {
                                popup.close(this.colorPalette);
                            },
                            onClose: function() {
                            }
                        });
                    } else {
                        popup.close(this.colorPalette);
                        this.colorOpen = false;
                    }
                },
                openOutlineColorPalette: function() {
                    if (!this.outlinecolorOpen) {
                        this.outlinecolorOpen = true;
                        popup.open({
                            parent: this,
                            popup: this.outlinecolorPalette,
                            around: this.outlinecolor,
                            orient: ["below-centered", "above-centered"],
                            onExecute: function() {
                                popup.close(this.outlinecolorPalette);
                            },
                            onCancel: function() {
                                popup.close(this.outlinecolorPalette);
                            },
                            onClose: function() {
                            }
                        });
                    } else {
                        popup.close(this.outlinecolorPalette);
                        this.outlinecolorOpen = false;
                    }
                },
                hideNode: function(node) {
                    domStyle.set(node, 'display', 'none');
                },
                showNode: function(node) {
                    domStyle.set(node, 'display', '');
                },
                updateColor: function(rgb, alpha) {
                    var tmp = new Color(rgb);

                    var tmp0 = tmp.toRgb();
                    tmp0[3] = alpha / 255;

                    this.myColor = new Color(tmp0);
                    this.myAlpha = alpha;


                    this.drawingInfo_renderer_symbol_color.set('value', this.myColor.toRgb() + "," + this.myAlpha);
                    domStyle.set(this.color, "color", this.myColor);
                },
                updateOutlineColor: function(rgb, alpha) {
                    var tmp = new Color(rgb);

                    var tmp0 = tmp.toRgb();
                    tmp0[3] = alpha / 255;

                    this.myOutlineColor = new Color(tmp0);
                    this.myOutlineAlpha = alpha;

                    this.drawingInfo_renderer_symbol_outline_color.set('value', this.myOutlineColor.toRgb() + "," + this.myOutlineAlpha);
                    domStyle.set(this.outlinecolor, "color", this.myOutlineColor);
                },
                sizeChanged: function(val) {
                    this.sizeSlider.set('value', val);
                },
                widthChanged: function(val) {
                    this.widthSlider.set('value', val);
                },
                outlineWidthChanged: function(val) {
                    this.outlineWidthSlider.set('value', val);
                },
                gatherFormValues: function() {
                    var layer = this.inherited(arguments);
                    if (this.mappingEditorChanges && lang.isArray(this.mappingEditorChanges) && this.mappingEditorChanges.length > 0) {

                        layer.fieldsConfig = this.mappingEditorChanges;
                        if (this.useAsId !== null && this.useAsId !== "") {
                            layer.useAsId = this.useAsId;
                        }
 

                    }
                    layer.enabled = this.enabled;

                    return layer;
                },
                enabledChanged: function(val) {
                    this.enabled = val;
                }
            });
        });