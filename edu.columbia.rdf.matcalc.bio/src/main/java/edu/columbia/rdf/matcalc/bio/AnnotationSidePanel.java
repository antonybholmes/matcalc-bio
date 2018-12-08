package edu.columbia.rdf.matcalc.bio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;

import org.jebtk.bioinformatics.genomic.GenomeService;
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
 * @author Antony Holmes Holmes
 *
 */
public class AnnotationSidePanel extends ModernComponent {
  private static final long serialVersionUID = 1L;

  private CheckBox mSelectAllButton = new ModernCheckSwitch("Select All");

  private Map<CheckBox, String> mCheckMap = new HashMap<CheckBox, String>();
  private Map<CheckBox, String> mGenomeMap = new HashMap<CheckBox, String>();

  public AnnotationSidePanel() {

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
      for (String genome : AnnotationService.getInstance().genomes()) {

        box.add(new ModernSubHeadingLabel(genome));
        box.add(UI.createVGap(5));

        for (String name : AnnotationService.getInstance()
            .annotations(genome)) {

          ModernCheckSwitch button = new ModernCheckSwitch(name);
          button.setBorder(LEFT_BORDER);
          mGenomeMap.put(button, genome);
          mCheckMap.put(button, name);
          box.add(button);

        }

        box.add(UI.createVGap(20)); // collapsePane.addTab(genome, box);
      }
    } catch (IOException e1) {
      e1.printStackTrace();
    }

    // collapsePane.setExpanded(true);

    // box.setBorder(BORDER);

    setBody(new ModernScrollPane(box)
        .setHorizontalScrollBarPolicy(ScrollBarPolicy.NEVER));

    setBorder(DOUBLE_BORDER);

    mSelectAllButton.addClickListener(new ModernClickListener() {

      @Override
      public void clicked(ModernClickEvent e) {
        checkAll();
      }
    });

    // Set a default

    for (CheckBox button : mCheckMap.keySet()) {
      if (button.getText().equals("ucsc_refseq_hg19")) {
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

  public List<GenomeDatabase> getGenomes() {
    List<GenomeDatabase> ret = new ArrayList<GenomeDatabase>(mCheckMap.size());

    for (CheckBox button : mCheckMap.keySet()) {
      if (button.isSelected()) {
        ret.add(new GenomeDatabase(GenomeService.getInstance().guessGenome(mGenomeMap.get(button)), 
            button.getText()));
      }
    }

    return ret;
  }

  public GenomeDatabase getGenome() {
    List<GenomeDatabase> genomes = getGenomes();

    if (genomes.size() > 0) {
      return genomes.get(0);
    } else {
      return null;
    }
  }
}
