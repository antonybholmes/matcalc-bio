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

import org.apache.batik.transcoder.TranscoderException;
import org.jebtk.bioinformatics.ui.external.genepattern.ClsGuiFileFilter;
import org.jebtk.core.io.FileUtils;
import org.jebtk.graphplot.figure.series.XYSeriesGroup;
import org.jebtk.math.matrix.DataFrame;
import org.jebtk.modern.AssetService;
import org.jebtk.modern.dialog.MessageDialogType;
import org.jebtk.modern.dialog.ModernDialogStatus;
import org.jebtk.modern.dialog.ModernMessageDialog;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.io.FileDialog;
import org.jebtk.modern.io.GuiFileExtFilter;
import org.jebtk.modern.io.RecentFilesService;
import org.jebtk.modern.ribbon.RibbonLargeButton;

import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.toolbox.core.io.IOModule;

/**
 * The class BoxWhiskerPlotModule.
 */
public class ClsModule extends IOModule implements ModernClickListener {

  private static final GuiFileExtFilter SAVE_CLS_FILTER = new ClsGuiFileFilter();
  /**
   * The member parent.
   */
  private MainMatCalcWindow mParent;

  public ClsModule() {
    registerFileSaveType(SAVE_CLS_FILTER);
  }

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

    RibbonLargeButton button = new RibbonLargeButton("Export CLS", AssetService.getInstance().loadIcon("save", 24),
        "Export CLS", "Export a GenePattern CLS using the groups.");
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
      export();
    } catch (IOException e1) {
      e1.printStackTrace();
    }
  }

  /**
   * Export.
   *
   * @throws IOException         Signals that an I/O exception has occurred.
   * @throws TranscoderException the transcoder exception
   */
  private void export() throws IOException {
    export(RecentFilesService.getInstance().getPwd());
  }

  /**
   * Export matrix.
   *
   * @param pwd the pwd
   * @throws IOException         Signals that an I/O exception has occurred.
   * @throws TranscoderException the transcoder exception
   */
  private void export(Path pwd) throws IOException {
    DataFrame m = mParent.getCurrentMatrix();

    if (m == null) {
      showLoadMatrixError(mParent);

      return;
    }

    Path file = FileDialog.saveFile(mParent, pwd, new ClsGuiFileFilter());

    boolean status = save(file, m);

    if (status) {
      ModernMessageDialog.createFileSavedDialog(mParent, file);
    }
  }

  private boolean save(Path file, DataFrame m) throws IOException {
    if (file == null) {
      return false;
    }

    if (FileUtils.exists(file)) {
      ModernDialogStatus status = ModernMessageDialog.createFileReplaceDialog(mParent, file);

      if (status == ModernDialogStatus.CANCEL) {
        return false;
      }
    }

    XYSeriesGroup groups = mParent.getGroups();

    if (groups.size() == 0) {
      ModernMessageDialog.createDialog(mParent, "You must create some groups.", MessageDialogType.WARNING);

      return false;
    }

    Cls.write(file, mParent.getGroups(), m);

    return true;
  }

  // @Override
  // public GuiFileExtFilter getSaveFileFilter() {
  // return SAVE_CLS_FILTER;
  // }

  /*
   * (non-Javadoc)
   * 
   * @see org.matcalc.toolbox.CalcModule#openFile(org.matcalc.MainMatCalcWindow,
   * java.nio.file.Path, boolean, int)
   */
  @Override
  public boolean write(final MainMatCalcWindow window, final Path file, final DataFrame m) throws IOException {
    return save(file, m);
  }
}
