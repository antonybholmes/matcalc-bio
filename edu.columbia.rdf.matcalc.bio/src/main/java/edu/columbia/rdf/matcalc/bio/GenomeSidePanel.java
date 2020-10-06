package edu.columbia.rdf.matcalc.bio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Box;

import org.jebtk.bioinformatics.genomic.Genome;
import org.jebtk.core.collections.IterMap;
import org.jebtk.modern.ModernComponent;
import org.jebtk.modern.UI;
import org.jebtk.modern.button.ModernButtonGroup;
import org.jebtk.modern.button.ModernRadioButton;
import org.jebtk.modern.panel.VBox;
import org.jebtk.modern.scrollpane.ModernScrollPane;
import org.jebtk.modern.scrollpane.ScrollBarPolicy;
import org.jebtk.modern.text.ModernSubHeadingLabel;

/**
 * List available annotations organized by genome that a user can select from.
 * 
 * @author Antony Holmes
 *
 */
public class GenomeSidePanel extends ModernComponent {
  private static final long serialVersionUID = 1L;

  private Map<ModernRadioButton, String> mCheckMap = new HashMap<ModernRadioButton, String>();
  private Map<ModernRadioButton, Genome> mGenomeMap = new HashMap<ModernRadioButton, Genome>();

  public GenomeSidePanel() {
    // ModernSubCollapsePane collapsePane = new ModernSubCollapsePane();

    Box box = VBox.create();

    ModernButtonGroup g = new ModernButtonGroup();

    try {
      IterMap<String, IterMap<String, IterMap<String, Genome>>> gmap = Genome
          .sort(AnnotationService.getInstance().genomes());

      for (Entry<String, IterMap<String, IterMap<String, Genome>>> nameEntry : gmap) {
        String name = nameEntry.getKey();

        box.add(new ModernSubHeadingLabel(name));
        box.add(UI.createVGap(5));

        for (Entry<String, IterMap<String, Genome>> assemblyEntry : nameEntry.getValue()) {
          // String assembly = nameEntry.getKey();

          for (Entry<String, Genome> trackEntry : assemblyEntry.getValue()) {
            // String track = trackEntry.getKey();

            Genome genome = trackEntry.getValue();

            ModernRadioButton button = new ModernRadioButton(name);
            g.add(button);
            button.setBorder(LEFT_BORDER);
            mGenomeMap.put(button, genome);
            mCheckMap.put(button, name);
            box.add(button);
          }

          box.add(UI.createVGap(20)); // collapsePane.addTab(genome, box);
        }
      }
    } catch (IOException e1) {
      e1.printStackTrace();
    }

    // collapsePane.setExpanded(true);

    // box.setBorder(BORDER);

    setBody(new ModernScrollPane(box).setHorizontalScrollBarPolicy(ScrollBarPolicy.NEVER));

    setBorder(DOUBLE_BORDER);

    // Set a default

    for (ModernRadioButton button : mCheckMap.keySet()) {
      if (button.getText().equals("ucsc_refseq_hg19")) {
        button.setSelected(true);
        break;
      }
    }
  }

//  public List<GenomeDatabase> getGenomes() {
//    List<GenomeDatabase> ret = new ArrayList<GenomeDatabase>(mCheckMap.size());
//
//    for (ModernRadioButton button : mCheckMap.keySet()) {
//      if (button.isSelected()) {
//        ret.add(new GenomeDatabase(GenomeService.getInstance().guessGenome(mGenomeMap.get(button)), 
//            button.getText()));
//      }
//    }
//
//    return ret;
//  }
//
//  public GenomeDatabase getGenome() {
//    List<GenomeDatabase> genomes = getGenomes();
//
//    if (genomes.size() > 0) {
//      return genomes.get(0);
//    } else {
//      return null;
//    }
//  }

  public List<Genome> getGenomes() {
    List<Genome> ret = new ArrayList<Genome>(mCheckMap.size());

    for (ModernRadioButton button : mCheckMap.keySet()) {
      if (button.isSelected()) {
        ret.add(mGenomeMap.get(button));
      }
    }

    return ret;
  }

  public Genome getGenome() {
    List<Genome> genomes = getGenomes();

    if (genomes.size() > 0) {
      return genomes.get(0);
    } else {
      return null;
    }
  }
}
