export default function Line(props) {
  const { layer, image, expr } = props;
  const linePatternDataUrl = image(expr(layer, "paint", "line-pattern"));

  const style = {
    stroke: linePatternDataUrl ? `url(#img1)` : expr(layer, "paint", "line-color"),
    strokeWidth: Math.max(2, Math.min(expr(layer, "paint", "line-width"), 8)),
    strokeOpacity: expr(layer, "paint", "line-opacity"),
    strokeDasharray: expr(layer, "paint", "line-dasharray"),
  };
  const sw = style.strokeWidth;
  let cssStyle = `stroke: ${style.stroke};`;
  cssStyle += `stroke-width: ${sw};`;
  if (style.strokeOpacity) {
    cssStyle += `stroke-opacity: ${style.strokeOpacity};`;
  }
  if (style.strokeDasharray) {
    cssStyle += `stroke-dasharray: ${style.strokeDasharray};`;
  }

  return {
    element: "svg",
    attributes: {
      viewBox: "0 0 20 20",
      xmlns: "http://www.w3.org/2000/svg",
    },
    children: [
      {
        element: "defs",
        attributes: {
          key: "defs",
        },
        children: [
          {
            element: "pattern",
            attributes: {
              key: "pattern",
              id: "img1",
              x: 0,
              y: 0,
              width: style.strokeWidth,
              height: style.strokeWidth,
              patternUnits: "userSpaceOnUse",
              patternTransform: `translate(${-(sw / 2)} ${-(sw / 2)}) rotate(45)`,
            },
            children: linePatternDataUrl
              ? [
                  {
                    element: "image",
                    attributes: {
                      key: "img",
                      xlinkHref: linePatternDataUrl,
                      x: 0,
                      y: 0,
                      width: style.strokeWidth,
                      height: style.strokeWidth,
                    },
                  },
                ]
              : [],
          },
        ],
      },
      {
        element: "path",
        attributes: {
          key: "path",
          style: cssStyle,
          d: "M0 20 L 20 0",
        },
      },
    ],
  };
}
