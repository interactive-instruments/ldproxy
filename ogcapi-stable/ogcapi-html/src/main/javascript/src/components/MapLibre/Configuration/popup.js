const changeCursor = (map, cursor) => {
  const canvas = map.getCanvas();
  canvas.style.cursor = cursor;
};

const firstCoordinate = (geometry) => {
  switch (geometry.type) {
    case "Point":
      return geometry.coordinates;
    case "LineString":
    case "MultiPoints":
      return geometry.coordinates[0];
    case "Polygon":
    case "MultiLineString":
      return geometry.coordinates[0][0];
    case "MultiPolygon":
      return geometry.coordinates[0][0][0];
    default:
      return null;
  }
};

const showPopup = (map, popup) => (e) => {
  changeCursor(map, "pointer");

  const lngLat = firstCoordinate(e.features[0].geometry);
  const description = e.features[0].id;

  // Ensure that if the map is zoomed out such that multiple
  // copies of the feature are visible, the popup appears
  // over the copy being pointed to.
  while (Math.abs(e.lngLat.lng - lngLat[0]) > 180) {
    lngLat[0] += e.lngLat.lng > lngLat[0] ? 360 : -360;
  }

  if (lngLat && description) {
    popup.setLngLat(lngLat).setHTML(description).addTo(map);
  }
};

const showPopupProps = (map, popup) => (e) => {
  const lngLat = firstCoordinate(e.features[0].geometry);
  const title =
    e.features[0].sourceLayer ||
    e.features[0].properties.featureType ||
    "feature";
  let description = `<h5>${title}</h5><hr/><table>`;

  Object.keys(e.features[0].properties)
    .sort()
    .forEach((prop) => {
      description += `<tr><td title="${prop}" class="pr-4"><strong>${prop}</strong></td><td title="${e.features[0].properties[prop]}">${e.features[0].properties[prop]}</td></tr>`;
    });

  description += "</table>";

  // Ensure that if the map is zoomed out such that multiple
  // copies of the feature are visible, the popup appears
  // over the copy being pointed to.
  while (Math.abs(e.lngLat.lng - lngLat[0]) > 180) {
    lngLat[0] += e.lngLat.lng > lngLat[0] ? 360 : -360;
  }

  if (lngLat && description) {
    popup.setLngLat(lngLat).setHTML(description).addTo(map);
  }
};

const hidePopup = (map, popup) => () => {
  changeCursor(map, "");
  popup.remove();
};

export const addPopup = (map, maplibre, layerIds = ["points"]) => {
  const popup = new maplibre.Popup({
    closeButton: false,
    closeOnClick: false,
  });

  layerIds.forEach((layerId) => {
    map.on("mouseenter", layerId, showPopup(map, popup));
    map.on("mouseleave", layerId, hidePopup(map, popup));
  });
};

export const addPopupProps = (map, maplibre, layerIds = []) => {
  const popup = new maplibre.Popup({
    closeButton: true,
    closeOnClick: true,
    maxWidth: "50%",
    className: "popup-props",
  });

  layerIds.forEach((layerId) => {
    map.on("mouseenter", layerId, () => changeCursor(map, "pointer"));
    map.on("mouseleave", layerId, () => changeCursor(map, ""));
    map.on("click", layerId, showPopupProps(map, popup));
  });
};

/*
    style.mustache

    <style>
    {{#layerSwitcher}}
    #menu {
    background: #fff;
    position: absolute;
    z-index: 1;
    top: 10px;
    right: 50px;
    border-radius: 3px;
    border: 1px solid rgba(0, 0, 0, 0.4);
    font-family: 'Open Sans', sans-serif;
    }

    #menu a {
    font-size: 12px;
    color: #404040;
    display: block;
    margin: 0;
    padding: 0;
    padding: 5px;
    text-decoration: none;
    border-bottom: 1px solid rgba(0, 0, 0, 0.25);
    text-align: center;
    }

    #menu a:last-child {
    border: none;
    }

    #menu a:hover {
    background-color: #f8f8f8;
    color: #404040;
    }

    #menu a.active {
    background-color: #3887be;
    color: #ffffff;
    }

    #menu a.active:hover {
    background: #3074a4;
    }
    {{/layerSwitcher}}
    </style>
*/

/*
    style.mustache

    <script>

    {{#layerSwitcher}}
    var toggleableLayerIdMap = {{{layerIds}}};

    // set up the corresponding toggle button for each layer
    for (var tileLayerId of Object.keys(toggleableLayerIdMap)) {
        var link = document.createElement('a');
        link.href = '#';
        link.className = 'active';
        link.textContent = tileLayerId;

        link.onclick = function (e) {
            var clickedLayer = this.textContent;
            e.preventDefault();
            e.stopPropagation();

            var styleLayerIds = toggleableLayerIdMap[clickedLayer];
            var obj = this;

            styleLayerIds.forEach(function(styleLayerId) {
                var visibility = map.getLayoutProperty(styleLayerId, 'visibility');

                // toggle layer visibility by changing the layout object's visibility property
                if (visibility === 'visible') {
                    obj.className = '';
                    map.setLayoutProperty(styleLayerId, 'visibility', 'none');
                } else {
                    obj.className = 'active';
                    map.setLayoutProperty(styleLayerId, 'visibility', 'visible');
                }
            })
        };

        var layers = document.getElementById('menu');
        layers.appendChild(link);
    }
    {{/layerSwitcher}}
    </script>

    {{#layerSwitcher}}
    <nav id="menu"></nav>
    {{/layerSwitcher}}
*/
