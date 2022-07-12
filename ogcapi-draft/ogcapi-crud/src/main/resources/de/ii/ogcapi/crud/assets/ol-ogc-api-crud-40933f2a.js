(function (global, factory) {
  typeof exports === 'object' && typeof module !== 'undefined' ? module.exports = factory(require('ol/source/Vector'), require('ol/layer/Vector'), require('ol/interaction/Draw'), require('ol/interaction/Modify'), require('ol/interaction/Snap'), require('ol/format/GeoJSON'), require('ol/proj/Projection'), require('ol/interaction/Interaction'), require('ol/control/Control')) :
  typeof define === 'function' && define.amd ? define(['ol/source/Vector', 'ol/layer/Vector', 'ol/interaction/Draw', 'ol/interaction/Modify', 'ol/interaction/Snap', 'ol/format/GeoJSON', 'ol/proj/Projection', 'ol/interaction/Interaction', 'ol/control/Control'], factory) :
  (global = typeof globalThis !== 'undefined' ? globalThis : global || self, global.OgcApiEditor = factory(global.ol.source.Vector, global.ol.layer.Vector, global.ol.interaction.Draw, global.ol.interaction.Modify, global.ol.interaction.Snap, global.ol.format.GeoJSON, global.ol.proj.Projection, global.ol.interaction.Interaction, global.ol.control.Control));
})(this, (function (VectorSource, VectorLayer, Draw, Modify, Snap, GeoJSON, Projection, Interaction, Control) { 'use strict';

  function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e : { 'default': e }; }

  var VectorSource__default = /*#__PURE__*/_interopDefaultLegacy(VectorSource);
  var VectorLayer__default = /*#__PURE__*/_interopDefaultLegacy(VectorLayer);
  var Draw__default = /*#__PURE__*/_interopDefaultLegacy(Draw);
  var Modify__default = /*#__PURE__*/_interopDefaultLegacy(Modify);
  var Snap__default = /*#__PURE__*/_interopDefaultLegacy(Snap);
  var GeoJSON__default = /*#__PURE__*/_interopDefaultLegacy(GeoJSON);
  var Projection__default = /*#__PURE__*/_interopDefaultLegacy(Projection);
  var Interaction__default = /*#__PURE__*/_interopDefaultLegacy(Interaction);
  var Control__default = /*#__PURE__*/_interopDefaultLegacy(Control);

  function ownKeys(object, enumerableOnly) {
    var keys = Object.keys(object);

    if (Object.getOwnPropertySymbols) {
      var symbols = Object.getOwnPropertySymbols(object);
      enumerableOnly && (symbols = symbols.filter(function (sym) {
        return Object.getOwnPropertyDescriptor(object, sym).enumerable;
      })), keys.push.apply(keys, symbols);
    }

    return keys;
  }

  function _objectSpread2(target) {
    for (var i = 1; i < arguments.length; i++) {
      var source = null != arguments[i] ? arguments[i] : {};
      i % 2 ? ownKeys(Object(source), !0).forEach(function (key) {
        _defineProperty(target, key, source[key]);
      }) : Object.getOwnPropertyDescriptors ? Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)) : ownKeys(Object(source)).forEach(function (key) {
        Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key));
      });
    }

    return target;
  }

  function _classCallCheck(instance, Constructor) {
    if (!(instance instanceof Constructor)) {
      throw new TypeError("Cannot call a class as a function");
    }
  }

  function _defineProperties(target, props) {
    for (var i = 0; i < props.length; i++) {
      var descriptor = props[i];
      descriptor.enumerable = descriptor.enumerable || false;
      descriptor.configurable = true;
      if ("value" in descriptor) descriptor.writable = true;
      Object.defineProperty(target, descriptor.key, descriptor);
    }
  }

  function _createClass(Constructor, protoProps, staticProps) {
    if (protoProps) _defineProperties(Constructor.prototype, protoProps);
    if (staticProps) _defineProperties(Constructor, staticProps);
    Object.defineProperty(Constructor, "prototype", {
      writable: false
    });
    return Constructor;
  }

  function _defineProperty(obj, key, value) {
    if (key in obj) {
      Object.defineProperty(obj, key, {
        value: value,
        enumerable: true,
        configurable: true,
        writable: true
      });
    } else {
      obj[key] = value;
    }

    return obj;
  }

  function _inherits(subClass, superClass) {
    if (typeof superClass !== "function" && superClass !== null) {
      throw new TypeError("Super expression must either be null or a function");
    }

    subClass.prototype = Object.create(superClass && superClass.prototype, {
      constructor: {
        value: subClass,
        writable: true,
        configurable: true
      }
    });
    Object.defineProperty(subClass, "prototype", {
      writable: false
    });
    if (superClass) _setPrototypeOf(subClass, superClass);
  }

  function _getPrototypeOf(o) {
    _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf.bind() : function _getPrototypeOf(o) {
      return o.__proto__ || Object.getPrototypeOf(o);
    };
    return _getPrototypeOf(o);
  }

  function _setPrototypeOf(o, p) {
    _setPrototypeOf = Object.setPrototypeOf ? Object.setPrototypeOf.bind() : function _setPrototypeOf(o, p) {
      o.__proto__ = p;
      return o;
    };
    return _setPrototypeOf(o, p);
  }

  function _isNativeReflectConstruct() {
    if (typeof Reflect === "undefined" || !Reflect.construct) return false;
    if (Reflect.construct.sham) return false;
    if (typeof Proxy === "function") return true;

    try {
      Boolean.prototype.valueOf.call(Reflect.construct(Boolean, [], function () {}));
      return true;
    } catch (e) {
      return false;
    }
  }

  function _assertThisInitialized(self) {
    if (self === void 0) {
      throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
    }

    return self;
  }

  function _possibleConstructorReturn(self, call) {
    if (call && (typeof call === "object" || typeof call === "function")) {
      return call;
    } else if (call !== void 0) {
      throw new TypeError("Derived constructors may only return object or undefined");
    }

    return _assertThisInitialized(self);
  }

  function _createSuper(Derived) {
    var hasNativeReflectConstruct = _isNativeReflectConstruct();

    return function _createSuperInternal() {
      var Super = _getPrototypeOf(Derived),
          result;

      if (hasNativeReflectConstruct) {
        var NewTarget = _getPrototypeOf(this).constructor;

        result = Reflect.construct(Super, arguments, NewTarget);
      } else {
        result = Super.apply(this, arguments);
      }

      return _possibleConstructorReturn(this, result);
    };
  }

  var EditorSelect = /*#__PURE__*/function (_Interaction) {
    _inherits(EditorSelect, _Interaction);

    var _super = _createSuper(EditorSelect);

    /**
     * @param {Options} [opt_options] Options.
     */
    function EditorSelect(opt_options) {
      var _this;

      _classCallCheck(this, EditorSelect);

      _this = _super.call(this);
      var options = opt_options ? opt_options : {};
      _this._onSelect = options.onSelect || null;
      _this._sourceIds = options.sourceIds || [];
      return _this;
    }

    _createClass(EditorSelect, [{
      key: "handleEvent",
      value: function handleEvent(evt) {
        var _this2 = this;

        var stopEvent = false;

        if (evt.type === "click") {
          var map = evt.map;
          var features = [];
          map.forEachFeatureAtPixel(evt.pixel, function (feature, layer) {
            features.push({
              feature: feature,
              layer: layer.sourceId || layer.get("sourceId")
            });
          }, {
            layerFilter: function layerFilter(layer) {
              return _this2._sourceIds.length === 0 || _this2._sourceIds.includes(layer.sourceId) || _this2._sourceIds.includes(layer.get("sourceId"));
            }
          });

          if (features.length > 0) {
            this.setActive(false);

            this._onSelect(features);
          }

          stopEvent = true;
        }

        return !stopEvent;
      }
    }]);

    return EditorSelect;
  }(Interaction__default["default"]);

  var _state;

  var MODE = "mode";
  var GEO_TYPE = "geoType";
  var COLLECTIONS = "collections";
  var COLLECTION = "collection";
  var FEATURE = "feature";
  var GEOMETRY_CHANGES = "geometryChanges";
  var PROPERTY_CHANGES = "propertyChanges";
  var STATUS = "status";
  var ERROR = "error";
  var ETAG = "etag";
  var MODES = {
    OFF: "OFF",
    EDIT: "EDIT",
    ADD: "ADD"
  }; //cannot import ol/geom/GeometryType

  var GEO_TYPES = {
    POINT: "Point",
    LINE_STRING: "LineString",
    POLYGON: "Polygon",
    MULTI_POINT: "MultiPoint",
    MULTI_LINE_STRING: "MultiLineString",
    MULTI_POLYGON: "MultiPolygon"
  };
  var STATUSES = {
    IDLE: "IDLE",
    CREATE: "CREATE",
    EDIT: "EDIT",
    SYNC: "SYNC",
    SUCCESS: "SUCCESS",
    ERROR: "ERROR"
  };
  var state = (_state = {}, _defineProperty(_state, MODE, MODES.OFF), _defineProperty(_state, GEO_TYPE, GEO_TYPES.POINT), _defineProperty(_state, STATUS, STATUSES.IDLE), _defineProperty(_state, COLLECTIONS, {}), _defineProperty(_state, COLLECTION, undefined), _defineProperty(_state, FEATURE, undefined), _defineProperty(_state, GEOMETRY_CHANGES, undefined), _defineProperty(_state, PROPERTY_CHANGES, undefined), _defineProperty(_state, ERROR, undefined), _defineProperty(_state, ETAG, undefined), _defineProperty(_state, "F2", undefined), _state);

  var comparators = _defineProperty({}, GEOMETRY_CHANGES, function (geo) {
    return geo ? geo.getRevision() : undefined;
  });

  var hashes = {};
  var subscriptions = Object.keys(state).reduce(function (sub, key) {
    sub[key] = [];
    return sub;
  }, {});

  var isChange = function isChange(key, newValue) {
    var dirty = state[key] !== newValue;

    if (comparators[key]) {
      var hash = comparators[key](newValue);
      var hashDirty = hashes[key] !== hash;

      if (hashDirty) {
        hashes[key] = hash;
      }

      return dirty || hashDirty;
    }

    return dirty;
  };

  var get = function get(key) {
    return state[key];
  };
  var has = function has(key) {
    return state[key] !== undefined;
  };
  var set = function set(key, value) {
    if (state.hasOwnProperty(key)) {
      var finalValue = typeof value === "function" ? value(state[key]) : value;

      if (isChange(key, finalValue)) {
        state[key] = finalValue;
        subscriptions[key].forEach(function (sub) {
          return sub(finalValue);
        });
      }
    }
  };
  var on = function on(key, handler, init) {
    if (Array.isArray(key)) {
      key.forEach(function (k) {
        return on(k, handler);
      });
      return;
    }

    subscriptions[key].push(handler);

    if (init) {
      handler(get(key));
    }
  };
  var isDirty = function isDirty(key) {
    if (has(FEATURE) && has(PROPERTY_CHANGES)) {
      var original = get(FEATURE).properties;
      var changes = get(PROPERTY_CHANGES);
      return changes[key] !== original[key];
    }

    return false;
  };
  var hasChanges = function hasChanges() {
    var dirty = get(MODE) === MODES.ADD || has(FEATURE) && has(GEOMETRY_CHANGES);

    if (!dirty && has(FEATURE) && has(PROPERTY_CHANGES)) {
      var original = get(FEATURE).properties;
      var changes = get(PROPERTY_CHANGES);
      return Object.keys(changes).some(function (key) {
        return changes[key] !== original[key];
      });
    }

    return dirty;
  };

  var addClass = function addClass(element, className) {
    if (!element.className.includes(className)) {
      element.className += " ".concat(className);
    }
  };
  var removeClass = function removeClass(element, className) {
    if (element.className.includes(className)) {
      element.className = element.className.replace(className, "");
    }
  };

  var button$1 = function button(label, color, enabled, onClick) {
    var button = document.createElement("button");
    button.innerHTML = label;
    button.disabled = !enabled;
    button.className = "px-4 py-1 text-sm text-white bg-".concat(color, " disabled:bg-").concat(color, "/75 font-semibold rounded-full border border-transparent focus:outline-none hover:enabled:ring-2 hover:enabled:ring-").concat(color, " hover:enabled:ring-offset-2");

    if (onClick) {
      button.addEventListener("click", onClick, false);
    }

    return button;
  };

  var table = function table(props, onChange) {
    var table = document.createElement("table");
    table.className = "w-full border-collapse";
    props.forEach(function (prop) {
      var tr = document.createElement("tr");
      tr.className = "even:bg-slate-300/40";
      var td1 = document.createElement("td");
      td1.innerHTML = prop.label;
      td1.title = prop.name;
      td1.className = "w-1/3 p-1" + " pl-".concat(prop.depth * 4);
      var td2 = document.createElement("td");

      if (prop.isValue) {
        var input = document.createElement("input");
        input.type = "text";
        input.className = "w-full bg-transparent p-1 placeholder:italic placeholder:text-slate-400";
        input.value = prop.value;
        input.placeholder = "not set";
        input.addEventListener("input", function (evt) {
          var dirty = onChange(prop.name, evt.target.value);

          if (dirty) {
            if (!tr.className.includes("font-bold")) {
              tr.className += " font-bold";
            }
          } else {
            tr.className = tr.className.replace(" font-bold", "");
          }
        }, false);
        td2.appendChild(input);
      }

      tr.appendChild(td1);
      tr.appendChild(td2);
      table.appendChild(tr);
    });
    return table;
  };

  var panel$1 = function panel(buttons) {
    var panel = document.createElement("div");
    panel.id = "ogc-api-editor-panel";
    panel.className = "ol-unselectable font-sans bg-white/90 text-slate-700 absolute flex transform bottom-0 left-0 w-full h-1/4 ease-in-out transition-transform duration-1000 translate-y-full";
    var left = document.createElement("div");
    left.id = "ogc-api-editor-panel-main";
    left.className = "flex flex-col justify-between py-2 px-4 min-w-[150px]";
    var header1 = document.createElement("h3");
    header1.id = "ogc-api-editor-panel-header";
    header1.className = "text-xl font-bold text-slate-700";
    var header2 = document.createElement("h3");
    header2.id = "ogc-api-editor-panel-header2";
    header2.className = "text-lg font-semibold text-slate-500";
    var header = document.createElement("div");
    header.className = "flex flex-col items-center border-b-2 border-slate-500 pb-2";
    header.appendChild(header1);
    header.appendChild(header2);
    left.appendChild(header);
    var right = document.createElement("div");
    right.id = "ogc-api-editor-panel-attributes";
    right.className = "grow p-2 overflow-y-auto";
    buttons.forEach(function (button) {
      left.appendChild(button);
    });
    var loading = document.createElement("div");
    loading.className = "bg-white/90 absolute w-full h-full flex justify-center items-center hidden";
    var loading2 = document.createElement("div");
    loading2.className = "text-xl font-bold text-slate-500 animate-pulse";
    loading2.innerHTML = "Saving...";
    loading.appendChild(loading2);
    var error = document.createElement("div");
    error.className = "bg-red-200/90 absolute w-full h-full flex flex-col gap-2 justify-center items-center hidden";
    var errorMessage = document.createElement("div");
    errorMessage.className = "text-xl font-bold text-slate-500 text-center";
    error.appendChild(errorMessage);
    var errorButton = button$1("Ok", "slate-400", true, function () {
      return set(STATUS, STATUSES.EDIT);
    });
    error.appendChild(errorButton);
    panel.appendChild(left);
    panel.appendChild(right);
    panel.appendChild(loading);
    panel.appendChild(error);
    return {
      root: panel,
      header1: header1,
      header2: header2,
      right: right,
      loading: loading,
      error: error,
      errorMessage: errorMessage
    };
  };

  var onChange = function onChange(key, value) {
    set(PROPERTY_CHANGES, function () {
      var prev = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};
      return _objectSpread2(_objectSpread2({}, prev), {}, _defineProperty({}, key, value));
    });
    return isDirty(key);
  };

  var EditorPanel = /*#__PURE__*/function (_Control) {
    _inherits(EditorPanel, _Control);

    var _super = _createSuper(EditorPanel);

    function EditorPanel(opt_options) {
      var _this;

      _classCallCheck(this, EditorPanel);

      var options = opt_options || {};
      var buttonSave = button$1("Save", "sky-600", false, options.onSave);
      var buttonCancel = button$1("Cancel", "slate-300", true, options.onCancel);
      var buttonDelete = button$1("Delete", "red-600", true, options.onDelete);
      var panelComponents = panel$1([buttonSave, buttonCancel, buttonDelete]);
      _this = _super.call(this, {
        element: panelComponents.root,
        target: options.target
      });
      _this._panel = panelComponents;
      _this._buttonDelete = buttonDelete;
      on([PROPERTY_CHANGES, GEOMETRY_CHANGES], function () {
        return buttonSave.disabled = !hasChanges();
      });
      on(STATUS, function (status) {
        if (status === STATUSES.SYNC) {
          removeClass(panelComponents.loading, "hidden");
        } else if (status === STATUSES.IDLE) {
          setTimeout(function () {
            return addClass(panelComponents.loading, "hidden");
          }, 2000);
        } else if (status === STATUSES.ERROR) {
          panelComponents.errorMessage.innerHTML = get(ERROR);
          removeClass(panelComponents.error, "hidden");
          addClass(panelComponents.loading, "hidden");
        } else if (status === STATUSES.EDIT) {
          addClass(panelComponents.error, "hidden");
          panelComponents.errorMessage.innerHTML = "";
        }
      });
      return _this;
    }

    _createClass(EditorPanel, [{
      key: "show",
      value: function show(collection, id, props, showDelete) {
        this._panel.right.replaceChildren(table(props, onChange));

        this._panel.header1.innerHTML = "".concat(collection);
        this._panel.header2.innerHTML = "".concat(id);
        this.element.className = this.element.className.replace("translate-y-full", "translate-y-0");
        showDelete ? removeClass(this._buttonDelete, "hidden") : addClass(this._buttonDelete, "hidden");
      }
    }, {
      key: "hide",
      value: function hide() {
        this.element.className = this.element.className.replace("translate-y-0", "translate-y-full");
      }
    }]);

    return EditorPanel;
  }(Control__default["default"]);

  var getItem = function getItem(baseUrl, collection, id, crs) {
    //return fetch(`${baseUrl}/collections/${collection}/items/${id}?crs=${crs}`, {
    return fetch("".concat(baseUrl, "/collections/").concat(collection, "/items/").concat(id, "?schema=receivables"), {
      headers: {
        Accept: "application/geo+json"
      }
    }).then(parseJson(true));
  };
  var postItem = function postItem(baseUrl, collection, feature) {
    console.log("POST", collection);
    return fetch("".concat(baseUrl, "/collections/").concat(collection, "/items"), {
      headers: {
        "Content-Type": "application/geo+json; charset=utf-8"
      },
      method: "POST",
      body: JSON.stringify(feature)
    }).then(checkError); //return new Promise((resolve) => setTimeout(resolve, 1000));
  };
  var putItem = function putItem(baseUrl, collection, feature, etag) {
    var headers = {
      "Content-Type": "application/geo+json; charset=utf-8",
      Accept: "application/json"
    };
    if (etag) headers["If-Match"] = etag;
    console.log("PUT", collection, feature.id, headers);
    return fetch("".concat(baseUrl, "/collections/").concat(collection, "/items/").concat(feature.id), {
      headers: headers,
      method: "PUT",
      body: JSON.stringify(feature)
    }).then(checkError); //return new Promise((resolve) => setTimeout(resolve, 1000));
  };
  var deleteItem = function deleteItem(baseUrl, collection, id) {
    console.log("DELETE", collection, id);
    return fetch("".concat(baseUrl, "/collections/").concat(collection, "/items/").concat(id), {
      method: "DELETE"
    }); //return new Promise((resolve) => setTimeout(resolve, 1000));
  };
  var getSchema = function getSchema(baseUrl, collection) {
    return fetch("".concat(baseUrl, "/collections/").concat(collection, "/schemas/replace"), {
      headers: {
        Accept: "application/schema+json"
      }
    }).then(parseJson(false));
  };

  var parseJson = function parseJson(withETag) {
    return function (response) {
      console.log("ETag", response.headers.get("etag"));
      return response && response.ok ? response.json().then(function (json) {
        return withETag ? {
          feature: json,
          etag: response.headers.get("etag")
        } : json;
      }) : withETag ? {
        feature: {},
        etag: response.headers.get("etag")
      } : {};
    };
  };

  var checkError = function checkError(response) {
    if (response.ok) {
      return response;
    }

    if (response.bodyUsed) {
      return response.json().then(function (json) {
        parseError(response, json.detail);
      })["catch"](function () {
        parseError(response);
      });
    }

    parseError(response);
  };

  var parseError = function parseError(response, details) {
    var message = response.status + " " + response.statusText;
    if (details) message += "<br/>" + details;
    throw new Error(message);
  };

  var _GEO_ICONS;
  var OL_BUTTON_HOVER_COLOR = "rgba(0,60,136,.7)";
  var BG_ACTIVE = "!bg-[color:var(--ol-toggle-active-bg,".concat(OL_BUTTON_HOVER_COLOR, ")]");
  var GEO_ICONS = (_GEO_ICONS = {}, _defineProperty(_GEO_ICONS, GEO_TYPES.POINT, '<i class="icon-map-marker"></i>'), _defineProperty(_GEO_ICONS, GEO_TYPES.LINE_STRING, "L"), _defineProperty(_GEO_ICONS, GEO_TYPES.POLYGON, "Y"), _GEO_ICONS);

  var button = function button(label, title, enabled, onClick) {
    var button = document.createElement("button");
    button.innerHTML = label;
    button.title = title;
    button.disabled = !enabled;
    button.className = "disabled:opacity-70 hover:disabled:opacity-50";

    if (onClick) {
      button.addEventListener("click", onClick, false);
    }

    return button;
  };

  var geoButton = function geoButton(geoType) {
    return button(GEO_ICONS[geoType], "", true, function (evt) {
      set(MODE, MODES.ADD);
      switchGeoType(evt.target, geoType);
    });
  };

  var groupAdd = function groupAdd() {
    var buttonAdd = button(GEO_ICONS[get(GEO_TYPE)], "Create Point Features", true, function (evt) {
      return toggleMode(evt.target, MODES.ADD);
    });
    var buttonPoint = geoButton(GEO_TYPES.POINT);
    var buttonLine = geoButton(GEO_TYPES.LINE_STRING);
    var buttonPolygon = geoButton(GEO_TYPES.POLYGON);
    var group = document.createElement("div");
    group.className = "flex";
    group.appendChild(buttonAdd);
    var geos = document.createElement("div");
    geos.className = "hidden";
    geos.appendChild(buttonPoint);
    geos.appendChild(buttonLine);
    geos.appendChild(buttonPolygon);
    /*group.appendChild(geos);
     buttonAdd.addEventListener(
      "mouseenter",
      () => !buttonAdd.disabled && (geos.className = "flex"),
      false
    );
    group.addEventListener(
      "mouseleave",
      () => (geos.className = "hidden"),
      false
    );*/

    on(MODE, function (mode) {
      switch (mode) {
        case MODES.ADD:
          addClass(buttonAdd, BG_ACTIVE);
          break;

        case MODES.EDIT:
          removeClass(buttonAdd, BG_ACTIVE);
          buttonAdd.disabled = true;
          break;

        case MODES.OFF:
        default:
          removeClass(buttonAdd, BG_ACTIVE);
          buttonAdd.disabled = false;
          break;
      }
    }, true);
    on(GEO_TYPE, function (geoType) {
      buttonAdd.innerHTML = GEO_ICONS[geoType];

      switch (geoType) {
        case GEO_TYPES.POINT:
          addClass(buttonPoint, BG_ACTIVE);
          removeClass(buttonLine, BG_ACTIVE);
          removeClass(buttonPolygon, BG_ACTIVE);
          break;

        case GEO_TYPES.LINE_STRING:
          removeClass(buttonPoint, BG_ACTIVE);
          addClass(buttonLine, BG_ACTIVE);
          removeClass(buttonPolygon, BG_ACTIVE);
          break;

        case GEO_TYPES.POLYGON:
        default:
          removeClass(buttonPoint, BG_ACTIVE);
          removeClass(buttonLine, BG_ACTIVE);
          addClass(buttonPolygon, BG_ACTIVE);
          break;
      }
    }, true);
    return group;
  };

  var buttonEdit = function buttonEdit() {
    var buttonEdit = button('<i class="icon-pencil"></i>', "Edit Features", true, function (evt) {
      return toggleMode(evt.target, MODES.EDIT);
    });
    on(MODE, function (mode) {
      console.log(MODE, mode);

      switch (mode) {
        case MODES.ADD:
          removeClass(buttonEdit, BG_ACTIVE);
          buttonEdit.disabled = true;
          break;

        case MODES.EDIT:
          addClass(buttonEdit, BG_ACTIVE);
          break;

        case MODES.OFF:
        default:
          removeClass(buttonEdit, BG_ACTIVE);
          buttonEdit.disabled = false;
          break;
      }
    });
    return buttonEdit;
  };

  var toggleMode = function toggleMode(button, mode) {
    button.blur();
    set(MODE, function (prev) {
      return prev === mode ? MODES.OFF : mode;
    });
  };

  var switchGeoType = function switchGeoType(button, geoType) {
    button.blur();
    set(GEO_TYPE, geoType);
  };

  var EditorControls = /*#__PURE__*/function (_Control) {
    _inherits(EditorControls, _Control);

    var _super = _createSuper(EditorControls);

    function EditorControls(opt_options) {
      var _this;

      _classCallCheck(this, EditorControls);

      var options = opt_options || {};
      var element = document.createElement("div");
      element.className = "ogc-api-editor ol-unselectable ol-control left-2 top-20 flex flex-col";
      _this = _super.call(this, {
        element: element,
        target: options.target
      });
      element.appendChild(groupAdd());
      element.appendChild(buttonEdit());
      return _this;
    }

    return _createClass(EditorControls);
  }(Control__default["default"]);

  var geoJson = new GeoJSON__default["default"]({
    dataProjection: new Projection__default["default"]({
      code: "EPSG:4326",
      axisOrientation: "neu"
    })
  });

  var source = function source() {
    var source = new VectorSource__default["default"]({});
    on(FEATURE, function (feature) {
      if (feature) {
        var f = geoJson.readFeature(feature, {
          //TODO: derive from collection.crs
          featureProjection: "EPSG:3857"
        });
        source.addFeature(f);
      } else {
        source.clear();
      }
    });
    on(STATUS, function (status) {
      if (status === STATUSES.IDLE) {
        source.clear();
      }
    });
    return source;
  };

  var layer = function layer(source, style) {
    return new VectorLayer__default["default"]({
      source: source,
      style: style
    });
  };

  var modify = function modify(source) {
    var modify = new Modify__default["default"]({
      source: source
    });
    modify.on("modifyend", function (evt) {
      if (evt.features && evt.features.getLength() > 0) {
        console.log("mod", evt.features.item(0).getGeometry().getRevision());
        set(GEOMETRY_CHANGES, evt.features.item(0).getGeometry());
      }
    });
    on(FEATURE, function (feature) {
      return modify.setActive(!!feature);
    });
    on(STATUS, function (status) {
      return status === STATUSES.SYNC && modify.setActive(false);
    });
    return modify;
  };

  var select = function select(baseUrl, collections, source) {
    var select = new EditorSelect({
      sourceIds: Object.keys(collections),
      onSelect: function onSelect(results) {
        var result = results[0];
        var collection = collections[result.layer];
        var id = result.feature.getId(); //result.feature.setStyle([]);

        set("F2", result.feature);
        source.removeFeature(result.feature); //TODO: error handling

        getItem(baseUrl, collection.id, id, collection.crs).then(function (json) {
          set(COLLECTION, collection.id);
          set(FEATURE, json.feature);
          set(ETAG, json.etag);
        });
      }
    });
    select.setActive(false);
    on(MODE, function (mode) {
      return select.setActive(mode === MODES.EDIT);
    });
    on(FEATURE, function (feature) {
      return select.setActive(!feature);
    });
    return select;
  };

  var snap = function snap(source) {
    return new Snap__default["default"]({
      source: source
    });
  };

  var draws = function draws(source) {
    var draws = {};
    [GEO_TYPES.POINT, GEO_TYPES.LINE_STRING, GEO_TYPES.POLYGON].forEach(function (geoType) {
      var draw = new Draw__default["default"]({
        source: source,
        type: geoType
      });
      draw.setActive(false);
      draw.on("drawend", function (evt) {
        console.log(evt.feature.getGeometry().getRevision());
        set(STATUS, STATUSES.CREATE);
        set(GEOMETRY_CHANGES, evt.feature.getGeometry());
      });
      draws[geoType] = draw;
    });
    on(MODE, function (mode) {
      if (mode === MODES.ADD) {
        draws[get(GEO_TYPE)].setActive(true);
      } else {
        Object.values(draws).forEach(function (draw) {
          return draw.setActive(false);
        });
      }
    });
    on(GEO_TYPE, function (geoType) {
      Object.keys(draws).forEach(function (geoType2) {
        return draws[geoType2].setActive(geoType === geoType2);
      });
    });
    on(STATUS, function (status) {
      if (status === STATUSES.CREATE) {
        draws[get(GEO_TYPE)].setActive(false);
      } else if (status === STATUSES.IDLE && get(MODE) === MODES.ADD) {
        draws[get(GEO_TYPE)].setActive(true);
      }
    });
    return Object.values(draws);
  };

  var panel = function panel(source, onSaveAsync, onDeleteAsync) {
    var clear = function clear() {
      if (has("F2")) {
        source.addFeature(get("F2"));
      }

      set(COLLECTION, undefined);
      set(FEATURE, undefined);
      set(ETAG, undefined);
      set(GEOMETRY_CHANGES, undefined);
      set(PROPERTY_CHANGES, undefined);
      set(STATUS, STATUSES.IDLE);
      set("F2", undefined);
    };

    var panel = new EditorPanel({
      onSave: function onSave() {
        set(STATUS, STATUSES.SYNC);
        return onSaveAsync().then(function () {
          console.log("CLEARING");
          set("F2", undefined);
          clear();
        })["catch"](function (error) {
          set(ERROR, error.message);
          set(STATUS, STATUSES.ERROR);
        });
      },
      onDelete: function onDelete() {
        set(STATUS, STATUSES.SYNC);
        return onDeleteAsync().then(function () {
          console.log("CLEARING");
          set("F2", undefined);
          clear();
        });
      },
      onCancel: clear
    });
    on(FEATURE, function (feature) {
      if (feature) {
        var collection = get(COLLECTIONS)[get(COLLECTION)];
        var props = flattenProps(collection.properties, feature.properties);
        console.log(props);
        panel.show(collection.label, feature.id, props, true);
      } else {
        //source.refresh();
        panel.hide();
      }
    });
    on(STATUS, function (status) {
      if (status === STATUSES.CREATE) {
        set(COLLECTION, Object.keys(get(COLLECTIONS))[0]);
        var collection = get(COLLECTIONS)[get(COLLECTION)];
        var props = flattenProps(collection.properties, {});
        panel.show(collection.label, "New", props, false);
      } else if (status === STATUSES.IDLE) {
        panel.hide();
      }
    });
    return panel;
  };

  var flattenProps = function flattenProps(schema, props) {
    var depth = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : 1;
    var prefix = arguments.length > 3 && arguments[3] !== undefined ? arguments[3] : "";
    return Object.keys(schema).flatMap(function (name) {
      if (schema[name].type === "object") {
        return [{
          name: prefix + name,
          label: schema[name].title || name,
          isValue: false,
          depth: depth
        }].concat(flattenProps(schema[name].properties, props[name] || {}, depth + 1, prefix + name + "."));
      }

      return {
        name: prefix + name,
        label: schema[name].title || name,
        value: props[name] || "",
        isValue: true,
        depth: depth
      };
    });
  };

  var controls = function controls() {
    return new EditorControls({});
  };

  var buildFeature = function buildFeature() {
    var feature = get(MODE) === MODES.ADD ? //TODO: add crs
    {
      type: "Feature",
      properties: {}
    } : get(FEATURE);

    if (has(GEOMETRY_CHANGES)) {
      feature.geometry = geoJson.writeGeometryObject(get(GEOMETRY_CHANGES), {
        //TODO: derive from collection.crs or better feature.crs
        featureProjection: "EPSG:3857",
        rightHanded: true,
        //TODO: configurable?
        decimals: 10
      });
    }

    if (has(PROPERTY_CHANGES)) {
      var changes = get(PROPERTY_CHANGES);

      var newProps = _objectSpread2({}, feature.properties);

      Object.keys(changes).forEach(function (key) {
        var path = key.split(".");
        var obj = newProps;

        for (var i = 0; i < path.length - 1; i++) {
          obj = obj[path[i]];
        }

        obj[path[path.length - 1]] = changes[key];
      });
      console.log(newProps);
      feature.properties = newProps;
    }

    return feature;
  };

  var parseSchema = function parseSchema(schema, defs) {
    var s = _objectSpread2({}, schema);

    var props = s.properties;
    Object.keys(props).forEach(function (key) {
      if (props[key]["$ref"] && props[key]["$ref"].includes("$defs")) {
        //console.log(props[key]["$ref"], defs[props[key]["$ref"].substr(8)]);
        props[key] = defs[props[key]["$ref"].substr(8)];
      }

      if (props[key].type === "object") {
        props[key] = parseSchema(props[key], defs);
      }

      if (props[key].type === "array") {
        delete props[key];
      }
    });
    return s;
  };

  var OgcApiEditor = /*#__PURE__*/function () {
    function OgcApiEditor(opt_options) {
      _classCallCheck(this, OgcApiEditor);

      var options = opt_options || {};
      this._baseUrl = options.baseUrl || null;
      var collections = options.collections || {};
      var schemas = Object.values(collections).map(function (collection) {
        return getSchema(options.baseUrl, collection.id).then(function (schema) {
          console.log(schema);
          var s = parseSchema(schema.properties.properties, schema["$defs"]);
          console.log(s);
          set(COLLECTIONS, function (prev) {
            return _objectSpread2(_objectSpread2({}, prev), {}, _defineProperty({}, collection.id, _objectSpread2(_objectSpread2({}, collection), {}, {
              label: schema.title,
              properties: s.properties
            })));
          });
          return get(COLLECTIONS);
        });
      });
      Promise.all(schemas).then(function (all) {
        return console.log("INITIALIZED", all);
      });
      this._style = options.styleFunction || null;
      this._source = source();
      this._layer = layer(this._source, this._style);
      this._modify = modify(this._source);
      this._select = select(this._baseUrl, collections, options.vectorSource);
      this._snap = snap(this._source);
      this._draws = draws(this._source);
      this._panel = panel(options.vectorSource, this._syncChanges.bind(this), this._deleteFeature.bind(this));
      this._controls = controls();
      this._vectorSource = options.vectorSource;
    }

    _createClass(OgcApiEditor, [{
      key: "addToMap",
      value: function addToMap(map) {
        map.addLayer(this._layer);
        map.addInteraction(this._select);
        map.addInteraction(this._modify);
        map.addInteraction(this._snap);

        this._draws.forEach(function (draw) {
          return map.addInteraction(draw);
        });

        map.addControl(this._panel);
        map.addControl(this._controls);
      }
    }, {
      key: "_syncChanges",
      value: function _syncChanges() {
        var _this = this;

        console.log("_syncChanges");

        if (hasChanges()) {
          var feature = buildFeature();
          console.log(get(MODE));

          if (get(MODE) === MODES.ADD) {
            return postItem(this._baseUrl, get(COLLECTION), feature).then(function () {
              console.log("SAVED", JSON.stringify(feature));
              var f = geoJson.readFeature(feature, {
                //TODO: derive from collection.crs
                featureProjection: "EPSG:3857"
              });

              _this._vectorSource.addFeature(f);
            });
          }

          return putItem(this._baseUrl, get(COLLECTION), feature, get(ETAG)).then(function () {
            console.log("SAVED", JSON.stringify(feature));
            var f = geoJson.readFeature(feature, {
              //TODO: derive from collection.crs
              featureProjection: "EPSG:3857"
            });

            _this._vectorSource.addFeature(f);
          });
        }
      }
    }, {
      key: "_deleteFeature",
      value: function _deleteFeature() {
        if (has(FEATURE)) {
          return deleteItem(this._baseUrl, get(COLLECTION), get(FEATURE).id).then(function () {
            return console.log("DELETED");
          });
        }
      }
    }]);

    return OgcApiEditor;
  }();

  return OgcApiEditor;

}));
