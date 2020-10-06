/**
 * Copyright 2016 Antony Holmes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.columbia.rdf.matcalc.bio.toolbox.genepattern.io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import org.jebtk.bioinformatics.ext.genepattern.ResMatrix;
import org.jebtk.bioinformatics.ui.filters.ResGuiFileFilter;
import org.jebtk.math.matrix.DataFrame;
import org.jebtk.modern.dialog.ModernDialogStatus;
import org.jebtk.modern.io.GuiFileExtFilter;

import edu.columbia.rdf.matcalc.FileType;
import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.toolbox.core.io.IOModule;

/**
 * Allow users to open and save Broad RES files
 *
 * @author Antony Holmes
 *
 */
public class ResIOModule extends IOModule {
  private static final GuiFileExtFilter FILTER = new ResGuiFileFilter();

  public ResIOModule() {
    registerFileType(FILTER);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.abh.lib.NameProperty#getName()
   */
  @Override
  public String getName() {
    return "RES IO";
  }

  @Override
  public DataFrame read(final MainMatCalcWindow window, final Path file, FileType type, int headers, int rowAnnotations,
      String delimiter, Collection<String> skipLines) throws IOException {

    ResDialog dialog = new ResDialog(window);

    dialog.setVisible(true);

    if (dialog.getStatus() == ModernDialogStatus.CANCEL) {
      return null;
    }

    return ResMatrix.parseMatrix(file, dialog.getKeepCols());
  }

  @Override
  public boolean write(final MainMatCalcWindow window, final Path file, final DataFrame m) throws IOException {
    ResMatrix.writeResMatrix(m, file);

    return true;
  }
}
