import React from 'react';
import { Text } from 'grommet';

import Metadata from '.';

export default {
  title: 'ogcapi-common/Metadata',
  component: Metadata
};

const Template = (args) => <Metadata {...args} />;

export const Plain = Template.bind({});

Plain.args = {
  contactName: 'Max Mustermann',
  contactUrl: 'https://example.com',
  contactEmail: 'foobar@example.com',
  contactPhone: '+491234567890',
  licenseName: 'License Name',
  licenseUrl: 'https://license.example.com',
  keywords: 'demo, test, example, new',
  version: '1.0.0',
  onChange: change => console.log(change)
};