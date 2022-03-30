package de.ii.ogcapi.foundation.domain;

import de.ii.xtraplatform.store.domain.entities.ChangingValue;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface ChangingItemCount extends ChangingValue<Long> {

  static ChangingItemCount of(long itemCount) {
    return new ImmutableChangingItemCount.Builder()
        .value(Objects.requireNonNullElse(itemCount, 0L))
        .build();
  }

  @Override
  default Optional<ChangingValue<Long>> updateWith(ChangingValue<Long> delta) {
    Long deltaCount = delta.getValue();

    //TODO
    /*if (deltaCount == 0) {
      return Optional.of(delta);
    }*/

    return Optional.of(ChangingItemCount.of(this.getValue() + deltaCount));
  }
}
