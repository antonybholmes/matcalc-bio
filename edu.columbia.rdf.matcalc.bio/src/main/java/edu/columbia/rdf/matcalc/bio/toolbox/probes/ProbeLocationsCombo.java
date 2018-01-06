package edu.columbia.rdf.matcalc.bio.toolbox.probes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jebtk.core.io.FileUtils;
import org.jebtk.core.io.PathUtils;
import org.jebtk.modern.combobox.ModernComboBox;

public class ProbeLocationsCombo extends ModernComboBox {

  private static final long serialVersionUID = 1L;

  public ProbeLocationsCombo() {
    try {
      List<Path> files = FileUtils.findAll(ProbeLocationsModule.PROBE_RES_DIR, "probe_locations.txt.gz");

      for (Path file : files) {
        addMenuItem(PathUtils.getNameNoExt(file));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
