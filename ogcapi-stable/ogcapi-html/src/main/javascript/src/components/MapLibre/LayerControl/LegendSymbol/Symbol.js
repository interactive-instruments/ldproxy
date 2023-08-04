function renderIconSymbol({ expr, layer, image }) {
  const imgKey = expr(layer, "layout", "icon-image");
  const imgSize = expr(layer, "layout", "icon-size");

  if (!imgKey) {
    return null;
  }
  const {
    url: dataUrl,
    dimensions: { width, height },
  } = image(imgKey);

  // eslint-disable-next-line no-nested-ternary
  const backgroundSize = imgSize
    ? imgSize * width > 16 || imgSize * height > 16
      ? "contain"
      : `${imgSize * width}px ${imgSize * height}px`
    : "contain";

  if (dataUrl) {
    return {
      element: "div",
      attributes: {
        style: {
          backgroundImage: `url(${dataUrl})`,
          backgroundSize,
          backgroundPosition: "center",
          backgroundRepeat: "no-repeat",
          width: "100%",
          height: "100%",
        },
      },
    };
  }
  return null;
}

function renderTextSymbol({ expr, layer }) {
  const textColor = expr(layer, "paint", "text-color");
  const textOpacity = expr(layer, "paint", "text-opacity");
  const textHaloColor = expr(layer, "paint", "text-halo-color");
  const textHaloWidth = expr(layer, "paint", "text-halo-width");

  // A "T" shape to signify text
  const d = "M 4,4 L 16,4 L 16,7 L 11.5 7 L 11.5 16 L 8.5 16 L 8.5 7 L 4 7 Z";

  return {
    element: "svg",
    attributes: {
      viewBox: "0 0 20 20",
      xmlns: "http://www.w3.org/2000/svg",
    },
    children: [
      {
        element: "path",
        attributes: {
          key: "l1",
          d,
          stroke: textHaloColor,
          "stroke-width": textHaloWidth * 2,
          fill: "transparent",
          "stroke-linejoin": "round",
        },
      },
      {
        element: "path",
        attributes: {
          key: "l2",
          d,
          fill: "white",
        },
      },
      {
        element: "path",
        attributes: {
          key: "l3",
          d,
          fill: textColor,
          opacity: textOpacity,
        },
      },
    ],
  };
}

export default function Symbol(props) {
  return renderIconSymbol(props) || renderTextSymbol(props);
}
