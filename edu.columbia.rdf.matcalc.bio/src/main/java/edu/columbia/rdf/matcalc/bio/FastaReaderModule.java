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
import java.util.Collection;
import java.util.List;

import org.jebtk.bioinformatics.Fasta;
import org.jebtk.bioinformatics.dna.Sequence;
import org.jebtk.bioinformatics.genomic.GenomeService;
import org.jebtk.bioinformatics.genomic.GenomicRegion;
import org.jebtk.bioinformatics.ui.filters.FastaGuiFileFilter;
import org.jebtk.math.matrix.DataFrame;

import edu.columbia.rdf.matcalc.FileType;
import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.toolbox.CalcModule;

/**
 * Allow users to open and save Broad GCT files
 *
 * @author Antony Holmes Holmes
 *
 */
public class FastaReaderModule extends CalcModule {
  public FastaReaderModule() {
    registerFileOpenType(FastaGuiFileFilter.FASTA_FILTER);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.abh.lib.NameProperty#getName()
   */
  @Override
  public String getName() {
    return "FASTA";
  }

  @Override
  public DataFrame autoOpenFile(final MainMatCalcWindow window,
      final Path file,
      FileType type,
      int headers,
      int rowAnnotations,
      String delimiter,
      Collection<String> skipLines) throws IOException {
    return toMatrix(file);
  }

  public static DataFrame toMatrix(Path file) throws IOException {
    return toMatrix(GenomeService.getInstance().guessGenome(file), Fasta.parse(file));
  }

  public static DataFrame toMatrix(String genome, List<Sequence> sequences) {

    DataFrame ret = DataFrame.createMixedMatrix(sequences.size(), 3);

    GenomicRegion.parse(genome, sequences.get(0).getName());

    ret.setColumnName(0, "Name");
    ret.setColumnName(1, "Location");
    ret.setColumnName(2, "DNA Sequence");

    for (int i = 0; i < sequences.size(); ++i) {
      Sequence s = sequences.get(i);

      String name = s.getName();

      ret.set(i, 0, name);

      GenomicRegion r = GenomicRegion.parse(genome, name);

      if (r != null) {
        ret.set(i, 1, r.getLocation());
      } else {
        ret.set(i, 1, name);
      }

      ret.set(i, 2, s.toString());
    }

    return ret;
  }
}
