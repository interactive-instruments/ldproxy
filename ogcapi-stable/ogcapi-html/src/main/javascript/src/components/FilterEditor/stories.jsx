import React, { useState, useEffect } from "react";
import qs from "qs";

import Editor from "./Editor";
import EditorHeader from "./Editor/Header";

export default {
  title: "@ogcapi/html/FilterEditor",
  component: Editor,
};

const Template = (args) => {
  const [filters, setFilters] = useState({});
  const [isOpen, setOpen] = useState(false);
  const fields = { firstName: "Vorname" };

  const toBounds = (filter) => {
    const a = filter.split(",");
    const b = [
      [parseFloat(a[0]), parseFloat(a[1])],
      [parseFloat(a[2]), parseFloat(a[3])],
    ];
    return b;
  };

  const query = qs.parse(window.location.search, {
    ignoreQueryPrefix: true,
  });

  useEffect(() => {
    setFilters(
      Object.keys(fields)
        .concat(["bbox", "datetime"])
        .reduce((reduced, field) => {
          if (query[field]) {
            // eslint-disable-next-line no-param-reassign
            reduced[field] = {
              value: query[field],
              add: false,
              remove: false,
            };
          }
          return reduced;
        }, {})
    );
  }, []);

  const onAdd = (field, value) => {
    setFilters((prev) => ({
      ...prev,
      [field]: { value, add: true, remove: false },
    }));
  };

  const deleteFilters = (field) => () => {
    setFilters((current) => {
      const copy = { ...current };
      delete copy[field];
      return copy;
    });
  };

  const onRemove = (field) => {
    setFilters((prev) => ({
      ...prev,
      [field]: { value: prev[field].value, add: false, remove: true },
    }));
  };

  const save = (event) => {
    event.target.blur();

    const newFilters = Object.keys(filters).reduce((reduced, key) => {
      if (filters[key].add || !filters[key].remove) {
        // eslint-disable-next-line no-param-reassign
        reduced[key] = {
          ...filters[key],
          add: false,
          remove: false,
        };
      }
      return reduced;
    }, {});

    delete query.offset;

    Object.keys(fields)
      .concat(["bbox", "datetime"])
      .forEach((field) => {
        delete query[field];
        if (newFilters[field]) {
          query[field] = newFilters[field].value;
        }
      });

    // eslint-disable-next-line no-undef
    window.location.search = qs.stringify(query, {
      addQueryPrefix: true,
    });
  };

  const cancel = (event) => {
    event.target.blur();

    const newFilters = Object.keys(filters).reduce((reduced, key) => {
      if (!filters[key].add) {
        // eslint-disable-next-line no-param-reassign
        reduced[key] = {
          ...filters[key],
          add: false,
          remove: false,
        };
      }
      return reduced;
    }, {});

    setFilters(newFilters);
    setOpen(false);
  };

  const enabled = true;

  const editorHeaderProps = {
    isOpen,
    setOpen,
    isEnabled: enabled,
    filters,
    save,
    cancel,
    onRemove,
  };

  return (
    <>
      <EditorHeader {...editorHeaderProps} />
      <Editor
        {...args}
        filters={filters}
        setFilters={setFilters}
        onAdd={onAdd}
        deleteFilters={deleteFilters}
        toBounds={toBounds}
        onRemove={onRemove}
        save={save}
      />
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
  filters: {},
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
