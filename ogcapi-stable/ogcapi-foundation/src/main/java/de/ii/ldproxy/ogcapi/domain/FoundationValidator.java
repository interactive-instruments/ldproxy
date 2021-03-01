/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

public class FoundationValidator {

    public static ImmutableValidationResult.Builder validateLinks(ImmutableValidationResult.Builder builder, List<Link> links, String path) {
        links.stream()
             .filter(link -> {
                 try {
                     new URL(link.getHref()).toURI();
                     return false;
                 } catch (URISyntaxException | MalformedURLException e) {
                     return true;
                 }
             })
             .forEach(link -> {
                 builder.addStrictErrors(MessageFormat.format("Link ''{0}'' in resource ''{1}'' is not a valid URI.", link.getHref(), path));
             });
        links.stream()
             .filter(link -> Objects.isNull(link.getRel()))
             .forEach(link -> {
                 builder.addStrictErrors(MessageFormat.format("Link ''{0}'' in resource ''{1}'' has no link relation type (attribute ''rel'').", link.getHref(), path));
             });
        return builder;
    }

    public static ImmutableValidationResult.Builder validateUri(ImmutableValidationResult.Builder builder, String uri, String path) {
         try {
             new URL(uri).toURI();
         } catch (URISyntaxException | MalformedURLException e) {
             builder.addStrictErrors(MessageFormat.format("Link ''{0}'' in resource ''{1}'' is not a valid URI.", uri, path));         }
        return builder;
    }
}
