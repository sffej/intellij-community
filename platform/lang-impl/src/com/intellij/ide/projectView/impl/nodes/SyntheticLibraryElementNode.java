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
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class SyntheticLibraryElementNode extends ProjectViewNode<SyntheticLibrary> {
  public SyntheticLibraryElementNode(@NotNull Project project, @NotNull SyntheticLibrary library, ViewSettings settings) {
    super(project, library, settings);
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    SyntheticLibrary library = getLibrary();
    return VfsUtilCore.isUnder(file, ContainerUtil.newHashSet(library.getSourceRoots()));
  }

  @NotNull
  @Override
  public Collection<AbstractTreeNode> getChildren() {
    List<AbstractTreeNode> children = new ArrayList<>();
    SyntheticLibrary library = getLibrary();
    Project project = getProject();
    if (project != null) {
      PsiManager psiManager = PsiManager.getInstance(project);
      for (VirtualFile file : library.getSourceRoots()) {
        if (!file.isValid()) continue;
        if (file.isDirectory()) {
          PsiDirectory psiDir = psiManager.findDirectory(file);
          if (psiDir != null) {
            children.add(new PsiDirectoryNode(project, psiDir, getSettings()));
          }
        }
        else {
          PsiFile psiFile = psiManager.findFile(file);
          if (psiFile != null) {
            children.add(new PsiFileNode(project, psiFile, getSettings()));
          }
        }
      }
    }
    return children;
  }

  @Override
  public String getName() {
    SyntheticLibrary library = getLibrary();
    return StringUtil.notNullize(library.getName());
  }

  @NotNull
  private SyntheticLibrary getLibrary() {
    return Objects.requireNonNull(getValue());
  }

  @Override
  protected void update(PresentationData presentation) {
    presentation.setPresentableText(getName());
    presentation.setIcon(AllIcons.Nodes.PpLibFolder);
  }
}
