export default function Fill(props) {
  const { image, expr, layer } = props;
  const { url: dataUrl } = image(expr(layer, "paint", "fill-pattern"));
  const baseStyle = {
    width: "100%",
    height: "100%",
    opacity: expr(layer, "paint", "fill-opacity"),
  };
  const style = dataUrl
  ? {
    ...baseStyle,
    backgroundImage: `url(${dataUrl})`,
    backgroundPosition: "top left",
  }
  : {
    ...baseStyle,
    backgroundColor: expr(layer, "paint", "fill-color"),
    backgroundSize: "66% 66%",
    backgroundPosition: "center",
   };

  return {
    element: "div",
    attributes: {
      style,
    },
  };
}
