import React from "react";
import LegendSymbol from "./main";
import Raster from "./Raster";

const camelCase = (obj) =>
  Object.assign(
    {},
    ...Object.keys(obj).map((key) => {
      const camelCased = key.includes("-")
        ? key.replace(/-[a-z]/g, (g) => g[1].toUpperCase())
        : key;
      return { [camelCased]: obj[key] };
    })
  );

function CSSstring(string) {
  const cssJson = `{"${string.replace(/;$/, "").replace(/;/g, '", "').replace(/: /g, '": "')}"}`;
  const obj = JSON.parse(cssJson);

  return camelCase(obj);
}

function asReact(tree, outerStyle) {
  let newStyle = {};
  const { style, ...attributes } = tree.attributes;

  if (typeof style === "string") {
    newStyle = CSSstring(style);
  } else if (typeof style === "object") {
    newStyle = style;
  }

  if (outerStyle) {
    newStyle = { ...newStyle, ...outerStyle };
  }

  return React.createElement(
    tree.element,
    { ...camelCase(attributes), style: newStyle },
    tree.children ? tree.children.map((c) => asReact(c)) : null
  );
}

export function LegendSymbolReact(props) {
  const icon = LegendSymbol(props);

  if (!icon) {
    return Raster(props);
  }

  return asReact(icon, props.style);
}
