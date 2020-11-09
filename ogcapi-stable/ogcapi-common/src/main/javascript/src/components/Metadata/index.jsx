import React, { useState } from 'react';
import { Box, Form, FormField, TextInput, TextArea } from 'grommet';
import { InfoLabel, useDebounceFields } from '@xtraplatform/core'

// From https://reactjs.org/docs/hooks-state.html
export default function Metadata({contactName, contactUrl, contactEmail, contactPhone, licenseName, licenseUrl, keywords, version, onChange}) {

  const fields = {
      contactName: contactName,
      contactUrl: contactUrl,
      contactEmail: contactEmail,
      contactPhone: contactPhone,
      licenseName: licenseName,
      licenseUrl: licenseUrl,
      keywords: keywords,
      version: version
    }

  const postProcess = change => {
    change.keywords = change.keywords.split(',').map(keyword => keyword.trim());
    onChange(change);
  }

  const [state, setState] = useDebounceFields(fields, 2000, postProcess);

  return (
    <Box pad={{ horizontal: 'small', vertical: 'medium' }} fill="horizontal">
      <Form>
        <FormField label={<InfoLabel label="Contact Name" />}>
          <TextInput name="contactName" value={state.contactName} onChange={setState} />
        </FormField>
        <FormField label={<InfoLabel label="Contact URL" />}>
          <TextInput name="contactUrl" value={state.contactUrl} onChange={setState} />
        </FormField>
        <FormField label={<InfoLabel label="Contact Email" />}>
          <TextInput name="contactEmail" value={state.contactEmail} onChange={setState} />
        </FormField>
        <FormField label={<InfoLabel label="Contact Phone" />}>
          <TextInput name="contactPhone" value={state.contactPhone} onChange={setState} />
        </FormField>
        <FormField label={<InfoLabel label="License Name" />}>
          <TextInput name="licenseName" value={state.licenseName} onChange={setState} />
        </FormField>
        <FormField label={<InfoLabel label="License URL" />}>
          <TextInput name="licenseUrl" value={state.licenseUrl} onChange={setState} />
        </FormField>
        <FormField label={<InfoLabel label="Keywords" />}>
          <TextInput name="keywords" value={state.keywords} onChange={setState} />
        </FormField>
        <FormField label={<InfoLabel label="Version" />}>
          <TextInput name="version" value={state.version} onChange={setState} />
        </FormField>
      </Form>
    </Box>
  );
}
