package edu.columbia.rdf.matcalc.bio.toolbox.probes;

import java.io.IOException;
import java.text.ParseException;

import org.jebtk.math.matrix.DataFrame;
import org.jebtk.math.matrix.MatrixGroup;
import org.jebtk.math.matrix.utils.MatrixOperations;
import org.jebtk.modern.UIService;
import org.jebtk.modern.dialog.ModernDialogStatus;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.ribbon.RibbonLargeButton;

import edu.columbia.rdf.matcalc.MainMatCalcWindow;
import edu.columbia.rdf.matcalc.toolbox.CalcModule;
import edu.columbia.rdf.matcalc.toolbox.core.collapse.CollapseDialog;
import edu.columbia.rdf.matcalc.toolbox.core.collapse.CollapseType;

public class CollapseModule extends CalcModule implements ModernClickListener {
  private MainMatCalcWindow mWindow;

  @Override
  public String getName() {
    return "Collapse";
  }

  @Override
  public void init(MainMatCalcWindow window) {
    mWindow = window;

    RibbonLargeButton button = new RibbonLargeButton("Collapse Rows", UIService.getInstance().loadIcon("collapse", 32),
        "Collapse Rows", "Collapse rows by annotation, e.g. probe ids.");
    button.addClickListener(this);
    mWindow.getRibbon().getToolbar("Annotation").getSection("Probes").add(button);
  }

  @Override
  public void clicked(ModernClickEvent e) {
    try {
      collapse();
    } catch (IOException ex) {
      ex.printStackTrace();
    } catch (ParseException e1) {
      e1.printStackTrace();
    }
  }

  private void collapse() throws IOException, ParseException {
    DataFrame m = mWindow.getCurrentMatrix();

    CollapseDialog dialog = new CollapseDialog(mWindow, m, mWindow.getGroups());

    dialog.setVisible(true);

    if (dialog.getStatus() == ModernDialogStatus.CANCEL) {
      return;
    }

    MatrixGroup group1 = dialog.getGroup1();
    MatrixGroup group2 = dialog.getGroup2();

    String collapseName = dialog.getCollapseName();
    CollapseType collapseType = dialog.getCollapseType();

    DataFrame c = null;

    switch (collapseType) {
    case MAX:
      c = MatrixOperations.collapseMax(m, collapseName);
      break;
    case MIN:
      c = MatrixOperations.collapseMin(m, collapseName);
      break;
    case MAX_STDEV:
      c = MatrixOperations.collapseMaxStdDev(m, collapseName);
      break;
    case MAX_MEAN:
      c = MatrixOperations.collapseMaxMean(m, collapseName);
      break;
    case MAX_MEDIAN:
      c = MatrixOperations.collapseMaxMedian(m, collapseName);
      break;
    case MAX_TSTAT:
      c = MatrixOperations.addTStat(m, group1, group2);
      mWindow.addToHistory("Add T-Stats", c);
      c = MatrixOperations.collapseMaxTStat(c, collapseName);
      break;
    default:
      c = m;
      break;
    }

    mWindow.addToHistory("Collapse rows", c);
  }
}
