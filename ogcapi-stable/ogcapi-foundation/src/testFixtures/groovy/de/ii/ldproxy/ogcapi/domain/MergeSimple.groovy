package de.ii.ldproxy.ogcapi.domain

trait MergeSimple<T extends ExtensionConfiguration> {

    abstract T getSimple();

    abstract T getSimpleFullMerged();

    def getUseCases() {
        super.getUseCases() << [
                "simple into full",
                "merging a configuration with simple values into a full configuration with differing values",
                "values from the source configuration should override target values",
                getSimple(),
                getFull(),
                getSimpleFullMerged()
        ]
    }

}
