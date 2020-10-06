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
package edu.columbia.rdf.matcalc.bio;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jebtk.bioinformatics.Fasta;
import org.jebtk.bioinformatics.genomic.Sequence;
import org.jebtk.bioinformatics.ui.filters.FastaSaveGuiFileFilter;
import org.jebtk.core.sys.SysUtils;
import org.jebtk.math.matrix.DataFrame;
import org.jebtk.modern.io.GuiFileExtFilter;

import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.toolbox.core.io.IOModule;

/**
 * Allow users to open and save Broad GCT files
 *
 * @author Antony Holmes
 *
 */
public class FastaWriterModule extends IOModule {
  private static final GuiFileExtFilter SAVE_FILTER = new FastaSaveGuiFileFilter();

  public FastaWriterModule() {
    registerFileSaveType(SAVE_FILTER);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.abh.lib.NameProperty#getName()
   */
  @Override
  public String getName() {
    return "Fasta Writer";
  }

  @Override
  public boolean write(final MainMatCalcWindow window, final Path file, final DataFrame m) throws IOException {

    List<Sequence> sequences = toSequences(window, m);

    if (sequences.size() == 0) {
      return false;
    }

    Fasta.write(file, sequences);

    return true;
  }

  public static List<Sequence> toSequences(final MainMatCalcWindow window, final DataFrame m) {

    int c1 = DataFrame.findColumn(m, "Name");

    if (c1 == -1) {
      c1 = DataFrame.findColumn(m, "DNA Location");
    }

    if (c1 == -1) {
      c1 = DataFrame.findColumn(m, "Location");
    }

    int c2 = DataFrame.findColumn(m, "DNA Sequence");

    if (c2 == -2) {
      return Collections.emptyList();
    }

    int c3 = DataFrame.findColumn(m, "Options");

    List<Sequence> sequences = new ArrayList<Sequence>(m.getRows());

    SysUtils.err().println("seq", c1, m.getText(0, c1));

    for (int i = 0; i < m.getRows(); ++i) {
      String name = c1 != -1 ? m.getText(i, c1) : "Seq" + (i + 1);

      // If we have some options describing how the sequence was
      // created, add those
      if (c3 != -1) {
        name += " " + m.getText(i, c3);
      }

      sequences.add(Sequence.create(name, m.getText(i, c2)));
    }

    return sequences;
  }
}
