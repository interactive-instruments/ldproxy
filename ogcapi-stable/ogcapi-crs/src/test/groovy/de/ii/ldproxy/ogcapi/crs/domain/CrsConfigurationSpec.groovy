package de.ii.ldproxy.ogcapi.crs.domain

import com.google.common.collect.ImmutableList
import de.ii.ldproxy.ogcapi.domain.*
import de.ii.xtraplatform.crs.domain.EpsgCrs

@SuppressWarnings('ClashingTraitMethods')
class CrsConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<CrsConfiguration>, MergeMinimal<CrsConfiguration>, MergeSimple<CrsConfiguration>, MergeCollection<CrsConfiguration> {

    @Override
    CrsConfiguration getFull() {
        return new ImmutableCrsConfiguration.Builder()
                .enabled(false)
                .addAdditionalCrs(EpsgCrs.of(4326))
                .build()
    }

    @Override
    CrsConfiguration getMinimal() {
        return new ImmutableCrsConfiguration.Builder()
                .build()
    }

    @Override
    CrsConfiguration getMinimalFullMerged() {
        return getFull()
    }

    @Override
    CrsConfiguration getSimple() {
        return new ImmutableCrsConfiguration.Builder()
                .enabled(true)
                .build()
    }

    @Override
    CrsConfiguration getSimpleFullMerged() {
        return new ImmutableCrsConfiguration.Builder()
                .from(getFull())
                .enabled(true)
                .build()
    }

    @Override
    CrsConfiguration getCollection() {
        return new ImmutableCrsConfiguration.Builder()
                .addAdditionalCrs(EpsgCrs.of(4258))
                .build()
    }

    @Override
    CrsConfiguration getCollectionFullMerged() {
        return new ImmutableCrsConfiguration.Builder()
                .from(getFull())
                .additionalCrs(ImmutableList.of(
                        EpsgCrs.of(4326),
                        EpsgCrs.of(4258)
                ))
                .build()
    }
}
