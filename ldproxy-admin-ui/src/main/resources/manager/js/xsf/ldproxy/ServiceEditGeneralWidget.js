/*
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
define([
    "dojo/_base/declare",
    "xsf/ServiceEditGeneralWidget",
    "dojo/text!./ServiceEditGeneralWidget/template.html"
    ],
    function(declare, ServiceEditGeneralWidget, template){
        return declare([ServiceEditGeneralWidget], {
            
            _getChildFormTemplate: function() {
                return template;
            }
            
        });
    });
