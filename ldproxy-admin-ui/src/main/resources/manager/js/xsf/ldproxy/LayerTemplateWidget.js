define([
    "dojo/_base/declare",
    "dijit/_WidgetBase",
    "dijit/_TemplatedMixin",
    "dojo/_base/lang",
    "dojo/text!./LayerTemplateWidget/template.html",
    "dojo/on",
    "dojo/when",
    "xsf/ConfirmationDialogWidget",
    "dojo/dom-style",
    "dojo/store/JsonRest",
    "xsf/FlashMessageMixin",
    "xsf/WFS2GSFS/ServiceEditLayerWidget",
    "dojo/i18n",
    "dojo/string"
],
        function(declare, WidgetBase, TemplatedMixin, lang, template, on, when,
                ConfirmationDialogWidget, DomStyle, JsonRest, FlashMessageMixin, 
                ServiceEditLayerWidget, i18n, string) {
            return declare([WidgetBase, TemplatedMixin, FlashMessageMixin], {
                templateString: template,
                templateid: null,
                namespace: null,
                layername: null,
                ltwcontainer: null,
                mappings: null,
                config: null,
                adminStore: null,
                postCreate: function() {
                    this.flashMessageNode = this.messageNode;
                    
                    this.messages = i18n.getLocalization("xsf.WFS2GSFS", "Module", navigator.userLanguage || navigator.language || navigator.language);
                    
                    on(this.editControl, "click", lang.hitch(this, this._loadTemplateEditor));
                    on(this.deleteControl, "click", lang.hitch(this, this._showDeleteTemplateConfirm));
                },
                _loadTemplateEditor: function() {

                    var ltStore = this.adminStore.getSubStore("/"+this.templateid);
                    when(ltStore.query(
                            
                            {}, {
                    }), lang.hitch(this, this._loadNamespaces), lang.hitch(this, this._onFailure));
                },
                _loadNamespaces: function(value) {
                    
                    this.mappings = value.featureTypeMapping.elementMappings;
                    this.config = value;
                    
                    var ltStore = this.adminStore.getSubStore("/getnamespaces");
                    
                    when(ltStore.query(
                            "?namespace=" + this.namespace +
                            "&layername=" + this.layername,
                            {}, {
                    }), lang.hitch(this, this._showTemplateEditor), lang.hitch(this, this._onFailure));
                    
                },
                _showTemplateEditor: function(value) {
                    var sel = new ServiceEditLayerWidget({
                        service: this.service,
                        config: this.config,
                        adminStore: this.adminStore.getSubStore("/"+this.templateid),
                        pageWidget: this.pageWidget,
                        mappings: this.mappings,
                        namespaces: value
                    });
                    sel.show();
                },
                _showDeleteTemplateConfirm: function() {
                    var confirm = new ConfirmationDialogWidget({
                        title: string.substitute(this.messages.deleteLayerTemplate, {name: this.layername}),
                        content: string.substitute(this.messages.reallyDeleteLayerTemplate, {name: this.layername}),
                        action: lang.hitch(this, function() {
                            this._deleteTemplate();
                        })
                    });
                    confirm.onHide = lang.hitch(confirm, function() {
                        this.destroyRecursive();
                    });
                    confirm.show();
                },
                _deleteTemplate: function() {
                    when(this.adminStore.remove(this.templateid), lang.hitch(this, this._onSuccess), lang.hitch(this, this._onFailure));
                },
                _onSuccess: function() {
                    this._hideNode(this.outernode);
                    lang.hitch(this, this.success, string.substitute(this.messages.deleteTemplateSuccess, {name: this.layername}), 5000)();
                    console.log("_onSuccess");
                },
                _onFailure: function() {
                    lang.hitch(this, this.error, string.substitute(this.messages.deleteTemplateFail, {name: this.layername}), 0)();
                    console.log("_onFailure");
                },
                _hideNode: function(node) {
                    DomStyle.set(node, 'display', 'none');
                },
            });
        });