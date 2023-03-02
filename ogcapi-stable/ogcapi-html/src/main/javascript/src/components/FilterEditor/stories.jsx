import React from "react";

import Editor from "./Editor";
import EditorHeader from "./Editor/Header";

export default {
  title: "@ogcapi/html/FilterEditor",
  component: Editor,
};

const Template = (args) => {
  const deleteFilters = (field) => () => {
    setFilters((current) => {
      const copy = { ...current };
      delete copy[field];
      return copy;
    });
  };

  return (
    <>
      <EditorHeader {...args} />
      <Editor {...args} deleteFilters={deleteFilters} />
    </>
  );
};

export const Plain = Template.bind({});

Plain.args = {
  fields: {
    firstName: "Vorname",
    lastName: "Nachname",
    age: "Alter",
    alive: "Lebendig",
    accountBalance: "Kontostand",
  },
  isOpen: true,
  isEnabled: true,
  filters: {
    firstName: { value: "Pascal", add: false, remove: false },
  },
  spatial: [
    [5.719412969894958, 50.31135979170666],
    [9.46927842749998, 53.15055217399161],
  ],
  code: { age: "567" },
  integerKeys: ["accountBalance"],
  booleanProperty: ["alive"],
  titleForFilter: {
    firstName: "Vorname",
    lastName: "Nachname",
    age: "Alter",
    alive: "Lebendig",
    accountBalance: "Kontostand",
  },
  start: 1666375200000,
  end: 1666375200000,
  temporal: {
    start: 1666375200000,
    end: 1666375200000,
  },
};
