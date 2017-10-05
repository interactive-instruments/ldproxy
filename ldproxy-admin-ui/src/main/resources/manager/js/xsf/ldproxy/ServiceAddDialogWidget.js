/*
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
define([
    "dojo/_base/declare",
    "xsf/api/ServiceAddDialogWidget",
    "dojo/text!./ServiceAddDialogWidget/template.html"
    ],
    function(declare, ServiceAddDialogWidget, template){
        return declare([ServiceAddDialogWidget], {
            
            _getChildFormTemplate: function() {
                return template;
            }
            
        });
    });