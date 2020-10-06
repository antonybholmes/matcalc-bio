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
package edu.columbia.rdf.matcalc.bio.toolbox.expression;

import org.jebtk.math.matrix.DataFrame;
import org.jebtk.math.ui.matrix.MatrixTransforms;
import org.jebtk.modern.button.ModernButtonWidget;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.graphics.icons.FilterVectorIcon;
import org.jebtk.modern.graphics.icons.ModernIcon;
import org.jebtk.modern.graphics.icons.Raster32Icon;
import org.jebtk.modern.menu.ModernPopupMenu2;
import org.jebtk.modern.menu.ModernTwoLineMenuItem;
import org.jebtk.modern.ribbon.RibbonLargeDropDownButton2;
import org.jebtk.modern.theme.ThemeService;

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
public class ExpressionModule extends Module implements ModernClickListener {
  public static final double SEGMENT_MEAN_ZERO = 0.0001;

  /*
   * private RibbonLargeButton2 mStvDevButton = new RibbonLargeButton2("STDEV",
   * UIResources.getInstance().loadIcon("std_dev", 32),
   * "Standard Deviation Filter",
   * "Filter out rows whose standard deviation does not meet a threshold.");
   * 
   * 
   * private RibbonLargeButton2 mMinExpButton = new RibbonLargeButton2("Min",
   * "Expression", UIResources.getInstance().loadIcon("min_exp", 32),
   * "Minimum Expression",
   * "Filter out rows that do not contain a specified number of cells with a minimum value."
   * );
   */

  private static final ModernIcon EXPRESSION_ICON = new Raster32Icon(
      new FilterVectorIcon(ThemeService.getInstance().getColors().getBlueTheme().getColor(5),
          ThemeService.getInstance().getColors().getBlueTheme().getColor(4)));

  /**
   * The member window.
   */
  private MainMatCalcWindow mWindow;

  private ModernButtonWidget mButton;

  /*
   * (non-Javadoc)
   * 
   * @see org.abh.lib.NameProperty#getName()
   */
  @Override
  public String getName() {
    return "Expression";
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.columbia.rdf.apps.matcalc.modules.Module#init(edu.columbia.rdf.apps.
   * matcalc.MainMatCalcWindow)
   */
  @Override
  public void init(MainMatCalcWindow window) {
    mWindow = window;

    // mStvDevButton.addClickListener(this);
    // mWindow.getRibbon().getToolbar("Bioinformatics").getSection("Expression").add(mStvDevButton);
    // mMinExpButton.addClickListener(this);
    // mWindow.getRibbon().getToolbar("Bioinformatics").getSection("Expression").add(mMinExpButton);

    ModernPopupMenu2 popup = new ModernPopupMenu2();

    popup.addMenuItem(
        new ModernTwoLineMenuItem("STDEV", "Rows must have a minimum standard deviation.", EXPRESSION_ICON));
    popup.addMenuItem(new ModernTwoLineMenuItem("Mean", "Rows must have a minimum mean.", EXPRESSION_ICON));
    popup.addMenuItem(new ModernTwoLineMenuItem("Minimum Expression", "Rows must have a minimum value in some columns.",
        EXPRESSION_ICON));

    mButton = new RibbonLargeDropDownButton2("Expression", EXPRESSION_ICON, popup);
    mButton.setShowText(false);

    mWindow.getRibbon().getToolbar("Data").getSection("Filter").add(mButton);

    mButton.addClickListener(this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.abh.lib.ui.modern.event.ModernClickListener#clicked(org.abh.lib.ui.
   * modern .event.ModernClickEvent)
   */
  @Override
  public final void clicked(ModernClickEvent e) {
    DataFrame m = mWindow.getCurrentMatrix();

    if (e.getMessage().equals("STDEV")) {
      mWindow.history().addToHistory("STDEV Filter", MatrixTransforms.stdDevFilter(mWindow, m, 1.5));
    } else if (e.getMessage().equals("Mean")) {
      mWindow.history().addToHistory("STDEV Filter", MatrixTransforms.meanFilter(mWindow, m, 1.5));
    } else if (e.getMessage().equals("Minimum Expression")) {
      mWindow.history().addToHistory("Minimum Expression Filter", MatrixTransforms.minExpFilter(mWindow, m, 100, 2)); // new
                                                                                                                      // MinExpFilterMatrixTransform(this,
                                                                                                                      // getCurrentMatrix(),
                                                                                                                      // 100,
                                                                                                                      // 2));

    } else {
      // Do nothing
    }
  }
}
