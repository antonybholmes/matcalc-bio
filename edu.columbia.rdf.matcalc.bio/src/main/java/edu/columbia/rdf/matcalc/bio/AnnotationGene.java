package edu.columbia.rdf.matcalc.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.jebtk.bioinformatics.gapsearch.BinarySearch;
import org.jebtk.bioinformatics.gapsearch.FixedGapSearch;
import org.jebtk.bioinformatics.gapsearch.GapSearch;
import org.jebtk.bioinformatics.genomic.Chromosome;
import org.jebtk.bioinformatics.genomic.GenomeService;
import org.jebtk.bioinformatics.genomic.GFF3Parser;
import org.jebtk.bioinformatics.genomic.GenomicRegion;
import org.jebtk.bioinformatics.genomic.Strand;
import org.jebtk.core.TextIdProperty;
import org.jebtk.core.collections.ArrayListCreator;
import org.jebtk.core.collections.CollectionUtils;
import org.jebtk.core.collections.DefaultHashMap;
import org.jebtk.core.collections.DefaultHashMapCreator;
import org.jebtk.core.collections.IterMap;
import org.jebtk.core.io.FileUtils;
import org.jebtk.core.io.Io;
import org.jebtk.core.text.Join;
import org.jebtk.core.text.Splitter;
import org.jebtk.core.text.TextUtils;

public class AnnotationGene extends GenomicRegion implements TextIdProperty {
  private static final int SYMBOL_COL = 0;
  private static final int CHR_COL = 1;
  private static final int STRAND_COL = 2;
  private static final int START_COL = 3;
  private static final int END_COL = 4;
  private static final int EXON_COUNT_COL = 5;
  private static final int EXON_START_COL = 6;
  private static final int EXON_END_COL = 7;
  private static final int ALT_COL = 8;

  private Strand mStrand;

  private List<GenomicRegion> mExons = new ArrayList<GenomicRegion>();

  private Map<String, String> mAltMap = new HashMap<String, String>();

  private static final Pattern SYMBOL_REGEX = Pattern
      .compile("gene_name=\"(.+?)\"");

  private static final Pattern TRANSCRIPT_REGEX = Pattern
      .compile("transcript_id=\"(.+?)\"");

  private GenomicRegion mTss = null;
  private String mId = null;
  private String mText = null;
  private String mSymbolType = null;
  private String mEntrezType = null;
  private String mRefSeqType = null;

  public AnnotationGene(String id, Strand strand, GenomicRegion region) {
    super(region);

    mId = id;
    mStrand = strand;

    if (mStrand == Strand.SENSE) {
      mTss = new GenomicRegion(region.getChr(), region.getStart(),
          region.getStart());
    } else {
      mTss = new GenomicRegion(region.getChr(), region.getEnd(),
          region.getEnd());
    }
  }

  public Strand getStrand() {
    return mStrand;
  }

  @Override
  public String getId() {
    return mId;
  }

  public String getSymbol() {
    if (mSymbolType == null) {
      mSymbolType = CollectionUtils.contains(mAltMap,
          "Gene Name",
          "gene_name",
          "gene_symbol",
          "symbol",
          "Symbol");
    }

    return getAltName(mSymbolType);
  }

  public List<GenomicRegion> getExons() {
    return mExons;
  }

  public GenomicRegion getTss() {
    return mTss;
  }

  public void addAltName(String type, String name) {
    mAltMap.put(type, name);

    mText = mId + " " + TextUtils
        .parenthesis(Join.onComma().values(CollectionUtils.toList(mAltMap)));
  }

  public Collection<String> getAltTypes() {
    return mAltMap.keySet();
  }

  /**
   * Genes can have multiple alternative names/ids associated with them
   * depending on the database.
   * 
   * @param type
   * @return
   */
  public String getAltName(String type) {
    if (mAltMap.containsKey(type)) {
      return mAltMap.get(type);
    } else {
      return TextUtils.NA;
    }
  }

