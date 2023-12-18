/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.expressions;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.Scope;
import io.harness.expression.functors.ExpressionFunctor;
import io.harness.ngmigration.utils.CaseFormat;
import io.harness.ngmigration.utils.MigratorUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.util.Pair;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_MIGRATOR})
public class SecretMigratorFunctor implements ExpressionFunctor {
  private Map<String, String> nameToIdentifier;
  private CaseFormat caseFormat;

  private Scope scope;

  private static Pattern VAR_PATTERN = Pattern.compile(".*VAR.*VAR.*");

  public SecretMigratorFunctor(Map<String, String> nameToIdentifier, CaseFormat caseFormat, Scope scope) {
    this.caseFormat = caseFormat;
    this.scope = Scope.PROJECT;
    this.nameToIdentifier = new HashMap<>();
    if (scope != null) {
      this.scope = scope;
    }
    if (EmptyPredicate.isNotEmpty(nameToIdentifier)) {
      this.nameToIdentifier = nameToIdentifier;
    }
  }

  public Object getValue(String secretName) {
    String secretIdentifier;
    if (nameToIdentifier.containsKey(secretName)) {
      secretIdentifier = nameToIdentifier.get(secretName);
    } else if (VAR_PATTERN.matcher(secretName).matches()) {
      // Nested expressions, expression engine creates an intermediate value like VARfadsVARfedasf. In this case, we
      // want to keep it as it is so the Engine can decode it.
      secretIdentifier = MigratorUtility.getScopedIdentifier(scope, secretName);
    } else {
      if (secretName.contains("<+")) {
        Pair<Map<String, String>, String> placeholdersAndInputString = replaceExpressions(secretName);
        Map<String, String> placeholders = placeholdersAndInputString.getFirst();
        secretIdentifier =
            MigratorUtility.getIdentifierWithScope(this.scope, placeholdersAndInputString.getSecond(), caseFormat);
        secretIdentifier = replacePlaceholders(secretIdentifier, placeholders);
      } else {
        // Default value
        secretIdentifier = MigratorUtility.getIdentifierWithScope(this.scope, secretName, caseFormat);
      }
    }
    return "<+secrets.getValue(\"" + secretIdentifier + "\")>";
  }

  private static Pair<Map<String, String>, String> replaceExpressions(String inputString) {
    Map<String, String> placeholders = new HashMap<>();
    Pattern pattern = Pattern.compile("<\\+([^>]+)>");
    Matcher matcher = pattern.matcher(inputString);

    int count = 1;
    while (matcher.find()) {
      String placeholder = "harnessplaceholder" + count;
      String expression = matcher.group(0);
      placeholders.put(placeholder, expression);
      inputString = inputString.replace(expression, placeholder);
      count++;
    }
    return Pair.of(placeholders, inputString);
  }

  private static String replacePlaceholders(String inputString, Map<String, String> placeholders) {
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      Pattern pattern = Pattern.compile(Pattern.quote(entry.getKey()), Pattern.CASE_INSENSITIVE);
      inputString = pattern.matcher(inputString).replaceAll(entry.getValue());
    }
    return inputString;
  }
}