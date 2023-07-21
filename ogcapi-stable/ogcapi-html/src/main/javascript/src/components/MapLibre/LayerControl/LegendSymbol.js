import React from "react";
import LegendSymbol from "@watergis/legend-symbol";

function CSSstring(string) {
  const css_json = `{"${string.replace(/;$/, "").replace(/;/g, '", "').replace(/: /g, '": "')}"}`;
  const obj = JSON.parse(css_json);

  const keyValues = Object.keys(obj).map((key) => {
    var camelCased = key.replace(/-[a-z]/g, (g) => g[1].toUpperCase());
    return { [camelCased]: obj[key] };
  });
  return Object.assign({}, ...keyValues);
}

function asReact(tree) {
  if (tree.attributes.style) {
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
