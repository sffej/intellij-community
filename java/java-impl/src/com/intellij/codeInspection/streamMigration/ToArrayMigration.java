/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.codeInspection.streamMigration;

import com.intellij.codeInspection.streamMigration.StreamApiMigrationInspection.CountingLoopSource;
import com.intellij.codeInspection.streamMigration.StreamApiMigrationInspection.InitializerUsageStatus;
import com.intellij.codeInspection.streamMigration.StreamApiMigrationInspection.MapOp;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.util.ObjectUtils.tryCast;

/**
 * @author Tagir Valeev
 */
public class ToArrayMigration extends BaseStreamApiMigration {
  protected ToArrayMigration() {
    super("toArray");
  }

  @Override
  PsiElement migrate(@NotNull Project project, @NotNull PsiStatement body, @NotNull TerminalBlock tb) {
    PsiLocalVariable arrayVariable = StreamApiMigrationInspection.extractArray(tb);
    if(arrayVariable == null) return null;
    PsiAssignmentExpression assignment = tb.getSingleExpression(PsiAssignmentExpression.class);
    if(assignment == null) return null;
    PsiExpression rValue = assignment.getRExpression();
    if(rValue == null) return null;
    PsiNewExpression initializer = tryCast(arrayVariable.getInitializer(), PsiNewExpression.class);
    if(initializer == null) return null;
    PsiExpression dimension = ArrayUtil.getFirstElement(initializer.getArrayDimensions());
    if(dimension == null) return null;
    CountingLoopSource loop = tb.getLastOperation(CountingLoopSource.class);
    if(loop == null) return null;
    PsiArrayType arrayType = tryCast(initializer.getType(), PsiArrayType.class);
    if(arrayType == null) return null;
    InitializerUsageStatus status = StreamApiMigrationInspection.getInitializerUsageStatus(arrayVariable, tb.getMainLoop());
    if(status == InitializerUsageStatus.UNKNOWN) return null;
    PsiType componentType = arrayType.getComponentType();
    String supplier;
    if(componentType instanceof PsiPrimitiveType || componentType.equalsToText(CommonClassNames.JAVA_LANG_OBJECT)) {
      supplier = "";
    } else {
      supplier = arrayType.getCanonicalText()+"::new";
    }
    MapOp mapping = new MapOp(rValue, tb.getVariable(), assignment.getType());
    String replacementText = loop.withBound(dimension).createReplacement() + mapping.createReplacement() + ".toArray(" + supplier + ")";
    return replaceInitializer(tb.getMainLoop(), arrayVariable, initializer, replacementText, status);
  }
}
