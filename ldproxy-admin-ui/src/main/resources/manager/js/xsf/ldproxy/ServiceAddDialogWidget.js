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