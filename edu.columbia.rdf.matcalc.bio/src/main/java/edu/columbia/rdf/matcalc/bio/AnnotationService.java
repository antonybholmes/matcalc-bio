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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jebtk.bioinformatics.genomic.GFF3Parser;
import org.jebtk.bioinformatics.genomic.GenesDB;
import org.jebtk.bioinformatics.genomic.Genome;
import org.jebtk.bioinformatics.genomic.GenomicType;
import org.jebtk.core.collections.DefaultHashMap;
import org.jebtk.core.collections.DefaultHashMapCreator;
import org.jebtk.core.collections.DefaultTreeMapCreator;
import org.jebtk.core.collections.HashMapCreator;
import org.jebtk.core.collections.IterMap;
import org.jebtk.core.collections.IterTreeMap;
import org.jebtk.core.collections.TreeMapCreator;
import org.jebtk.core.io.FileUtils;
import org.jebtk.core.io.PathUtils;
import org.jebtk.core.sys.SysUtils;
import org.jebtk.core.text.TextUtils;
import org.jebtk.core.tree.CheckTreeNode;
import org.jebtk.core.tree.TreeNode;
import org.jebtk.core.tree.TreeRootNode;
import org.jebtk.modern.tree.ModernCheckTree;
import org.jebtk.modern.tree.ModernCheckTreeMode;

/**
 * Service for extracting DNA from sequences.
 *
 * @author Antony Holmes
 */
public class AnnotationService implements Iterable<Genome> {

  private static class AnnotationServiceLoader {
    private static final AnnotationService INSTANCE = new AnnotationService();
  }

  public static AnnotationService getInstance() {
    return AnnotationServiceLoader.INSTANCE;
  }

  private static final Path RES_DIR = PathUtils
      .getPath("res/modules/gene_annotation/genomes");

  //private Map<String, Set<String>> mGenomeMap = DefaultTreeMap
  //    .create(new DefaultTreeSetCreator<String>());

  private Map<Genome, Path> mFileMap = 
      new IterTreeMap<Genome, Path>();
  
  private IterMap<String, IterMap<String, IterMap<String, Genome>>> mGenomeMap = DefaultHashMap
      .create(
          new DefaultTreeMapCreator<String, IterMap<String, Genome>>(new TreeMapCreator<String, Genome>()));

  // private Map<String, Map<Integer, Map<Integer,
  // FixedGapSearch<AnnotationGene>>>> mFixedGapSearchMap =
  // DefaultHashMap.create(new DefaultHashMapCreator<Integer, Map<Integer,
  // FixedGapSearch<AnnotationGene>>>(new HashMapCreator<Integer,
  // FixedGapSearch<AnnotationGene>>()));

  private IterMap<Genome, IterMap<Integer, IterMap<Integer, GenesDB>>> mSearchMap = DefaultHashMap
      .create(
          new DefaultHashMapCreator<Integer, IterMap<Integer, GenesDB>>(
              new HashMapCreator<Integer, GenesDB>()));

//  private IterMap<Genome, IterMap<Integer, IterMap<Integer, BinarySearch<AnnotationGene>>>> mBinarySearchMap = DefaultHashMap
//      .create(
//          new DefaultHashMapCreator<Integer, IterMap<Integer, BinarySearch<AnnotationGene>>>(
//              new HashMapCreator<Integer, BinarySearch<AnnotationGene>>()));

  private Map<Genome, IterMap<GenomicType, List<String>>> mGeneIdMap = 
      DefaultHashMap.create(new HashMapCreator<GenomicType, List<String>>());

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

  //public List<String> getGeneIdTypes(GenomeDatabase gb) throws IOException {
  //  return getGeneIdTypes(gb.getGenome(), gb.getDb());
  //}
  
  /**
   * Returns the names of the ids (e.g. entrez_id, refseq_id) associated with
   * a gene entity that can used to label it.
   * @param genome
   * @param type
   * @return
   * @throws IOException
   */
  public List<String> getGeneIdTypes(Genome genome, GenomicType type) throws IOException {
    autoLoad();

    //String id = genome.getAssembly() + name;
    
    if (!mFileMap.containsKey(genome)) {
      return null;
    }

    if (!mGeneIdMap.get(genome).containsKey(type)) {
      Path file = mFileMap.get(genome);
      
      SysUtils.err().println("creating types for " + file, type);

      if (PathUtils.getName(file).toLowerCase().contains("gff3")) {
        mGeneIdMap.get(genome).put(type, GFF3Parser.gff3IdTypes(file, type));
      }// else {
       // mGeneIdMap.put(genome, AnnotationGene.geneIdTypes(file));
      //}
    }

    return mGeneIdMap.get(genome).get(type);
  }
  
  //public FixedGapSearch<AnnotationGene> getSearch(GenomeDatabase gb) throws IOException {
  //  return getSearch(gb.getGenome(), gb.getDb());
  //}

  public GenesDB getSearch(Genome genome)
      throws IOException {
    return getSearch(genome, 0, 0);
  }

