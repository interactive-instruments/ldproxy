package de.ii.ldproxy.ogcapi.domain

trait MergeMinimal<T extends ExtensionConfiguration> {

    abstract T getMinimal();

    abstract T getMinimalFullMerged();

    def getUseCases() {
        super.getUseCases() << [
                "minimal into full",
                "merging a minimal configuration into a full configuration",
                "null/empty values should not override target values",
                getMinimal(),
                getFull(),
                getMinimalFullMerged()
        ]
    }

}
