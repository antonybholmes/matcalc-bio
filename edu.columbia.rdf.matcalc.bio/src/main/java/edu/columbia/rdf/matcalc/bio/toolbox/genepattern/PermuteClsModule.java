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
package edu.columbia.rdf.matcalc.bio.toolbox.genepattern;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.batik.transcoder.TranscoderException;
import org.jebtk.core.Mathematics;
import org.jebtk.core.collections.UniqueArrayList;
import org.jebtk.core.io.PathUtils;
import org.jebtk.graphplot.figure.series.XYSeries;
import org.jebtk.graphplot.figure.series.XYSeriesGroup;
import org.jebtk.math.matrix.DataFrame;
import org.jebtk.modern.AssetService;
import org.jebtk.modern.dialog.MessageDialogType;
import org.jebtk.modern.dialog.ModernDialogStatus;
import org.jebtk.modern.dialog.ModernMessageDialog;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.io.RecentFilesService;
import org.jebtk.modern.ribbon.RibbonLargeButton;

import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.toolbox.Module;

/**
 * The class BoxWhiskerPlotModule.
 */
public class PermuteClsModule extends Module implements ModernClickListener {

  /**
   * The member parent.
   */
  private MainMatCalcWindow mParent;

  /*
   * (non-Javadoc)
   * 
   * @see org.abh.lib.NameProperty#getName()
   */
  @Override
  public String getName() {
    return "CLS";
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.columbia.rdf.apps.matcalc.modules.Module#init(edu.columbia.rdf.apps.
   * matcalc.MainMatCalcWindow)
   */
  @Override
  public void init(MainMatCalcWindow window) {
    mParent = window;

    RibbonLargeButton button = new RibbonLargeButton("Permute CLS", AssetService.getInstance().loadIcon("save", 24),
        "Permute CLS", "Create random permutations of a GenePattern CLS using the groups.");
    button.addClickListener(this);

    mParent.getRibbon().getToolbar("Bioinformatics").getSection("GenePattern").add(button);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.abh.lib.ui.modern.event.ModernClickListener#clicked(org.abh.lib.ui.
   * modern .event.ModernClickEvent)
   */
  @Override
  public void clicked(ModernClickEvent e) {
    try {
      permute();
    } catch (IOException e1) {
      e1.printStackTrace();
    } catch (ParseException e1) {
      e1.printStackTrace();
    }
  }

  /**
   * Export.
   *
   * @throws IOException         Signals that an I/O exception has occurred.
   * @throws ParseException
   * @throws TranscoderException the transcoder exception
   */
  private void permute() throws IOException, ParseException {
    export(RecentFilesService.getInstance().getPwd());
  }

  /**
   * Export matrix.
   *
   * @param pwd the pwd
   * @throws IOException         Signals that an I/O exception has occurred.
   * @throws ParseException
   * @throws TranscoderException the transcoder exception
   */
  private void export(Path pwd) throws IOException, ParseException {
    DataFrame m = mParent.getCurrentMatrix();

    if (m == null) {
      showLoadMatrixError(mParent);

      return;
    }

    XYSeriesGroup groups = mParent.getGroups();

    if (groups.size() < 2) {
      ModernMessageDialog.createDialog(mParent, "You must create some groups.", MessageDialogType.WARNING);

      return;
    }

    PermuteDialog dialog = new PermuteDialog(mParent, mParent.getGroups());

    dialog.setVisible(true);

    if (dialog.getStatus() == ModernDialogStatus.CANCEL) {
      return;
    }

    int reps = dialog.getPermutations();

    if (reps < 1) {
      ModernMessageDialog.createDialog(mParent, "You must create at least one permutation.", MessageDialogType.WARNING);

      return;
    }

    Path dir = dialog.getDir();

    String prefix = dialog.getPrefix();

    DataFrame matrix = mParent.getCurrentMatrix();

    groups = mParent.getGroups();

    boolean sampleWithReplacement = dialog.getSampleWithReplacement();

    XYSeries group1 = dialog.getGroup1();
    int size1 = dialog.getSample1Size();

    XYSeries group2 = dialog.getGroup2();
    int size2 = dialog.getSample2Size();

    List<Integer> indices1 = XYSeries.findColumnIndices(matrix, group1);
    List<Integer> indices2 = XYSeries.findColumnIndices(matrix, group2);

    // Create multiple files
    for (int rep = 0; rep < reps; ++rep) {
      Path file = dir.resolve(prefix + "_" + (rep + 1) + ".cls");

      System.err.println("Creating CLS " + file);

      Map<String, String> groupMap = new HashMap<String, String>();

      for (String name : matrix.getColumnNames()) {
        groupMap.put(name, Cls.UNDEF_GROUP);
      }

      List<Integer> subIndices1;
      List<Integer> subIndices2;

      if (sampleWithReplacement) {
        subIndices1 = Mathematics.randSubsetWithReplacement(indices1, size1);
        subIndices2 = Mathematics.randSubsetWithReplacement(indices2, size2);
      } else {
        subIndices1 = Mathematics.randSubsetWithoutReplacement(indices1, size1);
        subIndices2 = Mathematics.randSubsetWithoutReplacement(indices2, size2);
      }

      for (int i : subIndices1) {
        groupMap.put(matrix.getColumnName(i), group1.getName());
      }

      for (int i : subIndices2) {
        groupMap.put(matrix.getColumnName(i), group2.getName());
      }

      // Now make a list of the unique group names in the order they appear

      List<String> names = new UniqueArrayList<String>();

      for (String name : matrix.getColumnNames()) {
        names.add(groupMap.get(name));
      }

      Cls.write(file, names, groupMap, matrix);

    }

    if (reps == 1) {
      ModernMessageDialog.createDialog(mParent, MessageDialogType.INFORMATION, "The CLS file has been saved in:",
          PathUtils.toString(dir));
    } else {
      ModernMessageDialog.createDialog(mParent, MessageDialogType.INFORMATION,
          "The " + reps + " CLS files have been saved in:", PathUtils.toString(dir));
    }
  }
}
