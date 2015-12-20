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
