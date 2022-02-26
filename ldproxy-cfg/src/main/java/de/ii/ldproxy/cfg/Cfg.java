package de.ii.ldproxy.cfg;

import de.ii.xtraplatform.store.domain.entities.EntityData;
import java.io.IOException;
import java.nio.file.Path;

public interface Cfg {

  Builders builder();

  <T extends EntityData> void writeEntity(T data, Path... patches) throws IOException;

  <T extends EntityData> void writeDefaults(T data, Path... defaults) throws IOException;
}
