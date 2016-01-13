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