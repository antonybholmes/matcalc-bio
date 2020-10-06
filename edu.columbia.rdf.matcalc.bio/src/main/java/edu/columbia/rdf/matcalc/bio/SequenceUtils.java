package edu.columbia.rdf.matcalc.bio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jebtk.bioinformatics.genomic.Chromosome;
import org.jebtk.bioinformatics.genomic.ChromosomeService;
import org.jebtk.bioinformatics.genomic.Genome;
import org.jebtk.bioinformatics.genomic.GenomicRegion;
import org.jebtk.bioinformatics.genomic.Sequence;
import org.jebtk.math.matrix.DataFrame;

public class SequenceUtils {
  private SequenceUtils() {
    // Do nothing
  }

  public static List<SearchSequence> matrixToSequences(Genome genome, DataFrame m) {
    int dnaLocationColumn = -1;
    int chrCol = -1;
    int startCol = -1;
    int endCol = -1;
    int idCol = -1;

    // Find a column refering to a genomic location
    for (int i = 0; i < m.getCols(); ++i) {
      if (GenomicRegion.isGenomicRegion(m.getText(0, i))) {
        dnaLocationColumn = i;
        break;
      }
    }

    // If this is not found test whether we have chr, start and end columns
    if (dnaLocationColumn == -1) {
      for (int i = 0; i < m.getCols(); ++i) {
        if (Chromosome.isChr(m.getText(0, i))) {
          chrCol = i;
          break;
        }
      }

      if (chrCol != -1) {
        startCol = chrCol + 1;
        endCol = chrCol + 2;
      }
    }

    // Last resort pick the first column as an id
    if (chrCol == -1) {
      idCol = 0;
    }

    int dnaColumn = -1;

    for (int i = 0; i < m.getCols(); ++i) {
      if (isDna(m.getText(0, i))) {
        dnaColumn = i;
        break;
      }
    }

    if (dnaColumn == -1) {
      return Collections.emptyList();
    }

    List<SearchSequence> sequences = new ArrayList<SearchSequence>(m.getRows());

    if (dnaLocationColumn != -1) {
      for (int i = 0; i < m.getRows(); ++i) {
        GenomicRegion region = GenomicRegion.parse(genome, m.getText(i, dnaLocationColumn));
        String dna = m.getText(i, dnaColumn);

        sequences.add(new SearchSequence(region, Sequence.create(dna)));
      }
    } else if (dnaLocationColumn != -1) {
      for (int i = 0; i < m.getRows(); ++i) {
        GenomicRegion region = GenomicRegion.create(ChromosomeService.getInstance().chr(genome, m.getText(i, chrCol)),
            (int) m.getValue(i, startCol), (int) m.getValue(i, endCol));

        String dna = m.getText(i, dnaColumn);

        sequences.add(new SearchSequence(region, Sequence.create(dna)));
      }
    } else {
      for (int i = 0; i < m.getRows(); ++i) {
        String id = m.getText(i, idCol);

        String dna = m.getText(i, dnaColumn);

        sequences.add(new SearchSequence(id, Sequence.create(dna)));
      }
    }

    return sequences;
  }

  /**
   * The DNA sequence must be at least 10 bp long to be considered useful. This is
   * to stop short labels in columns such as 'a' from being misinterpreted as DNA.
   * 
   * @param text
   * @return
   */
  public static boolean isDna(String text) {
    return Sequence.DNA_REGEX.matcher(text).matches() && text.length() > 5;
  }

}
