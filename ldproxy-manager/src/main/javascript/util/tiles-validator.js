export const validateFormats = () => (value, ui) => {


    if (ui.formatJsonEnabled === null) {
        Array.isArray(ui.formatJsonArray) ?
            ui.formatJsonArray.forEach(function (entry) {
                if (entry.get("application/json") !== undefined)
                    ui.formatJsonEnabled = entry.get("application/json")

            })
            : ui.formatJsonEnabled = ui.formatJsonArray;
    }


    if (ui.formatMvtEnabled === null) {
        Array.isArray(ui.formatMvtArray) ?
            ui.formatMvtArray.forEach(function (entry) {
                if (entry.get("application/vnd.mapbox-vector-tile") !== undefined)
                    ui.formatMvtEnabled = entry.get("application/vnd.mapbox-vector-tile")
            })
            : ui.formatMvtEnabled = ui.formatMvtArray;
    }

    if (ui.formatJsonEnabled === true && ui.formatMvtEnabled === true) {
        var formatsToAdd = ["application/json", "application/vnd.mapbox-vector-tile"];
        ui.formats = ui.formats.concat(formatsToAdd);
    }
    if (ui.formatJsonEnabled === true && ui.formatMvtEnabled === false) {
        var formatsToAdd = ["application/json"];
        ui.formats = ui.formats.concat(formatsToAdd);
    }

    if (ui.formatJsonEnabled === false && ui.formatMvtEnabled === true) {
        var formatsToAdd = ["application/vnd.mapbox-vector-tile"];
        ui.formats = ui.formats.concat(formatsToAdd);
    }

    if (ui.formatJsonEnabled === false && ui.formatMvtEnabled === false) {
        var formatsToAdd = [];
        ui.formats = ui.formats.concat(formatsToAdd);
    }

}

export const validateZoomLevel = (isMax) => (value, ui) => {
    value = parseInt(value);
    if (value < 0 || value > 22)
        return "invalid for the Google Maps Tiling Scheme"
    if (isMax && value < ui.minZoomLevel)
        return "invalid, must be greater then the minimum zoom level"
    if (!isMax && value > ui.maxZoomLevel)
        return "invalid, must be smaller then the maximum zoom level"

}

export const validateSeeding = (isMax) => (value, ui) => {
    value = parseInt(value);
    if (isMax && value < ui.minSeeding)
        return "invalid, must be greater then the minimum seeding"
    if (!isMax && value > ui.maxSeeding)
        return "invalid, must be smaller then the maximum seeding"

    if (isMax && value > ui.maxZoomLevel)
        return "invalid for the specified zoom levels"
    if (!isMax && value < ui.minZoomLevel)
        return "invalid for the specified zoom levels"



}
