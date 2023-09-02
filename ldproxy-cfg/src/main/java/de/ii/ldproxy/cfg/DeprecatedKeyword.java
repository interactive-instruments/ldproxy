/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.AbstractJsonValidator;
import com.networknt.schema.AbstractKeyword;
import com.networknt.schema.CustomErrorMessageType;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonValidator;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.ValidationMessage;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Set;

public class DeprecatedKeyword extends AbstractKeyword {

  public static final String KEYWORD = "deprecated";

  public static boolean isDeprecated(ValidationMessage vm) {
    return Objects.equals(vm.getCode(), KEYWORD);
  }

  public DeprecatedKeyword() {
    super(KEYWORD);
  }

  @Override
  public JsonValidator newValidator(
      String schemaPath,
      JsonNode schemaNode,
      JsonSchema parentSchema,
      ValidationContext validationContext) {
    boolean deprecated = schemaNode.asBoolean(false);

    return new AbstractJsonValidator() {
      @Override
      public Set<ValidationMessage> validate(JsonNode node, JsonNode rootNode, String at) {
        if (deprecated) {
          return Set.of(
              ValidationMessage.of(
                  getValue(),
                  CustomErrorMessageType.of(getValue()),
                  new MessageFormat("{0}: is deprecated and should be upgraded"),
                  at,
                  schemaPath));
        }

        return Set.of();
      }
    };
  }
}
