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
package edu.columbia.rdf.matcalc.bio.toolbox.fillgaps;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.jebtk.bioinformatics.ext.ucsc.Bed;
import org.jebtk.bioinformatics.ext.ucsc.UCSCTrack;
import org.jebtk.bioinformatics.gapsearch.BinaryGapSearch;
import org.jebtk.bioinformatics.gapsearch.BinarySearch;
import org.jebtk.bioinformatics.gapsearch.GappedSearchFeatures;
import org.jebtk.bioinformatics.genomic.Chromosome;
import org.jebtk.bioinformatics.genomic.Genome;
import org.jebtk.bioinformatics.genomic.GenomeService;
import org.jebtk.bioinformatics.genomic.GenomicElement;
import org.jebtk.bioinformatics.genomic.GenomicElementsMap;
import org.jebtk.bioinformatics.genomic.GenomicRegion;
import org.jebtk.bioinformatics.genomic.GenomicType;
import org.jebtk.bioinformatics.genomic.Human;
import org.jebtk.core.cli.ArgParser;
import org.jebtk.core.cli.Args;
import org.jebtk.core.io.FileUtils;
import org.jebtk.core.io.PathUtils;
import org.jebtk.math.matrix.DataFrame;
import org.jebtk.modern.AssetService;
import org.jebtk.modern.button.ModernButton;
import org.jebtk.modern.dialog.MessageDialogType;
import org.jebtk.modern.dialog.ModernDialogStatus;
import org.jebtk.modern.dialog.ModernMessageDialog;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.ribbon.RibbonLargeButton;
import org.jebtk.modern.tooltip.ModernToolTip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.toolbox.Module;

/**
 * Merges designated segments together using the merge column. Consecutive rows
 * with the same merge id will be merged together. Coordinates and copy number
 * will be adjusted but genes, cytobands etc are not.
 *
 * @author Antony Holmes
 *
 */
public class FillGapsModule extends Module implements ModernClickListener {
  private static final Logger LOG = LoggerFactory
      .getLogger(FillGapsModule.class);

  /**
   * The member convert button.
   */
  private ModernButton mFillGapsButton = new RibbonLargeButton("Fill Gaps",
      AssetService.getInstance().loadIcon("fill_gaps", 32),
      AssetService.getInstance().loadIcon("fill_gaps", 24));

  private static final Path RES_FOLDER = PathUtils
      .getPath("res/modules/annotation");

  public static final double SEGMENT_MEAN_ZERO = 0.0001;
  
  private static final Args ARGS = new Args();
  
  static {
    ARGS.add('s', "switch-tab");
  }

  /**
   * The member window.
   */
  private MainMatCalcWindow mWindow;

  private Map<String, Path> mBedFileMap = new TreeMap<String, Path>();

  private Map<String, Map<String, String>> mDescriptionMap = 
      new TreeMap<String, Map<String, String>>();

  /*
   * (non-Javadoc)
   * 
   * @see org.abh.lib.NameProperty#getName()
   */
  @Override
  public String getName() {
    return "Fill Gaps";
  }

  @Override
  public Args getArgs() {
    return ARGS;
  }
  