  @Override
  public String toString() {
    return mText;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof AnnotationGene) {
      return compareTo((AnnotationGene) o) == 0;
    } else {
      return false;
    }
  }

  /**
   * Returns the TSS of the gene accounting for the strand.
   * 
   * @param gene
   * @return
   */
  public static int getTss(AnnotationGene gene) {
    if (gene.mStrand == Strand.SENSE) {
      return gene.getStart();
    } else {
      return gene.getEnd();
    }
  }

  /**
   * Get the distance from the mid point of a region to a gene accounting for
   * the strand.
   * 
   * @param gene
   * @param region
   * @return
   */
  public static int getTssMidDist(AnnotationGene gene, GenomicRegion region) {
    int mid = GenomicRegion.mid(region);

    return getTssMidDist(gene, mid);
  }

  /**
   * Returns the distance of the mid to the gene tss. If the mid is downstream,
   * the value is positive.
   * 
   * @param gene
   * @param mid
   * @return
   */
  public static int getTssMidDist(AnnotationGene gene, int mid) {
    if (gene.mStrand == Strand.SENSE) {
      return mid - gene.getStart();
    } else {
      return gene.getEnd() - mid;
    }
  }

  public String getEntrez() {
    if (mEntrezType == null) {
      mEntrezType = CollectionUtils
          .contains(mAltMap, "Entrez Id", "entrez_id", "entrez", "Entrez");
    }

    return getAltName(mEntrezType);
  }

  public String getRefSeq() {
    if (mRefSeqType == null) {
      mRefSeqType = CollectionUtils.contains(mAltMap,
          "Transcript Id",
          "transcript_id",
          "RefSeq Id",
          "refseq_id",
          "RefSeq");
    }

    return getAltName(mRefSeqType);
  }

  /**
   * Test for multiple alternative names returning the first found or n/a if
   * nothing matches.
   * 
   * @param names
   * @return
   */
  private String getAltName(String... names) {
    for (String name : names) {
      String ret = getAltName(name);

      if (ret != null) {
        return ret;
      }
    }

    return TextUtils.NA;
  }

  /*
   * public static FixedGapSearch<AnnotationGene>
   * parseTssForFixedGappedSearch(Path file, int ext5p, int ext3p) throws
   * IOException {
   * 
   * FixedGapSearch<AnnotationGene> search = new
   * FixedGapSearch<AnnotationGene>(10000);
   * 
   * parseTssForSearch(file, ext5p, ext3p, search);
   * 
   * return search; }
   */

  public static FixedGapSearch<AnnotationGene> parseTssForSearch(String name,
      Path file,
      int ext5p,
      int ext3p) throws IOException {
    FixedGapSearch<AnnotationGene> search = new FixedGapSearch<AnnotationGene>();

    parseTssForSearch(file, ext5p, ext3p, search);

    return search;
  }

  public static BinarySearch<AnnotationGene> parseTssForBinarySearch(
      String name,
      Path file,
      int ext5p,
      int ext3p) throws IOException {

    BinarySearch<AnnotationGene> search = new BinarySearch<AnnotationGene>();

    parseTssForSearch(file, ext5p, ext3p, search);

    return search;
  }

  public static void parseTssForSearch(Path file,
      int ext5p,
      int ext3p,
      GapSearch<AnnotationGene> gappedSearch) throws IOException {

    String line;
    List<String> tokens;

    Splitter splitter = Splitter.onTab();
    Splitter exonSplitter = Splitter.on(TextUtils.SEMI_COLON_DELIMITER);

    BufferedReader reader = FileUtils.newBufferedReader(file);

    try {

      List<String> header = splitter.text(reader.readLine());

      List<String> altNames = CollectionUtils
          .subList(header, ALT_COL, header.size() - ALT_COL);

      while ((line = reader.readLine()) != null) {
        if (Io.isEmptyLine(line)) {
          continue;
        }

        tokens = splitter.text(line);

        String symbol = tokens.get(SYMBOL_COL);

        Chromosome chr = GenomeService.getInstance()
            .chr(GenomeService.getInstance().guessGenome(file), tokens.get(CHR_COL));
        Strand strand = Strand.parse(tokens.get(STRAND_COL));

        // UCSC convention
        int start = Integer.parseInt(tokens.get(START_COL)) + 1;
        int end = Integer.parseInt(tokens.get(END_COL));
        int exonCount = Integer.parseInt(tokens.get(EXON_COUNT_COL));

        List<Integer> exonStarts = TextUtils
            .toInt(exonSplitter.text(tokens.get(EXON_START_COL)));

        List<Integer> exonEnds = TextUtils
            .toInt(exonSplitter.text(tokens.get(EXON_END_COL)));

        GenomicRegion region = new GenomicRegion(chr, start, end);

        AnnotationGene gene = new AnnotationGene(symbol, strand, region);

        // System.err.println("line " + line);

        for (int i = 0; i < exonCount; ++i) {
          // UCSC convention
          GenomicRegion exon = new GenomicRegion(chr, exonStarts.get(i) + 1,
              exonEnds.get(i));

          gene.getExons().add(exon);
        }

        // Add the alternative ids for this gene

        for (int i = 0; i < altNames.size(); ++i) {
          String alt = tokens.get(ALT_COL + i);

          if (alt != null && !alt.equals(TextUtils.NA)) {
            gene.addAltName(altNames.get(i), alt);
          }
        }

        // extend so we can find elements in the promoter
        if (strand == Strand.SENSE) {
          start -= ext5p;
        } else {
          end += ext3p;
        }

        gappedSearch.add(GenomicRegion.create(region.getChr(), start, end),
            gene);
      }

    } finally {
      reader.close();
    }
  }

  public static FixedGapSearch<AnnotationGene> parseGFF3(String genome,
      String name,
      Path file,
      int ext5p,
      int ext3p) throws IOException {

    FixedGapSearch<AnnotationGene> search = new FixedGapSearch<AnnotationGene>();

    parseGFF3(genome, file, ext5p, ext3p, search);

    return search;
  }

  public static BinarySearch<AnnotationGene> parseGFF3Binary(String genome,
      String name,
      Path file,
      int ext5p,
      int ext3p) throws IOException {

    BinarySearch<AnnotationGene> search = new BinarySearch<AnnotationGene>();

    parseGFF3(genome, file, ext5p, ext3p, search);

    return search;
  }

  public static void parseGFF3(String genome,
      Path file,
      int ext5p,
      int ext3p,
      GapSearch<AnnotationGene> gappedSearch) throws IOException {

    String line;
    List<String> tokens;

    Splitter splitter = Splitter.onTab();

    IterMap<String, IterMap<Chromosome, List<GenomicRegion>>> exonMap = DefaultHashMap
        .create(new DefaultHashMapCreator<Chromosome, List<GenomicRegion>>(
            new ArrayListCreator<GenomicRegion>()));

    Map<String, String> symbolMap = new HashMap<String, String>();

    Map<String, Map<String, String>> attributeMap = new HashMap<String, Map<String, String>>();

    BufferedReader reader = FileUtils.newBufferedReader(file);

    try {
      while ((line = reader.readLine()) != null) {
        if (Io.isEmptyLine(line)) {
          continue;
        }

        tokens = splitter.text(line);

        String type = tokens.get(2);

        // Skip non exons
        if (!type.equals("exon")) {
          continue;
        }

        Map<String, String> attributes = GFF3Parser
            .parseGFF3Attributes(tokens.get(8));

        String symbol = attributes.get("gene_name");

        if (symbol == null) {
          continue;
        }

        String transcript = attributes.get("transcript_id");

        if (transcript == null) {
          continue;
        }

        Chromosome chr = GenomeService.getInstance().chr(genome, tokens.get(0));
        Strand strand = Strand.parse(tokens.get(6));

        // UCSC convention
        int start = Integer.parseInt(tokens.get(3));
        int end = Integer.parseInt(tokens.get(4));

        GenomicRegion region = new GenomicRegion(chr, start, end, strand);

        exonMap.get(transcript).get(chr).add(region);

        symbolMap.put(transcript, symbol);

        if (!attributeMap.containsKey(symbol)) {
          attributeMap.put(transcript, attributes);
        }
      }
    } finally {
      reader.close();
    }

    for (Entry<String, IterMap<Chromosome, List<GenomicRegion>>> em : exonMap) {
      String transcript = em.getKey();
      
      for (Entry<Chromosome, List<GenomicRegion>> cm : em.getValue()) {
        Chromosome chr = cm.getKey();
        
        int start = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;

        List<GenomicRegion> regions = exonMap.get(transcript).get(chr);

        for (GenomicRegion r : regions) {
          start = Math.min(start, r.getStart());
          end = Math.max(end, r.getEnd());
        }

        GenomicRegion exon = regions.get(0);

        // Chromosome chr = exon.getChr();

        // Use the first exon to get the strand
        Strand strand = exon.getStrand();

        String symbol = symbolMap.get(transcript);

        AnnotationGene gene = new AnnotationGene(symbol, strand,
            new GenomicRegion(chr, start, end));

        for (GenomicRegion r : regions) {
          gene.getExons().add(r);
        }

        Map<String, String> attributes = attributeMap.get(transcript);

        for (String attribute : CollectionUtils.sortKeys(attributes)) {
          gene.addAltName(GFF3Parser.formatAttributeName(attribute),
              attributes.get(attribute));
        }

        // Extend to create promotor region
        if (Strand.isSense(strand)) {
          start -= ext5p;
        } else {
          end += ext3p;
        }

        gappedSearch.add(GenomicRegion.create(chr, start, end), gene);
      }
    }
  }

  public static List<String> geneIdTypes(Path file) throws IOException {

    Splitter splitter = Splitter.onTab();

    BufferedReader reader = FileUtils.newBufferedReader(file);

    List<String> ret = new ArrayList<String>(25);

    try {

      List<String> header = splitter.text(reader.readLine());

      ret.addAll(
          CollectionUtils.subList(header, ALT_COL, header.size() - ALT_COL));
    } finally {
      reader.close();
    }

    Collections.sort(ret);

    return ret;
  }

  public static List<String> gff3IdTypes(Path file) throws IOException {

    BufferedReader reader = FileUtils.newBufferedReader(file);

    List<String> ret = new ArrayList<String>(25);

    try {
      Map<String, String> attributes = GFF3Parser
          .parseGFF3Attributes(TextUtils.tabSplit(reader.readLine()));

      for (String attribute : CollectionUtils.sortKeys(attributes)) {
        ret.add(GFF3Parser.formatAttributeName(attribute));
      }
    } finally {
      reader.close();
    }

    Collections.sort(ret);

    return ret;
  }
}
