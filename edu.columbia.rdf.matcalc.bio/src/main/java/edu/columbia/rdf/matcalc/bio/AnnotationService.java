/**
 * Copyright (C) 2016, Antony Holmes
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. Neither the name of copyright holder nor the names of its contributors 
 *     may be used to endorse or promote products derived from this software 
 *     without specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package edu.columbia.rdf.matcalc.bio;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jebtk.bioinformatics.gapsearch.BinarySearch;
import org.jebtk.bioinformatics.gapsearch.FixedGapSearch;
import org.jebtk.bioinformatics.genomic.GTB2Parser;
import org.jebtk.core.collections.DefaultHashMap;
import org.jebtk.core.collections.DefaultHashMapCreator;
import org.jebtk.core.collections.DefaultTreeMap;
import org.jebtk.core.collections.DefaultTreeSetCreator;
import org.jebtk.core.collections.HashMapCreator;
import org.jebtk.core.collections.IterMap;
import org.jebtk.core.io.FileUtils;
import org.jebtk.core.io.PathUtils;
import org.jebtk.core.text.TextUtils;
import org.jebtk.core.tree.CheckTreeNode;
import org.jebtk.core.tree.TreeNode;
import org.jebtk.core.tree.TreeRootNode;
import org.jebtk.modern.tree.ModernCheckTree;
import org.jebtk.modern.tree.ModernCheckTreeMode;

/**
 * Service for extracting DNA from sequences.
 *
 * @author Antony Holmes Holmes
 */
public class AnnotationService implements Iterable<String> {

  private static class AnnotationServiceLoader {
    private static final AnnotationService INSTANCE = new AnnotationService();
  }

  public static AnnotationService getInstance() {
    return AnnotationServiceLoader.INSTANCE;
  }

  private static final Path RES_DIR = PathUtils.getPath("res/modules/gene_annotation/genomes");

  private Map<String, Set<String>> mGenomeMap = DefaultTreeMap.create(new DefaultTreeSetCreator<String>());

  private Map<String, Path> mFileMap = new TreeMap<String, Path>();

  // private Map<String, Map<Integer, Map<Integer,
  // FixedGapSearch<AnnotationGene>>>> mFixedGapSearchMap =
  // DefaultHashMap.create(new DefaultHashMapCreator<Integer, Map<Integer,
  // FixedGapSearch<AnnotationGene>>>(new HashMapCreator<Integer,
  // FixedGapSearch<AnnotationGene>>()));

  private IterMap<String, IterMap<Integer, IterMap<Integer, FixedGapSearch<AnnotationGene>>>> mSearchMap = DefaultHashMap
      .create(new DefaultHashMapCreator<Integer, IterMap<Integer, FixedGapSearch<AnnotationGene>>>(
          new HashMapCreator<Integer, FixedGapSearch<AnnotationGene>>()));

  private IterMap<String, IterMap<Integer, IterMap<Integer, BinarySearch<AnnotationGene>>>> mBinarySearchMap = DefaultHashMap
      .create(new DefaultHashMapCreator<Integer, IterMap<Integer, BinarySearch<AnnotationGene>>>(
          new HashMapCreator<Integer, BinarySearch<AnnotationGene>>()));

  private Map<String, List<String>> mGeneIdMap = new HashMap<String, List<String>>();

  private boolean mAutoLoad = true;

  /*
   * public FixedGapSearch<AnnotationGene> getFixedGapSearch(String name, int
   * ext5p, int ext3p) throws IOException { autoLoad();
   * 
   * if (!mFileMap.containsKey(name)) { return null; }
   * 
   * // We have a valid assembly name so load it if
   * (!mFixedGapSearchMap.containsKey(name) ||
   * mFixedGapSearchMap.get(name).containsKey(ext5p) ||
   * mFixedGapSearchMap.get(name).get(ext5p).containsKey(ext3p)) { Path file =
   * mFileMap.get(name);
   * 
   * FixedGapSearch<AnnotationGene> assembly =
   * AnnotationGene.parseTssForFixedGappedSearch(file, ext5p, ext3p);
   * 
   * mFixedGapSearchMap.get(name).get(ext5p).put(ext3p, assembly); }
   * 
   * return mFixedGapSearchMap.get(name).get(ext5p).get(ext3p); }
   */

  public List<String> getGeneIdTypes(String genome) throws IOException {
    autoLoad();

    if (!mFileMap.containsKey(genome)) {
      return null;
    }

    if (!mGeneIdMap.containsKey(genome)) {
      Path file = mFileMap.get(genome);

      if (PathUtils.getName(file).toLowerCase().contains("gff3")) {
        mGeneIdMap.put(genome, AnnotationGene.gff3IdTypes(file));
      } else {
        mGeneIdMap.put(genome, AnnotationGene.geneIdTypes(file));
      }
    }

    return mGeneIdMap.get(genome);
  }

  public FixedGapSearch<AnnotationGene> getSearch(String name) throws IOException {
    return getSearch(name, 0, 0);
  }

