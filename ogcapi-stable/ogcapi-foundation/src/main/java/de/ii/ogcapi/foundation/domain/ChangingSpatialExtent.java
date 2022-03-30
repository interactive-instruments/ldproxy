package de.ii.ogcapi.foundation.domain;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.store.domain.entities.ChangingValue;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface ChangingSpatialExtent extends ChangingValue<BoundingBox> {

  static ChangingSpatialExtent of(BoundingBox bbox) {
    return new ImmutableChangingSpatialExtent.Builder()
        .value(Objects.requireNonNullElse(bbox, BoundingBox.of(-180, -90, 180, 90, OgcCrs.CRS84)))
        .build();
  }

  @Override
  default Optional<ChangingValue<BoundingBox>> updateWith(ChangingValue<BoundingBox> delta) {
    BoundingBox deltaExtent = delta.getValue();
    if (this.getValue().getEpsgCrs().getCode() != deltaExtent.getEpsgCrs().getCode()
        || this.getValue().getEpsgCrs().getForceAxisOrder() != deltaExtent.getEpsgCrs().getForceAxisOrder())
      throw new IllegalStateException("Both bounding boxes must have the same CRS.");

    if (getValue().getXmin() <= deltaExtent.getXmin() && getValue().getYmin() <= deltaExtent.getYmin()
        && getValue().getXmax() >= deltaExtent.getXmax() && getValue().getYmax() >= deltaExtent.getYmax())
      return Optional.empty();

    return Optional.of(ChangingSpatialExtent.of(BoundingBox.of(Math.min(getValue().getXmin(), deltaExtent.getXmin()),
        Math.min(getValue().getYmin(), deltaExtent.getYmin()),
        Math.max(getValue().getXmax(), deltaExtent.getXmax()),
        Math.max(getValue().getYmax(), deltaExtent.getYmax()),
        OgcCrs.CRS84)));
  }
}
