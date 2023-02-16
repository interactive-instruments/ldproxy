import React from "react";

import FilterEditor from "./FetchingSpatialTemporal";

export default {
  title: "@ogcapi/html/FilterEditor",
  component: FilterEditor,
};

const Template = (args) => <FilterEditor {...args} />;

export const Plain = Template.bind({});

Plain.args = {
  fields: { firstName: "Vorname" },
};
