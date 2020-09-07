import React, { useState } from 'react';
import { Box, Form, FormField, TextInput, TextArea, CheckBox, Select } from 'grommet';
import { InfoLabel, useDebounceFields } from '@xtraplatform/core'

// From https://reactjs.org/docs/hooks-state.html
export default function Api({commonCore, json, html, oas30, featuresCore, geosJson, featuresHtml, crs, styles, queryables, schema, filterCql, xml, featuresGml, onChange}) {

  const fields = {
    commonCore: {
      enabled: commonCore.enabled,
      includeHomeLink: commonCore.includeHomeLink,
      useLangParameter: commonCore.useLangParameter,
      includeLinkHeader: commonCore.includeLinkHeader
    },
    json: {
      enabled: json.enabled
    },
    html: {
      enabled: html.enabled
    },
    oas30: {
      enabled: oas30.enabled
    },
    featuresCore: {
      enabled: featuresCore.enabled,
      defaultCrs: featuresCore.defaultCrs,
      minimumPageSize: featuresCore.minimumPageSize,
      defaultPageSize: featuresCore.defaultPageSize,
      maxPageSize: featuresCore.maxPageSize,
      showsFeatureSelfLink: featuresCore.showsFeatureSelfLink
    },
    geosJson: {
      enabled: geosJson.enabled,
      nestedObjectStrategy: geosJson.nestedObjectStrategy,
      multiplicityStrategy: geosJson.multiplicityStrategy,
      separator: geosJson.separator,
      useFormattedJsonOutput: geosJson.useFormattedJsonOutput
    },
    featuresHtml: {
      enabled: featuresHtml.enabled,
      layout: featuresHtml.layout
    },
    crs: {
      enabled: crs.enabled,
      additionalCrs: crs.additionalCrs
    },
    styles: {
      enabled: styles.enabled,
      styleEncodings: styles.styleEncodings
    },
    queryables: {
      enabled: queryables.enabled
    },
    schema: {
      enabled: schema.enabled
    },
    queryables: {
      enabled: queryables.enabled
    },
    filterCql: {
      enabled: filterCql.enabled
    },
    xml: {
      enabled: xml.enabled
    },
    featuresGml: {
      enabled: featuresGml.enabled
    }
  }

  const [state, setState] = useDebounceFields(fields, 2000, onChange);

  return (
    <Box pad={{ horizontal: 'small', vertical: 'medium' }} fill="horizontal">
      <Form>

        <FormField label={<InfoLabel label="Common Core (OGC_API_COMMON)" />}>
          <CheckBox name="commonCore" checked={state.commonCore.enabled} label="enabled" onChange={setState} toggle={true} />
          <CheckBox name="commonCore" checked={state.commonCore.includeHomeLink} label="includeHomeLink" onChange={setState} toggle={true} />
          <CheckBox name="commonCore" checked={state.commonCore.useLangParameter} label="useLangParameter" onChange={setState} toggle={true} />
          <CheckBox name="commonCore" checked={state.commonCore.includeLinkHeader} label="includeLinkHeader" onChange={setState} toggle={true} />
        </FormField>

        <FormField label={<InfoLabel label="JSON" />}>
          <CheckBox name="json" checked={state.json.enabled} label="Enabled" onChange={setState} toggle={true} />
        </FormField>

        <FormField label={<InfoLabel label="HTML" />}>
          <CheckBox name="html" checked={state.html.enabled} label="Enabled" onChange={setState} toggle={true} />
        </FormField>

        <FormField label={<InfoLabel label="OpenAPI 3.0 (OAS30)" />}>
          <CheckBox name="oas30" checked={state.oas30.enabled} label="Enabled" onChange={setState} toggle={true} />
        </FormField>

        <FormField label={<InfoLabel label="Features Core" />}>
          <CheckBox name="featuresCore" checked={state.featuresCore.enabled} label="Enabled" onChange={setState} toggle={true} />
          <Select name="featuresCore" options={['CRS84', 'CRS84h']} value={state.featuresCore.defaultCrs} onChange={setState} />
          <TextInput name="featuresCore" placeholder="Minimum page size" value={state.featuresCore.minimumPageSize} onChange={setState} />
          <TextInput name="featuresCore" placeholder="Default page size" value={state.featuresCore.defaultPageSize} onChange={setState} />
          <TextInput name="featuresCore" placeholder="Max page size" value={state.featuresCore.maxPageSize} onChange={setState} />
          <CheckBox name="featuresCore" checked={state.featuresCore.showsFeatureSelfLink} label="showsFeatureSelfLink" onChange={setState} toggle={true} />
        </FormField>

        <FormField label={<InfoLabel label="Features GeoJSON" />}>
          <CheckBox name="geosJson" checked={state.geosJson.enabled} label="Enabled" onChange={setState} toggle={true} />
          <InfoLabel label="nestedObjectStrategy" />
          <Select name="geosJson" options={['NEST', 'FLATTEN']} value={state.geosJson.nestedObjectStrategy} onChange={setState} />
          <InfoLabel label="multiplicityStrategy" />
          <Select name="geosJson" options={['ARRAY', 'SUFFIX ']} value={state.geosJson.multiplicityStrategy} onChange={setState} />
          <InfoLabel label="separator" />
          <Select name="geosJson" options={['.', '_']} value={state.geosJson.separator} onChange={setState} />
          <CheckBox name="geosJson" checked={state.geosJson.useFormattedJsonOutput} label="useFormattedJsonOutput" onChange={setState} toggle={true} />
        </FormField>

        <FormField label={<InfoLabel label="Features HTML" />}>
          <CheckBox name="featuresHtml" checked={state.featuresHtml.enabled} label="Enabled" onChange={setState} toggle={true} />
          <InfoLabel label="layout" />
          <Select name="featuresHtml" options={['Option1', 'Option2']} value={state.featuresHtml.layout} onChange={setState} />
        </FormField>

        <FormField label={<InfoLabel label="Coordinate Reference Systems" />}>
          <CheckBox name="crs" checked={state.crs.enabled} label="Enabled" onChange={setState} toggle={true} />
          <InfoLabel label="additionalCrs" />
          <Select
            name="crs"
            options={['EPSG:4326','EPSG:3857','EPSG:4258','EPSG:3395','EPSG:3034','EPSG:3035','EPSG:25831','EPSG:25832','EPSG:25833','EPSG:25834']}
            value={state.crs.additionalCrs}
            onChange={setState}
          />
        </FormField>

        <FormField label={<InfoLabel label="Styles" />}>
          <CheckBox name="styles" checked={state.styles.enabled} label="Enabled" onChange={setState} toggle={true} />
          <CheckBox name="styles" checked={state.styles.resourcesEnabled} label="resourcesEnabled" onChange={setState} toggle={true} />
        </FormField>

        <FormField label={<InfoLabel label="Queryables" />}>
          <CheckBox name="queryables" checked={state.queryables.enabled} label="Enabled" onChange={setState} toggle={true} />
        </FormField>

        <FormField label={<InfoLabel label="Schema" />}>
          <CheckBox name="schema" checked={state.schema.enabled} label="Enabled" onChange={setState} toggle={true} />
        </FormField>

        <FormField label={<InfoLabel label="Filter (CQL)" />}>
          <CheckBox name="filterCql" checked={state.filterCql.enabled} label="Enabled" onChange={setState} toggle={true} />
        </FormField>

        <FormField label={<InfoLabel label="XML" />}>
          <CheckBox name="xml" checked={state.xml.enabled} label="Enabled" onChange={setState} toggle={true} />
        </FormField>

        <FormField label={<InfoLabel label="Features GML" />}>
          <CheckBox name="featuresGml" checked={state.featuresGml.enabled} label="Enabled" onChange={setState} toggle={true} />
        </FormField>

      </Form>
    </Box>
  );
}
