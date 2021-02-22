package de.ii.ldproxy.ogcapi.domain

trait MergeCollection<T extends ExtensionConfiguration> {

    abstract T getCollection();

    abstract T getCollectionFullMerged();

    def getUseCases() {
        super.getUseCases() << [
                "collection into full",
                "merging a configuration with collection values into a full configuration with differing values",
                "source and target collections should be merged",
                getCollection(),
                getFull(),
                getCollectionFullMerged()
        ]
    }

}
