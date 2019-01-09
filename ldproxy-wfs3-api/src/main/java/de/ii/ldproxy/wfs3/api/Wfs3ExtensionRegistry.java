/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
public interface Wfs3ExtensionRegistry {

    List<Wfs3Extension> getExtensions();

    List<Wfs3ConformanceClass> getConformanceClasses();

    Map<Wfs3MediaType, Wfs3OutputFormatExtension> getOutputFormats();

    List<Wfs3EndpointExtension> getEndpoints();

    List<Wfs3StartupTask> getStartupTasks();

    List<Wfs3ParameterExtension> getWfs3Parameters();

    <T extends Wfs3Extension> List<T> getExtensionsForType(Class<T> extensionType);
}