  public GenesDB getSearch(Genome genome,
      int ext5p,
      int ext3p) throws IOException {
    autoLoad();

    //String id = genome.getAssembly() + name;

    //System.err.println("Looking for " + genome + " df " + mFileMap.keySet());
    
    if (!mFileMap.containsKey(genome)) {
      return null;
    }

    // We have a valid assembly name so load it
    if (!mSearchMap.containsKey(genome)
        || !mSearchMap.get(genome).containsKey(ext5p)
        || !mSearchMap.get(genome).get(ext5p).containsKey(ext3p)) {
      Path file = mFileMap.get(genome);

      // System.err.println("blob " + name + " " +
      // mSearchMap.get(name).containsKey(ext5p));

      GenesDB assembly = null;

      if (PathUtils.getName(file).toLowerCase().contains("gff3")) {
        System.err.println("Creating assembly from " + file);
        
        assembly = new GFF3Parser().parse(file, genome);
        
        
        
        //assembly = AnnotationGene.parseGFF3(genome, file, ext5p, ext3p);
      } //else {
        //assembly = AnnotationGene.parseTssForSearch(file, ext5p, ext3p);
      //}

      if (assembly != null) {
        mSearchMap.get(genome).get(ext5p).put(ext3p, assembly);
      }
    }

    return mSearchMap.get(genome).get(ext5p).get(ext3p);
  }
  
//  //public BinarySearch<AnnotationGene> getBinarySearch(GenomeDatabase genome) throws IOException {
//  //  return getBinarySearch(genome.getGenome(), genome.getDb());
//  //}
//
//  //public BinarySearch<AnnotationGene> getBinarySearch(Genome genome, String name)
//   //   throws IOException {
//  //  return getBinarySearch(genome, name, 0, 0);
//  //}
////
////  public BinarySearch<AnnotationGene> getBinarySearch(Genome genome,
////      int ext5p,
////      int ext3p) throws IOException {
////    autoLoad();
////
////    //String id = genome.getAssembly() + name;
////    
////    if (!mFileMap.containsKey(genome)) {
////      return null;
////    }
////
////    // We have a valid assembly name so load it
////    if (!mBinarySearchMap.containsKey(genome)
////        || !mBinarySearchMap.get(genome).containsKey(ext5p)
////        || !mBinarySearchMap.get(genome).get(ext5p).containsKey(ext3p)) {
////      Path file = mFileMap.get(genome);
////
////      // System.err.println("blob " + name + " " +
////      // mSearchMap.get(name).containsKey(ext5p));
////
////      BinarySearch<AnnotationGene> assembly = null;
////
////      if (PathUtils.getName(file).toLowerCase().contains("gff3")) {
////        assembly = AnnotationGene.parseGFF3Binary(genome, file, ext5p, ext3p);
////      } else {
////        assembly = AnnotationGene
////            .parseTssForBinarySearch(file, ext5p, ext3p);
////      }
////
////      mBinarySearchMap.get(genome).get(ext5p).put(ext3p, assembly);
////    }
////
////    return mBinarySearchMap.get(genome).get(ext5p).get(ext3p);
////  }

  /**
   * Load a list of files
   * 
   * @throws IOException
   */
  private void autoLoad() throws IOException {
    if (mAutoLoad) {
      mAutoLoad = false;
      
      Deque<Path> stack = new ArrayDeque<Path>();

      stack.push(RES_DIR);

      while (!stack.isEmpty()) {
        Path file = stack.pop();

        if (FileUtils.exists(file)) {
          if (FileUtils.isFile(file)) {
            String f = PathUtils.getName(file);

            if (f.contains("gff3") || f.contains("txt")) {
              String name = PathUtils.getName(file.getParent().getParent());
              
              String build = PathUtils.getName(file.getParent());
              
              // Remove extension
              String track = f.replaceFirst("\\..+", TextUtils.EMPTY_STRING);

              Genome genome = new Genome(name, build, track);
              
              //String id = genome + name;
              
              mFileMap.put(genome, file);
              
              
              mGenomeMap.get(genome.getName()).get(genome.getAssembly()).put(genome.getTrack(), genome);

              //String genome = file.getParent().getFileName().toString();

              //mGenomeMap.get(genome).add(name);
            }
          } else {
            List<Path> files = FileUtils.ls(file);

            for (Path f : files) {
              stack.push(f);
            }
          }
        }
      }
    }
  }

  public Iterable<Genome> genomes() throws IOException {
    autoLoad();

    return mFileMap.keySet();
  }
  
//  public Iterable<String> names() throws IOException {
//    autoLoad();
//
//    return mGenomeMap.keySet();
//  }
//  
//  public Iterable<String> assemblies(String name) throws IOException {
//    autoLoad();
//    
//    return mGenomeMap.get(name).keySet();
//  }
//  
//  public Iterable<String> tracks(String name, String assembly) throws IOException {
//    autoLoad();
//    
//    return mGenomeMap.get(name).get(assembly).keySet();
//  }
  
//  public Genome genome(String name, String assembly, String track) throws IOException {
//    autoLoad();
//    
//    return mGenomeMap.get(name).get(assembly).get(track);
//  }

  //public Iterable<String> annotations(String genome) throws IOException {
  //  autoLoad();

  //  return mGenomeMap.get(genome);
  //}

  public ModernCheckTree<String> createTree() throws IOException {
    return createTree(ModernCheckTreeMode.MULTI);
  }

  public ModernCheckTree<String> createTree(ModernCheckTreeMode mode)
      throws IOException {
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

      for (Path file : files) {

        TreeNode<String> n = null;

        String f = PathUtils.getName(file);
        
        if (FileUtils.isFile(file)) {
          
          
          if (f.contains("gff3") || f.contains("txt")) {
            // Remove extension
            String name = PathUtils.getName(file.getParent().getParent());
            
            String build = PathUtils.getName(file.getParent());
            
            // Remove extension
            String track = f.replaceFirst("\\..+", TextUtils.EMPTY_STRING);

            Genome genome = new Genome(name, build, track);
            
            n = new CheckTreeNode<String>(genome.toString());
          }
        } else {
          n = new TreeNode<String>(f);
          treeStack.push(n);
          stack.push(file);
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
  public Iterator<Genome> iterator() {
    try {
      autoLoad();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return mFileMap.keySet().iterator();
  }

 

}