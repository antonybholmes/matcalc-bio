package edu.columbia.rdf.matcalc.bio.toolbox.probes;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jebtk.core.io.FileUtils;
import org.jebtk.core.io.PathUtils;
import org.jebtk.core.text.TextUtils;
import org.jebtk.math.matrix.DataFrame;
import org.jebtk.modern.AssetService;
import org.jebtk.modern.dialog.ModernDialogStatus;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.ribbon.RibbonLargeButton;

import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.toolbox.Module;

public class ProbeLocationsModule extends Module implements ModernClickListener {
  private MainMatCalcWindow mWindow;

  public static final Path PROBE_RES_DIR = PathUtils.getPath("res/modules/probes/locations");

  @Override
  public String getName() {
    return "Probe Locations";
  }

  @Override
  public void init(MainMatCalcWindow window) {
    mWindow = window;

    RibbonLargeButton button = new RibbonLargeButton("Probe Locations",
        AssetService.getInstance().loadIcon("probes", 24));

    button.addClickListener(this);
    mWindow.getRibbon().getToolbar("Bioinformatics").getSection("Probes").add(button);
  }

  @Override
  public void clicked(ModernClickEvent e) {
    try {
      addLocations();
    } catch (IOException ex) {
      ex.printStackTrace();
    } catch (ParseException e1) {
      e1.printStackTrace();
    }
  }

  private void addLocations() throws IOException, ParseException {
    DataFrame m = mWindow.getCurrentMatrix();

    ProbeLocationsDialog dialog = new ProbeLocationsDialog(mWindow, m);

    dialog.setVisible(true);

    if (dialog.getStatus() == ModernDialogStatus.CANCEL) {
      return;
    }

    Path file = PROBE_RES_DIR.resolve(dialog.getAnnotation() + ".probe_locations.txt.gz");

    BufferedReader reader = FileUtils.newBufferedReader(file);

    String line;
    List<String> tokens;
    String probe;
    String loc;

    Map<String, String> locMap = new HashMap<String, String>();

    try {
      reader.readLine();

      while ((line = reader.readLine()) != null) {
        tokens = TextUtils.tabSplit(line);

        probe = tokens.get(0);

        loc = tokens.get(1) + ":" + tokens.get(2) + "-" + tokens.get(3);

        locMap.put(probe, loc);
      }
    } finally {
      reader.close();
    }

    List<String> locs = new ArrayList<String>(m.getRows());

    for (String p : m.getIndex().getText(dialog.getAnnotation())) {
      if (locMap.containsKey(p)) {
        locs.add(locMap.get(p));
      } else {
        locs.add(TextUtils.NA);
      }
    }

    locMap.clear();

    DataFrame ml = new DataFrame(m);
    ml.getIndex().setAnnotation("Probe Location (hg19)", locs.toArray());

    mWindow.history().addToHistory("Probe Locations", ml);
  }
}