  public FixedGapSearch<AnnotationGene> getSearch(String name, int ext5p, int ext3p) throws IOException {
    autoLoad();

    if (!mFileMap.containsKey(name)) {
      return null;
    }

    // We have a valid assembly name so load it
    if (!mSearchMap.containsKey(name) || !mSearchMap.get(name).containsKey(ext5p)
        || !mSearchMap.get(name).get(ext5p).containsKey(ext3p)) {
      Path file = mFileMap.get(name);

      // System.err.println("blob " + name + " " +
      // mSearchMap.get(name).containsKey(ext5p));

      FixedGapSearch<AnnotationGene> assembly = null;

      if (PathUtils.getName(file).toLowerCase().contains("gff3")) {
        assembly = AnnotationGene.parseGFF3(name, file, ext5p, ext3p);
      } else {
        assembly = AnnotationGene.parseTssForSearch(name, file, ext5p, ext3p);
      }

      mSearchMap.get(name).get(ext5p).put(ext3p, assembly);
    }

    return mSearchMap.get(name).get(ext5p).get(ext3p);
  }

  public BinarySearch<AnnotationGene> getBinarySearch(String name) throws IOException {
    return getBinarySearch(name, 0, 0);
  }

  public BinarySearch<AnnotationGene> getBinarySearch(String name, int ext5p, int ext3p) throws IOException {
    autoLoad();

    if (!mFileMap.containsKey(name)) {
      return null;
    }

    // We have a valid assembly name so load it
    if (!mBinarySearchMap.containsKey(name) || !mBinarySearchMap.get(name).containsKey(ext5p)
        || !mBinarySearchMap.get(name).get(ext5p).containsKey(ext3p)) {
      Path file = mFileMap.get(name);

      // System.err.println("blob " + name + " " +
      // mSearchMap.get(name).containsKey(ext5p));

      BinarySearch<AnnotationGene> assembly = null;

      if (PathUtils.getName(file).toLowerCase().contains("gff3")) {
        assembly = AnnotationGene.parseGFF3Binary(name, file, ext5p, ext3p);
      } else {
        assembly = AnnotationGene.parseTssForBinarySearch(name, file, ext5p, ext3p);
      }

      mBinarySearchMap.get(name).get(ext5p).put(ext3p, assembly);
    }

    return mBinarySearchMap.get(name).get(ext5p).get(ext3p);
  }

  /**
   * Load a list of files
   * 
   * @throws IOException
   */
  private void autoLoad() throws IOException {
    if (mAutoLoad) {
      Deque<Path> stack = new ArrayDeque<Path>();

      stack.push(RES_DIR);

      while (!stack.isEmpty()) {
        Path file = stack.pop();

        if (FileUtils.exists(file)) {
          if (FileUtils.isFile(file)) {
            String name = PathUtils.getName(file);

            if (name.contains("gff3") || name.contains("txt")) {
              // Remove extension
              name = name.replaceFirst("\\..+", TextUtils.EMPTY_STRING);

              mFileMap.put(name, file);

              String genome = file.getParent().getFileName().toString();

              mGenomeMap.get(genome).add(name);
            }
          } else {
            List<Path> files = FileUtils.ls(file);

            for (Path f : files) {
              stack.push(f);
            }
          }
        }
      }

      mAutoLoad = false;
    }
  }

  public Iterable<String> genomes() throws IOException {
    autoLoad();

    return mGenomeMap.keySet();
  }

  public Iterable<String> annotations(String genome) throws IOException {
    autoLoad();

    return mGenomeMap.get(genome);
  }

  public ModernCheckTree<String> createTree() throws IOException {
    return createTree(ModernCheckTreeMode.MULTI);
  }

  public ModernCheckTree<String> createTree(ModernCheckTreeMode mode) throws IOException {
    ModernCheckTree<String> tree = new ModernCheckTree<String>(mode);

    Deque<Path> stack = new ArrayDeque<Path>();
    stack.push(RES_DIR);

    TreeRootNode<String> root = new TreeRootNode<String>();

    Deque<TreeNode<String>> treeStack = new ArrayDeque<TreeNode<String>>();
    treeStack.push(root);

    while (!stack.isEmpty()) {
      Path dir = stack.pop();
      TreeNode<String> node = treeStack.pop();

      // System.err.println("annotation dir " + dir);

      List<Path> files = FileUtils.ls(dir);

      for (Path f : files) {

        String name = PathUtils.getName(f);

        TreeNode<String> n = null;

        if (FileUtils.isFile(f)) {
          if (name.contains("gff3") || name.contains("txt")) {
            // Remove extension
            name = name.replaceFirst("\\..+", "");
            n = new CheckTreeNode<String>(name);
          }
        } else {
          n = new TreeNode<String>(name);
          treeStack.push(n);
          stack.push(f);
        }

        if (n != null) {
          node.addChild(n);
        }
      }
    }

    tree.setRoot(root);

    tree.setChildrenAreExpanded(true, true);

    return tree;
  }

  @Override
  public Iterator<String> iterator() {
    try {
      autoLoad();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return mFileMap.keySet().iterator();
  }

}