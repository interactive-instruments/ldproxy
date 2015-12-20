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