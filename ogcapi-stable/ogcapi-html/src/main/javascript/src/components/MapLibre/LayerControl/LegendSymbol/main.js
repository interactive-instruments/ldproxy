import Circle from "./Circle";
import Fill from "./Fill";
import Line from "./Line";
import Symbol from "./Symbol";
import { exprHandler } from "./util";

function extractPartOfImage(img, { x, y, width, height, pixelRatio }) {
  const dpi = 1/pixelRatio;
  const el = document.createElement("canvas");
  el.width = width * dpi;
  el.height = height * dpi;
  const ctx = el.getContext("2d");
  ctx.drawImage(img, x, y, width, height, 0, 0, width * dpi, height * dpi);
  return { url: el.toDataURL(), dimensions: { width: width * dpi, height: height * dpi } };
}

export default function LegendSymbol({ sprite, zoom, layer, properties }) {
  const TYPE_MAP = {
    circle: Circle,
    symbol: Symbol,
    line: Line,
    fill: Fill,
  };

  const handler = TYPE_MAP[layer.type];
  const expr = exprHandler({ zoom, properties });
  const image = (imgKey) => {
    if (sprite && sprite.json) {
      const dimensions = sprite.json[imgKey];
      if (dimensions) {
        return extractPartOfImage(sprite.image, dimensions);
      }
    }
    return {};
  };

  if (handler) {
    return handler({ layer, expr, image });
  }
  return null;
}
