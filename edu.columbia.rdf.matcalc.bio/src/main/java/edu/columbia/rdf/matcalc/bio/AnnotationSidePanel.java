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
import org.jebtk.modern.button.CheckBox;
import org.jebtk.modern.button.ModernCheckSwitch;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
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
public class AnnotationSidePanel extends ModernComponent {
  private static final long serialVersionUID = 1L;

  private CheckBox mSelectAllButton = new ModernCheckSwitch("Select All");

  private Map<CheckBox, String> mCheckMap = new HashMap<CheckBox, String>();
  private Map<CheckBox, Genome> mGenomeMap = new HashMap<CheckBox, Genome>();

  public AnnotationSidePanel() {
    this("ucsc_refseq_hg19");
  }

  public AnnotationSidePanel(String defaultAnnotation) {

    Box box = VBox.create();

    // box.add(new ModernSubHeadingLabel("Genomes"));
    box.add(UI.createVGap(10));
    box.add(mSelectAllButton);
    box.add(UI.createVGap(20));

    setHeader(box);

    // ModernSubCollapsePane collapsePane = new ModernSubCollapsePane();

    box = VBox.create();

    // If two services provide the same genome, use the later.
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
            String track = trackEntry.getKey();

            Genome genome = trackEntry.getValue();

            ModernCheckSwitch button = new ModernCheckSwitch(track);
            button.setBorder(LEFT_BORDER);
            mGenomeMap.put(button, genome);
            mCheckMap.put(button, track);
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

    mSelectAllButton.addClickListener(new ModernClickListener() {

      @Override
      public void clicked(ModernClickEvent e) {
        checkAll();
      }
    });

    // Set a default

    for (CheckBox button : mCheckMap.keySet()) {
      if (button.getText().equals(defaultAnnotation)) {
        button.setSelected(true);
        break;
      }
    }
  }

  private void checkAll() {
    for (CheckBox button : mCheckMap.keySet()) {
      button.setSelected(mSelectAllButton.isSelected());
    }
  }

  public List<Genome> getGenomes() {
    List<Genome> ret = new ArrayList<Genome>(mCheckMap.size());

    for (CheckBox button : mCheckMap.keySet()) {
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
