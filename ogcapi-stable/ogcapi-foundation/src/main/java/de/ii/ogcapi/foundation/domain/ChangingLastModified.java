package de.ii.ogcapi.foundation.domain;

import de.ii.xtraplatform.store.domain.entities.ChangingValue;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface ChangingLastModified extends ChangingValue<Instant> {

  static ChangingLastModified of(Instant lastModified) {
    return new ImmutableChangingLastModified.Builder()
        .value(Objects.requireNonNullElse(lastModified, Instant.MIN))
        .build();
  }

  @Override
  default Optional<ChangingValue<Instant>> updateWith(ChangingValue<Instant> delta) {
    Instant deltaInstant = delta.getValue();

    if (!this.getValue().isBefore(deltaInstant)) {
      return Optional.empty();
    }

    return Optional.of(delta);
  }
}
