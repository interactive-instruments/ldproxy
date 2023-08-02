export const getSubLayerIds = (allParentGroups) =>
  allParentGroups
    .map((group) => {
      if (group.subLayers) {
        return group.subLayers;
      }
      if (group.entries) {
        return group.entries.filter(
          (entry) => entry.type !== "group" && entry.type !== "source-layer"
        );
      }
      return [];
    })
    .flatMap((layers) => {
      return layers.flatMap((subLayer) =>
        subLayer.layers ? [...new Set([subLayer.id].concat(subLayer.layers))] : [subLayer.id]
      );
    });

export const getRadioGroups = (groups, selected) => {
  const radioGroups = {};

  groups
    .filter((g) => g.isBasemap === true)
    .forEach((g) => {
      if (selected) {
        radioGroups[g.id] = g.entries && g.entries.length > 0 ? g.entries[0].id : null;
      } else {
        radioGroups[g.id] = g.entries ? g.entries.map((e) => e.id) : [];
      }
    });

  return radioGroups;
};

export const getDeps = (groups, parents = []) => {
  const deps = {};

  groups.forEach((g) => {
    const children = g.entries || g.subLayers || g.layers || [];
    const childDeps = getDeps(children, parents.concat(g.id ? [g.id] : [g]));
    deps[g.id || g] = [
      ...new Set(
        parents
          .concat(g.id ? [g.id] : [g])
          .concat(Object.values(childDeps).flatMap((c) => c.id || c))
      ),
    ];
    Object.keys(childDeps).forEach((c) => {
      deps[c] = childDeps[c];
    });
  });

  return deps;
};

export const getParentDeps = (groups, parents = []) => {
  const deps = {};

  groups.forEach((g) => {
    const id = g.id || g;
    const children = g.entries || g.subLayers || g.layers || [];
    const childDeps = getParentDeps(children, [id].concat(parents));
    deps[id] = parents.filter((id2) => id2 !== id);
    Object.keys(childDeps).forEach((c) => {
      deps[c] = childDeps[c];
    });
  });

  return deps;
};

export const getChildDeps = (groups, tmp) => {
  const deps = { clean: {}, tmp: {} };

  groups.forEach((g) => {
    const id = g.id || g;
    const children = g.entries || g.subLayers || g.layers || [];
    const childDeps = getChildDeps(children, true);
    deps.tmp[id] =
      Object.keys(childDeps.tmp).length > 0
        ? [...new Set([id].concat(Object.values(childDeps.tmp).flatMap((c) => c.id || c)))]
        : [id];
    deps.clean[id] = deps.tmp[id].filter((id2) => id2 !== id);
    Object.keys(childDeps.clean).forEach((c) => {
      deps.clean[c] = childDeps.clean[c];
    });
  });

  return tmp ? deps : deps.clean;
};
