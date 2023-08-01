export default function Fill(props) {
  const { image, expr, layer } = props;
  const dataUrl = image(expr(layer, "paint", "fill-pattern"));

  const style = {
    width: "100%",
    height: "100%",
    backgroundImage: `url(${dataUrl})`,
    backgroundColor: expr(layer, "paint", "fill-color"),
    opacity: expr(layer, "paint", "fill-opacity"),
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
