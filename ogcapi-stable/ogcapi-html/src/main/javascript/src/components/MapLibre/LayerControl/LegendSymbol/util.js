/* eslint-disable react/destructuring-assignment */
import { expression, latest, function as styleFunction } from "@maplibre/maplibre-gl-style-spec";

const PROP_MAP = [
  ["background"],
  ["circle"],
  ["fill-extrusion"],
  ["fill"],
  ["heatmap"],
  ["hillshade"],
  ["line"],
  ["raster"],
  ["icon", "symbol"],
  ["text", "symbol"],
];

export function exprHandler({ zoom, properties }) {
  function prefixFromProp(prop) {
    const out = PROP_MAP.find((def) => {
      const type = def[0];
      return prop.startsWith(type);
    });
    return out ? out[1] || out[0] : null;
  }

  return function (layer, type, prop) {
    const prefix = prefixFromProp(prop);
    const specItem = latest[`${type}_${prefix}`][prop];
    const dflt = specItem.default;

    if (!layer[type]) {
      return dflt;
    }

    const input = layer[type][prop];

    const objType = typeof input;
    // Is it an expression...
    if (objType === "undefined") {
      return specItem.default;
    }
    if (typeof input === "object") {
      let expr;
      if (Array.isArray(input)) {
        if (specItem.type === "array") {
          // Special case: some properties are arrays, which should not be mistaken for expressions. It should be treated as a literal value.
          return input;
        }
        expr = expression.createExpression(input).value;
      } else {
        expr = styleFunction.createFunction(input, specItem);
      }
      if (!expr.evaluate) {
        return null;
      }
      const result = expr.evaluate({ zoom }, { properties });
      if (result) {
        // Because it can be a resolved image.
        return result.name || result;
      }
      return null;
    }
    return input;
  };
}

export function mapImageToDataURL(map, icon) {
  if (!icon) {
    return undefined;
  }

  const image = map.style.imageManager.images[icon];
  if (!image) {
    return undefined;
  }

  const canvasEl = document.createElement("canvas");
  canvasEl.width = image.data.width;
  canvasEl.height = image.data.height;
  const ctx = canvasEl.getContext("2d");
  ctx.putImageData(
    new ImageData(Uint8ClampedArray.from(image.data.data), image.data.width, image.data.height),
    0,
    0
  );

  // Not toBlob() because toDataURL is faster and synchronous.
  return canvasEl.toDataURL();
}

const dataStore = new Map();
export const cache = {
  add: (key, value) => {
    if (dataStore.has(key)) {
      throw new Error(`Cache already contains '${key}'`);
    }
    dataStore.set(key, {
      value,
      count: 1,
    });
  },
  fetch: (key) => {
    const cacheObj = dataStore.get(key);
    if (cacheObj) {
      cacheObj.count += 1;
      return cacheObj.value;
    }
    return null;
  },
  release: (key) => {
    const cacheObj = dataStore.get(key);
    if (!cacheObj) {
      throw new Error(`No such key in cache '${key}'`);
    }
    cacheObj.count -= 1;

    if (cacheObj.count === 0) {
      dataStore.delete(key);
    }
  },
};

function loadImageViaTag(url) {
  let cancelled = false;
  const promise = new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = "Anonymous";
    img.onload = () => {
      if (!cancelled) resolve(img);
    };
    img.onerror = (e) => {
      if (!cancelled) reject(e);
    };
    img.src = url;
  });
  promise.cancel = () => {
    cancelled = true;
  };
  return promise;
}

function removeUrl(obj) {
  const obj2 = { ...obj };
  delete obj2.url;
  return obj2;
}

function loadImageViaFetch(url, init) {
  return fetch(url, init)
    .then((res) => res.blob())
    .then((blob) => URL.createObjectURL(blob))
    .then((url2) => loadImageViaTag(url2));
}

export function loadImage(url, { transformRequest }) {
  const fetchObj = { ...transformRequest(url) };

  if (fetchObj.headers) {
    return loadImageViaFetch(url, removeUrl(fetchObj));
  }
  return loadImageViaTag(url);
}

export function loadJson(url, { transformRequest }) {
  const fetchObj = { ...transformRequest(url) };
  return fetch(fetchObj.url, removeUrl(fetchObj)).then((res) => res.json());
}
