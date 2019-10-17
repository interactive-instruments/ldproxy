import React, { Component } from 'react';

import { Box, Heading, Text, FormField } from 'grommet';


import TextInputUi from 'xtraplatform-manager/src/components/common/TextInputUi';
import CheckboxUi from 'xtraplatform-manager/src/components/common/CheckboxUi';

import uiValidator, { forbiddenChars } from 'xtraplatform-manager/src/components/common/ui-validator';
const validateFormats = () => (value, ui) => {


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

const validateZoomLevel = (isMax) => (value, ui) => {
    value = parseInt(value);
    if (value < 0 || value > 22)
        return "invalid for the Google Maps Tiling Scheme"
    if (isMax && value < ui.minZoomLevel)
        return "invalid, must be greater then the minimum zoom level"
    if (!isMax && value > ui.maxZoomLevel)
        return "invalid, must be smaller then the maximum zoom level"

}

const validateSeeding = (isMax) => (value, ui) => {
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
@uiValidator({
    formats: validateFormats(),
    maxZoomLevel: validateZoomLevel(true),
    minZoomLevel: validateZoomLevel(false),
    maxSeeding: validateSeeding(true),
    minSeeding: validateSeeding(false)
}, true)

export default class EditTiles extends Component {


    _save = () => {
        const { ui, validator, onChange } = this.props;

        const newCapabilities = (type, change) => ui.extensions.map(ext => ext.extensionType === type ? { ...ext, ...change } : ext)

        if (validator.valid) {
            onChange({
                capabilities: newCapabilities('TILES', {
                    enabled: true,
                    formats: ui.formats,
                    zoomLevels: {
                        default: {
                            max: ui.maxZoomLevel,
                            min: ui.minZoomLevel
                        }
                    },
                    seeding: {
                        default: {
                            max: ui.maxSeeding,
                            min: ui.minSeeding
                        }
                    }
                })
            });
        }
    }

    render() {
        const { ui, updateUI, validator, tilesEnabled } = this.props;


        return (tilesEnabled &&
            <Box pad={{ horizontal: 'small', vertical: 'medium' }} fill="horizontal">

                <Box pad={{ bottom: 'small' }}>
                    <Text weight='bold'>Formats</Text>
                </Box>

                <FormField>
                    <Box pad={{ bottom: 'small' }}>
                        <CheckboxUi name='formatJsonEnabled'
                            label="application/json"
                            checked={ui.formatJsonEnabled}
                            onChange={updateUI}
                            onDebounce={this._save}
                            disabled={!ui.formatMvtEnabled}
                            toggle={true} />
                    </Box>
                    <Box pad={{ bottom: 'small' }}>
                        <CheckboxUi name='formatMvtEnabled'
                            label="application/vnd.mapbox-vector-tile"
                            checked={ui.formatMvtEnabled}
                            onChange={updateUI}
                            onDebounce={this._save}
                            disabled={!ui.formatJsonEnabled}
                            toggle={true} />
                    </Box>
                </FormField>

                <Box pad={{ top: 'medium', bottom: 'xsmall' }}>
                    <Text weight='bold'>Zoom levels</Text>
                </Box>

                <FormField label="minimum" error={validator.messages.minZoomLevel}>
                    <TextInputUi name="minZoomLevel"
                        value={ui.minZoomLevel}
                        placeHolder="value between 0 and 22"
                        onChange={updateUI}
                        onDebounce={this._save}
                    />
                </FormField>
                <FormField label="maximum" error={validator.messages.maxZoomLevel} >
                    <TextInputUi name="maxZoomLevel"
                        value={ui.maxZoomLevel}
                        placeHolder="value between 0 and 22"
                        onChange={updateUI}
                        onDebounce={this._save}
                    />
                </FormField>

                <Box pad={{ top: 'medium', bottom: 'xsmall' }}>
                    <Text weight='bold'>Seeding</Text>
                </Box>

                <FormField label="minimum" error={validator.messages.minSeeding}>
                    <TextInputUi name="minSeeding"
                        value={ui.minSeeding}
                        placeHolder="value between zoom Level range"
                        onChange={updateUI}
                        onDebounce={this._save}
                    />
                </FormField>
                <FormField label="maximum" error={validator.messages.maxSeeding}>
                    <TextInputUi name="maxSeeding"
                        value={ui.maxSeeding}
                        placeHolder="value between zoom Level range"
                        onChange={updateUI}
                        onDebounce={this._save}
                    />
                </FormField>

            </Box >
        );
    }
}
