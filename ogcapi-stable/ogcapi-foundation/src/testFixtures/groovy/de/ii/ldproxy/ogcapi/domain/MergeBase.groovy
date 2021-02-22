package de.ii.ldproxy.ogcapi.domain

trait MergeBase<T extends ExtensionConfiguration> {

    abstract T getFull();

    def getUseCases() {
        []
    }

}
