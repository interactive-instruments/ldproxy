import React from 'react';
import { Text } from 'grommet';

import Api from '.';

export default {
  title: 'ogcapi-common/Api',
  component: Api
};

const Template = (args) => <Api {...args} />;

export const Plain = Template.bind({});

Plain.args = {
  commonCore: {
    enabled: true,
    includeHomeLink: false,
    useLangParameter: true,
    includeLinkHeader: true
  },
  json: {
    enabled: false
  },
  html: {
    enabled: true
  },
  oas30: {
    enabled: true
  },
  featuresCore: {
    enabled: true,
    defaultCrs: "CRS84h",
    minimumPageSize: 5,
    defaultPageSize: 10,
    maxPageSize: 15,
    showsFeatureSelfLink: true
  },
  geosJson: {
    enabled: true,
    nestedObjectStrategy: "NEST",
    multiplicityStrategy: "ARRAY",
    separator: '.',
    useFormattedJsonOutput: true
  },
  featuresHtml: {
    enabled: true,
    layout: "Option1"
  },
  crs: {
    enabled: true,
    additionalCrs: "EPSG:4326"
  },
  styles: {
    enabled: true,
    resourcesEnabled: false
  },
  queryables: {enabled: true},
  schema: {enabled: true},
  filterCql: {enabled: true},
  xml: {enabled: false},
  featuresGml: {enabled: false},
  onChange: change => console.log(change)
};