  @Override
  public void run(ArgParser ap) {
    if (ap.contains("switch-tab")) {
       mWindow.getRibbon().changeTab("Annotation");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * edu.columbia.rdf.apps.matcalc.modules.Module#init(edu.columbia.rdf.apps.
   * matcalc.MainMatCalcWindow)
   */
  @Override
  public void init(MainMatCalcWindow window) {
    mWindow = window;

    try {
      load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // home
    mFillGapsButton.setToolTip(new ModernToolTip("Fill Gaps", "Fill gaps using reference."));
    mFillGapsButton.setClickMessage("Fill Gaps");
    mWindow.getRibbon().getToolbar("Bioinformatics").getSection("Fill Gaps")
        .add(mFillGapsButton);

    mFillGapsButton.addClickListener(this);

  }

  private void load() throws IOException {
    if (!FileUtils.exists(RES_FOLDER)) {
      return;
    }

    for (Path file : FileUtils.ls(RES_FOLDER)) {
      if (PathUtils.getName(file).contains("bed.gz")) {

        String name = null;

        try {
          name = UCSCTrack.getNameFromTrack(file);
        } catch (IOException e) {
          e.printStackTrace();
        }

        mBedFileMap.put(name, file);

        try {
          mDescriptionMap.put(name, UCSCTrack.getTrackAttributes(file));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.abh.lib.ui.modern.event.ModernClickListener#clicked(org.abh.lib.ui.
   * modern .event.ModernClickEvent)
   */
  @Override
  public final void clicked(ModernClickEvent e) {
    if (e.getSource().equals(mFillGapsButton)) {
      try {
        annotate(Genome.HG18);
      } catch (Exception e1) {
        e1.printStackTrace();
      }
    }
  }

  /**
   * Analysis.
   * 
   * @throws Exception
   */
  private void annotate(Genome genome) throws Exception {
    DataFrame m = mWindow.getCurrentMatrix();

    if (m == null) {
      showLoadMatrixError(mWindow);

      return;
    }

    Map<String, Integer> colMap = findColumns(mWindow,
        m,
        "chr",
        "start",
        "end",
        "markers|probes",
        "segment",
        "mean");

    if (colMap == null) {
      return;
    }

    FillGapsDialog dialog = new FillGapsDialog(mWindow, mBedFileMap,
        mDescriptionMap);

    dialog.setVisible(true);

    if (dialog.getStatus() == ModernDialogStatus.CANCEL) {
      return;
    }

    UCSCTrack track = Bed.parseTrack(GenomicType.REGION, mBedFileMap.get(dialog.getAnnotation()));

    BinarySearch<GenomicRegion> gapSearch = getBinarySearch(
        track.getElements());

    List<String> samples = dialog.getSamples();

    double meanZero = dialog.getMeanZero();

    if (samples == null) {
      ModernMessageDialog.createDialog(mWindow,
          "You must load some samples",
          MessageDialogType.WARNING);

      return;
    }

    Set<String> allSamples = new HashSet<String>();

    for (String sample : samples) {
      allSamples.add(sample);
    }

    // Organize segments by sample and so we can sort them
    Map<String, Map<Chromosome, List<Segment>>> segments = 
        new TreeMap<String, Map<Chromosome, List<Segment>>>();

    for (int r = 0; r < m.getRows(); ++r) {
      String name = m.getText(r, colMap.get("segment"));

      if (!allSamples.contains(name)) {
        continue;
      }

      Segment segment = new Segment();

      segment.name = name;
      segment.chr = GenomeService.getInstance()
          .chr(genome, m.getText(r, colMap.get("chr")));
      segment.start = (int) m.getValue(r, colMap.get("start"));
      segment.end = (int) m.getValue(r, colMap.get("end"));
      segment.markers = (int) m.getValue(r, colMap.get("markers|probes"));
      segment.copyNumberMean = m.getValue(r, colMap.get("mean"));

      addSegment(segment, segments);
    }

    // now we have some segments, update to closest snp probes

    for (String name : segments.keySet()) {
      for (Chromosome chr : segments.get(name).keySet()) {
        for (Segment segment : segments.get(name).get(chr)) {
          List<GappedSearchFeatures<GenomicRegion>> features = gapSearch
              .getFeatures(chr, segment.start, segment.end);

          // update so the segment begins and ends on a probe
          segment.start = features.get(0).getPosition();
          segment.end = features.get(features.size() - 1).getPosition();
          segment.markers = features.size();
        }
      }
    }

    // Sort by position

    for (String name : segments.keySet()) {
      for (Chromosome chr : segments.get(name).keySet()) {
        Collections.sort(segments.get(name).get(chr));
      }
    }

    mWindow.history().addToHistory("Closest Markers", segmentsToMatrix(segments));

    // regions that happen to start and end close to each other may end
    // up overlapping since we extend regions in both directions to map
    // to probes. Thus in adjacent segments s1 and s2, s1's end may be
    // extended past s2's start and s2's start may be extended in the 5'
    // beyond s1's end. To correct for this, set overlapping ends to the
    // start of the next segment

    /*
     * for (String name : segments.keySet()) { for (Chromosome chr :
     * segments.get(name).keySet()) { for (int i = 0; i <
     * segments.get(name).get(chr).size() - 1; ++i) { Segment segment1 =
     * segments.get(name).get(chr).get(i); Segment segment2 =
     * segments.get(name).get(chr).get(i + 1);
     * 
     * if (segment1.end >= segment2.start) { int index =
     * BinaryGapSearch.getStartIndex(gapSearch.getFeatures(chr),
     * segment2.start);
     * 
     * System.err.println("hum " + segment1.end + " " + segment2.start + " " +
     * chr);
     * 
     * 
     * System.err.println("new end " + segment1.end + " " +
     * (gapSearch.getFeaturesAt(chr, index - 1).getPosition()));
     * 
     * segment1.end = gapSearch.getFeaturesAt(chr, index - 1).getPosition();
     * 
     * // loss of a marker --segment1.markers; } } } }
     */

    // If a segment runs into another, merge the two as one

    for (String name : segments.keySet()) {
      for (Chromosome chr : segments.get(name).keySet()) {
        boolean merged = true;

        while (merged) {
          merged = false;

          int remove = -1;

          for (int i = 0; i < segments.get(name).get(chr).size() - 1; ++i) {
            Segment segment1 = segments.get(name).get(chr).get(i);
            Segment segment2 = segments.get(name).get(chr).get(i + 1);

            // merge 1 into 2
            if (segment1.end >= segment2.start) {
              segment1.end = Math.max(segment1.end, segment2.end);

              int i1 = BinaryGapSearch.getStartIndex(gapSearch.getBins(chr),
                  segment1.start);
              int i2 = BinaryGapSearch.getStartIndex(gapSearch.getBins(chr),
                  segment1.end);

              segment1.markers = i2 - i1 + 1;

              System.err.println("hum " + segment1.start + " " + segment1.end
                  + " " + segment2.start + " " + segment2.end + " " + chr + " "
                  + i1 + " " + i2);

              remove = i + 1;

              merged = true;

              break;
            }
          }

          // Remove offending segments
          if (remove != -1) {
            segments.get(name).get(chr).remove(remove);
          }
        }
      }
    }

    mWindow.history().addToHistory("Remove Overlaps", segmentsToMatrix(segments));

    //
    // Fill in the blanks
    //

    Map<String, Map<Chromosome, List<Segment>>> newSegments = new TreeMap<String, Map<Chromosome, List<Segment>>>();

    Segment segment;
    Segment previousSegment;

    Segment newSegment;
    int index;
    int previousIndex;
    int nextIndex;

    // Since the segments are ordered, add a new segment from the
    // chr start to this segment start

    for (String name : segments.keySet()) {
      for (Chromosome chr : segments.get(name).keySet()) {
        segment = segments.get(name).get(chr).get(0);

        index = BinaryGapSearch.getStartIndex(gapSearch.getBins(chr),
            segment.start);

        // System.err.println("huh " + segment.name + " " + chr + " " +
        // segment.start +
        // " " + index);

        if (index > 0) {
          previousIndex = index - 1;

          newSegment = new Segment();
          newSegment.name = name;
          newSegment.chr = chr;
          newSegment.start = gapSearch.getFeaturesAt(chr, 0).getPosition();
          newSegment.end = gapSearch.getFeaturesAt(chr, previousIndex)
              .getPosition();
          newSegment.markers = previousIndex + 1;
          newSegment.copyNumberMean = meanZero;

          addSegment(newSegment, newSegments);
        }

        // Add the first segment

        addSegment(segment, newSegments);
      }
    }

    // Fill in the blanks for these segments by taking each segment and
    // getting its rank, then calculate the difference in rank between it
    // and the previous segment. If the difference is greater than one,
    // add a blank.

    for (String name : segments.keySet()) {
      for (Chromosome chr : segments.get(name).keySet()) {
        for (int i = 1; i < segments.get(name).get(chr).size(); ++i) {
          segment = segments.get(name).get(chr).get(i);

          index = BinaryGapSearch.getStartIndex(gapSearch.getBins(chr),
              segment.start);

          System.err.println("i " + index + " " + segment.start);

          previousSegment = segments.get(name).get(chr).get(i - 1);

          previousIndex = BinarySearch.getEndIndex(gapSearch.getBins(chr),
              previousSegment.end);

          System.err.println("pi " + previousIndex + " " + previousSegment.end);

          if (index - previousIndex > 1) {
            // Since there is a gap of more than one rank, we need
            // to make a segment to fit in between the two segments
            --index;
            ++previousIndex;

            newSegment = new Segment();
            newSegment.name = name;
            newSegment.chr = chr;
            newSegment.start = gapSearch.getFeaturesAt(chr, previousIndex)
                .getPosition();
            newSegment.end = gapSearch.getFeaturesAt(chr, index).getPosition();
            newSegment.markers = index - previousIndex + 1;
            newSegment.copyNumberMean = meanZero;

            addSegment(newSegment, newSegments);
          }

          addSegment(segment, newSegments);
        }
      }
    }

    // Now deal with the end segments

    // Since the segments are ordered, add a new segment from the
    // chr start to this segment start

    for (String name : segments.keySet()) {
      for (Chromosome chr : segments.get(name).keySet()) {
        segment = segments.get(name).get(chr)
            .get(segments.get(name).get(chr).size() - 1);

        index = BinarySearch.getStartIndex(gapSearch.getBins(segment.chr),
            segment.end);

        nextIndex = gapSearch.getBins(segment.chr).size() - 1; // gapSearch.size(chr)
                                                               // - 1;

        if (index < nextIndex) {
          // System.err.println(chr + " " + index + " " + nextIndex);

          // If the index is not the last marker, fill the gap to
          // the end of the chromosome

          // We start at the next index.
          ++index;

          newSegment = new Segment();
          newSegment.name = name;
          newSegment.chr = chr;
          newSegment.start = gapSearch.getFeaturesAt(chr, index).getPosition();
          newSegment.end = gapSearch.getFeaturesAt(chr, nextIndex)
              .getPosition();
          newSegment.markers = nextIndex - index + 1;
          newSegment.copyNumberMean = meanZero;

          addSegment(newSegment, newSegments);
        }
      }
    }

    //
    // Now deal with segments with chromosomes missing
    //

    for (String name : samples) {
      for (Chromosome chr : Human.CHROMOSOMES) {
        if (!newSegments.containsKey(name)
            || !newSegments.get(name).containsKey(chr)) {
          // Empty chr that needs filling

          if (!gapSearch.containsChr(chr)) {
            continue;
          }

          newSegment = new Segment();
          newSegment.name = name;
          newSegment.chr = chr;
          newSegment.start = gapSearch.getFeaturesAt(chr, 0).getPosition();
          newSegment.end = gapSearch.getFeaturesAt(chr, gapSearch.size(chr) - 1)
              .getPosition();
          newSegment.markers = gapSearch.size(chr);
          newSegment.copyNumberMean = meanZero;

          addSegment(newSegment, newSegments);
        }
      }
    }

    mWindow.history().addToHistory("Fill Gaps", segmentsToMatrix(newSegments));
  }

  private static void addSegment(final Segment segment,
      Map<String, Map<Chromosome, List<Segment>>> segments) {
    if (!segments.containsKey(segment.name)) {
      segments.put(segment.name, new TreeMap<Chromosome, List<Segment>>());
    }

    if (!segments.get(segment.name).containsKey(segment.chr)) {
      segments.get(segment.name).put(segment.chr, new ArrayList<Segment>());
    }

    segments.get(segment.name).get(segment.chr).add(segment);
  }

  private static DataFrame segmentsToMatrix(
      Map<String, Map<Chromosome, List<Segment>>> segments) {

    int n = 0;

    for (String name : segments.keySet()) {
      for (Chromosome chr : segments.get(name).keySet()) {
        n += segments.get(name).get(chr).size();
      }
    }

    DataFrame ret = DataFrame.createDataFrame(n, 6);

    ret.setColumnName(0, "segment");
    ret.setColumnName(1, "chromosome");
    ret.setColumnName(2, "start");
    ret.setColumnName(3, "end");
    ret.setColumnName(4, "num.markers");
    ret.setColumnName(5, "seg.mean");

    int r = 0;

    for (String name : segments.keySet()) {
      for (Chromosome chr : segments.get(name).keySet()) {
        for (Segment s : segments.get(name).get(chr)) {
          ret.set(r, 0, s.name);
          ret.set(r, 1, s.chr.toString());
          ret.set(r, 2, s.start);
          ret.set(r, 3, s.end);
          ret.set(r, 4, s.markers);
          ret.set(r, 5, s.copyNumberMean);

          ++r;
        }
      }
    }

    return ret;
  }

  public static BinarySearch<GenomicRegion> getBinarySearch(
      GenomicElementsMap regions) {
    BinarySearch<GenomicRegion> search = new BinarySearch<GenomicRegion>();

    for (Entry<Chromosome, Set<GenomicElement>> region : regions) {
      for (GenomicElement e : region.getValue()) {
        search.add(e, e);
      }
    }

    return search;
  }
}
