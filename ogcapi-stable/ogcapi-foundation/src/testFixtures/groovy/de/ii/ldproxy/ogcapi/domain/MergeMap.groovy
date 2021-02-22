package de.ii.ldproxy.ogcapi.domain

trait MergeMap<T extends ExtensionConfiguration> {

    abstract T getMap();

    abstract T getMapFullMerged();

    def getUseCases() {
        super.getUseCases() << [
                "map into full",
                "merging a configuration with map values into a full configuration with differing values",
                "source and target maps should be merged",
                getMap(),
                getFull(),
                getMapFullMerged()
        ]
    }

}
