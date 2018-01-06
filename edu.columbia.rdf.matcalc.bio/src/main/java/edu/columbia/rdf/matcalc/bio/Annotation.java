package edu.columbia.rdf.matcalc.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.jebtk.bioinformatics.ext.ucsc.Bed;
import org.jebtk.bioinformatics.ext.ucsc.BedRegion;
import org.jebtk.bioinformatics.gapsearch.BinaryGapSearch;
import org.jebtk.bioinformatics.gapsearch.FixedGapSearch;
import org.jebtk.bioinformatics.genomic.ChromosomeService;
import org.jebtk.bioinformatics.genomic.GenomeService;
import org.jebtk.bioinformatics.genomic.GenomicRegion;
import org.jebtk.bioinformatics.ui.Bioinformatics;
import org.jebtk.core.collections.CollectionUtils;
import org.jebtk.core.io.FileUtils;
import org.jebtk.core.io.Io;
import org.jebtk.core.io.PathUtils;
import org.jebtk.core.text.TextUtils;
import org.jebtk.math.external.microsoft.Excel;
import org.jebtk.math.matrix.DataFrame;
import org.jebtk.modern.dataview.ModernDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Annotation implements Comparable<Annotation> {
  private String mName;
  private GenomicRegion mRegion;

  private static final Logger LOG = LoggerFactory.getLogger(Annotation.class);

  public Annotation(String name, GenomicRegion region) {
    mName = name;
    mRegion = region;
  }

  public String getName() {
    return mName;
  }

  public GenomicRegion getRegion() {
    return mRegion;
  }

  @Override
  public int compareTo(Annotation a) {
    return mName.compareTo(a.mName);
  }

  public static Map<String, BinaryGapSearch<Annotation>> parseBedEnhancers(Path file)
      throws IOException, ParseException {
    return parseBedEnhancers(CollectionUtils.asList(file));
  }

  public static Map<String, BinaryGapSearch<Annotation>> parseBedEnhancers(List<Path> files)
      throws IOException, ParseException {
    Map<String, BinaryGapSearch<Annotation>> map = new HashMap<String, BinaryGapSearch<Annotation>>();

    for (Path file : files) {

      String name = file.getFileName().toString().substring(0, file.getFileName().toString().length() - 4);

      System.err.println("Loading " + file + " " + name);

      map.put(name, parseBed(file));
    }

    return map;
  }

  public static BinaryGapSearch<Annotation> parseBed(Path file) throws IOException {
    return parseBed(CollectionUtils.asList(file));
  }

  public static BinaryGapSearch<Annotation> parseBed(List<Path> files) throws IOException {
    BinaryGapSearch<Annotation> annotations = new BinaryGapSearch<Annotation>();

    parseBed(files, annotations);

    return annotations;
  }

  public static FixedGapSearch<Annotation> parseBedFixed(Path file) throws IOException {
    return parseBedFixed(CollectionUtils.asList(file));
  }

  public static FixedGapSearch<Annotation> parseBedFixed(List<Path> files) throws IOException {
    FixedGapSearch<Annotation> annotations = new FixedGapSearch<Annotation>();

    parseBed(files, annotations);

    return annotations;
  }

  public static void parseBed(List<Path> files, FixedGapSearch<Annotation> annotations) throws IOException {
    for (Path file : files) {
      System.err.println("Loading BED " + file);

      // check for header

      BufferedReader reader = FileUtils.newBufferedReader(file);

      String line;

      int c = 0;

      try {
        while ((line = reader.readLine()) != null) {
          if (Io.isEmptyLine(line)) {
            continue;
          }

          if (line.startsWith("track")) {
            continue;
          }

          BedRegion region = BedRegion.parse(GenomeService.getInstance().guess(file), line);

          if (region != null) {
            Annotation annotation = new Annotation(region.getName(), region);

            annotations.add(region, annotation);

            ++c;
          } else {
            LOG.info("Invalid region (ignored): {}", line);
          }
        }
      } finally {
        reader.close();
      }

      LOG.info("BED file contained {} lines.", c);
    }
  }

  public static BinaryGapSearch<Annotation> parsePeaks(Path file) throws IOException, ParseException {
    BinaryGapSearch<Annotation> gappedSearch = new BinaryGapSearch<Annotation>();

    LOG.info("Parsing {}...", file);

    BufferedReader reader = FileUtils.newBufferedReader(file);

    String line;
    List<String> tokens;

    try {
      while ((line = reader.readLine()) != null) {
        if (Io.isEmptyLine(line)) {
          continue;
        }

        tokens = TextUtils.tabSplit(line);

        GenomicRegion region = GenomicRegion.parse(tokens.get(0));

        Annotation annotation = new Annotation(region.toString(), region);

        gappedSearch.add(region, annotation);
      }

    } finally {
      reader.close();
    }

    return gappedSearch;
  }

  public static BinaryGapSearch<Annotation> parsePeaks(ModernDataModel model) {
    BinaryGapSearch<Annotation> gappedSearch = new BinaryGapSearch<Annotation>();

    for (int i = 0; i < model.getRowCount(); ++i) {
      GenomicRegion region = GenomicRegion.parse(model.getValueAsString(i, 0));

      if (region == null) {
        continue;
      }

      Annotation annotation = new Annotation(region.toString(), region);

      gappedSearch.add(region, annotation);
    }

    return gappedSearch;
  }

  public static BinaryGapSearch<Annotation> parseRegions(DataFrame model) throws ParseException {
    BinaryGapSearch<Annotation> gappedSearch = new BinaryGapSearch<Annotation>();

    parseRegions(model, gappedSearch);

    return gappedSearch;
  }

  public static FixedGapSearch<Annotation> parseRegionsFixed(DataFrame model) {
    FixedGapSearch<Annotation> gappedSearch = new FixedGapSearch<Annotation>();

    parseRegions(model, gappedSearch);

    return gappedSearch;
  }

  public static void parseRegions(DataFrame model, FixedGapSearch<Annotation> gappedSearch) {
    for (int i = 0; i < model.getRows(); ++i) {

      GenomicRegion region = null;

      if (GenomicRegion.isGenomicRegion(model.getText(i, 0))) {
        region = GenomicRegion.parse(model.getText(i, 0));
      } else {
        region = new GenomicRegion(ChromosomeService.getInstance().parse(model.getText(i, 0)),
            (int) model.getValue(i, 1), (int) model.getValue(i, 2));
      }

      if (region == null) {
        continue;
      }

      Annotation annotation = new Annotation(region.toString(), region);

      gappedSearch.add(region, annotation);
    }
  }

  public static FixedGapSearch<Annotation> parseRegionsFixed(Path file) throws IOException, ParseException {
    FixedGapSearch<Annotation> gappedSearch = new FixedGapSearch<Annotation>();

    System.err.println("Loading " + file);

    BufferedReader reader = FileUtils.newBufferedReader(file);

    String line;
    List<String> tokens;

    try {
      while ((line = reader.readLine()) != null) {
        if (Io.isEmptyLine(line)) {
          continue;
        }

        tokens = TextUtils.tabSplit(line);

        GenomicRegion region = GenomicRegion.parse(tokens.get(0));

        Annotation annotation = new Annotation(region.toString(), region);

        gappedSearch.add(region, annotation);
      }

    } finally {
      reader.close();
    }

    return gappedSearch;
  }

  public static FixedGapSearch<Annotation> parsePeaksFixed(ModernDataModel model, int header) throws ParseException {
    FixedGapSearch<Annotation> gappedSearch = new FixedGapSearch<Annotation>();

    for (int i = 0; i < model.getRowCount(); ++i) {
      GenomicRegion region = GenomicRegion.parse(model.getValueAsString(i, header));

      if (region == null) {
        continue;
      }

      Annotation annotation = new Annotation(region.toString(), region);

      gappedSearch.add(region, annotation);
    }

    return gappedSearch;
  }

  public static FixedGapSearch<Annotation> parseFixed(Path file, int header)
      throws IOException, InvalidFormatException, ParseException {
    FixedGapSearch<Annotation> gappedSearch;

    if (PathUtils.getFileExt(file).equals("bed")) {
      gappedSearch = Annotation.parseBedFixed(file);
    } else if (PathUtils.getFileExt(file).equals("bedgraph")) {
      gappedSearch = Annotation.parseBedFixed(file);
    } else {
      ModernDataModel model = Bioinformatics.getModel(file, true, TextUtils.emptyList(), 0, TextUtils.TAB_DELIMITER);

      gappedSearch = parsePeaksFixed(model, header);
    }

    return gappedSearch;
  }

  public static void main(String[] args) throws IOException {
    /*
     * List<Path> files = ArrayUtils.toList(new
     * Path("res/enhancers/human/Bladder.bed"));
     * 
     * GappedSearch<Annotation> gappedSearch = Annotation.parseBED(files);
     * 
     * GappedSearchRange<Annotation> results =
     * gappedSearch.getFeatures(Chromosome.CHR11, 65238140, 114070355);
     * 
     * for (GappedSearchFeatures<Annotation> annotations : results) {
     * System.err.println("Position " + annotations.getPosition());
     * 
     * for (Annotation annotation : annotations) { System.err.println("name " +
     * annotation.getName() + " " + annotation.getRegion()); } }
     */
  }

  /**
   * Parse a file of genomic locations.
   * 
   * @param file
   * @return
   * @throws InvalidFormatException
   * @throws ParseException
   * @throws IOException
   */
  public static FixedGapSearch<Annotation> parse(Path file) throws InvalidFormatException, ParseException, IOException {
    if (PathUtils.getFileExt(file).equals("bed") || PathUtils.getFileExt(file).equals("bedgraph")) {
      return parseBed(file);
    } else {
      return parseRegions(Excel.convertToMatrix(file, true, TextUtils.emptyList(), 0, TextUtils.TAB_DELIMITER));
    }

  }

  public static FixedGapSearch<Annotation> parseFixed(Path file) throws InvalidFormatException, IOException {
    if (PathUtils.getFileExt(file).equals("bed") || PathUtils.getFileExt(file).equals("bedgraph")) {
      return parseBedFixed(file);
    } else {
      return parseRegionsFixed(Excel.convertToMatrix(file, true, TextUtils.emptyList(), 0, TextUtils.TAB_DELIMITER));
    }
  }

  public static DataFrame toMatrix(Path file) throws InvalidFormatException, IOException {
    if (PathUtils.getFileExt(file).equals("bed") || PathUtils.getFileExt(file).equals("bedgraph")) {
      return Bed.toMatrix(file);
    } else {
      return Excel.convertToMatrix(file, true, TextUtils.emptyList(), 0, TextUtils.TAB_DELIMITER);
    }
  }
}
