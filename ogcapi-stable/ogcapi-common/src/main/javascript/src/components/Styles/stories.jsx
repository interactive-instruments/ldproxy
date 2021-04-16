import React from "react";

import Form from ".";

export default {
  title: "ogcapi-stable/Styles",
  component: Form,
};

const Template = (args) => <Form {...args} />;

export const Plain = Template.bind({});

Plain.args = {
  enabled: true,
  onChange: (change) => console.log(change),
};
