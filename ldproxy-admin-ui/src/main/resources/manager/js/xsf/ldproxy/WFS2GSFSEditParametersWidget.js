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
    "dijit/_TemplatedMixin",
    "dijit/_WidgetsInTemplateMixin",
    "dojo/text!./WFS2GSFSEditParametersWidget/template.html",
    "dojo/_base/lang",
    "dojo/when",
    'dijit/form/Form',
    'dojox/form/manager/_Mixin',
    'dojox/form/manager/_FormMixin',
    'dojox/form/manager/_DisplayMixin',
    'dojox/form/manager/_ValueMixin',
    'dijit/form/ValidationTextBox',
    'dojox/validate',
    "dojo/query",
    "dojo/i18n"
],
        function(declare, WidgetBase, TemplatedMixin, WidgetsInTemplateMixin,
        template, lang, when, Form, FormMgrMixin, FormMgrFormMixin, 
        FormMgrDisplayMixin, FormMgrValueMixin,
                ValidationTextBox, Validate, query, i18n) {
            return declare([WidgetBase, Form, WidgetsInTemplateMixin, FormMgrMixin, FormMgrFormMixin, FormMgrDisplayMixin, FormMgrValueMixin], {
                templateString: template,
                baseClass: "serviceEditGeneralWidget",
                dialog: null,
                adminStore: null,
                pageWidget: null,
                destroyProgressMessage: null,
                title: "Add Service",
                service: null,                
                postMixInProperties: function() {
                    this.messages = i18n.getLocalization("xsf.WFS2GSFS", "Module", navigator.userLanguage || navigator.language);
                },
                postCreate: function() {
                    if (this.config.flipDefaultSRSAxisOrder) {
                        this.flipDefaultSRSAxisOrder.set('checked', true);
                    }
                    if (this.config.flipRequestSRSAxisOrder) {
                        this.flipRequestSRSAxisOrder.set('checked', true);
                    }
                    if (this.config.supportsMaxAllowableOffset) {
                        this.supportsMaxAllowableOffset.set('checked', true);
                    }
                }

            });
        });