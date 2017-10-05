/*
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
define([
    "dojo/_base/declare",
    "xsf/api/ServiceEditDialogWidget",
    "dojo/text!./ServiceEditDialogWidget/template.html"
],
        function (declare, ServiceEditDialogWidget, template) {
            return declare([ServiceEditDialogWidget], {
                postMixInProperties: function()
                {
                    this.inherited(arguments);

                    if (this.manualProvider) {
                        this.manualProvider.getManualIcon( this.dialog.titleBar, "editservices");
                    }
                },
                _getChildFormTemplate: function () {
                    return null;
                }

            });
        });