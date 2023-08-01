import React from "react";
import LegendSymbol from "@watergis/legend-symbol";

function CSSstring(string) {
  const cssJson = `{"${string.replace(/;$/, "").replace(/;/g, '", "').replace(/: /g, '": "')}"}`;
  const obj = JSON.parse(cssJson);

  const keyValues = Object.keys(obj).map((key) => {
    const camelCased = key.replace(/-[a-z]/g, (g) => g[1].toUpperCase());
    return { [camelCased]: obj[key] };
  });
  return Object.assign({}, ...keyValues);
}

function asReact(tree) {
  if (tree.attributes.style) {
    // eslint-disable-next-line no-param-reassign
    tree.attributes.style = CSSstring(tree.attributes.style);
  }
  return React.createElement(
    tree.element,
    tree.attributes,
    tree.children ? tree.children.map(asReact) : null
  );
}

export function LegendSymbolReact(props) {
  return asReact(LegendSymbol(props));
}
