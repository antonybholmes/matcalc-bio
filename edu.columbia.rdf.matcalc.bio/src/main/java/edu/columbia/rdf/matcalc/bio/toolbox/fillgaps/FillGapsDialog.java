package edu.columbia.rdf.matcalc.bio.toolbox.fillgaps;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Box;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.jebtk.core.collections.CollectionUtils;
import org.jebtk.math.external.microsoft.Excel;
import org.jebtk.math.ui.external.microsoft.ExcelDialog;
import org.jebtk.modern.AssetService;
import org.jebtk.modern.ModernWidget;
import org.jebtk.modern.UI;
import org.jebtk.modern.button.ModernButtonWidget;
import org.jebtk.modern.combobox.ModernComboBox;
import org.jebtk.modern.dialog.ModernDialogTaskWindow;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.io.RecentFilesService;
import org.jebtk.modern.list.ModernList;
import org.jebtk.modern.list.ModernListModel;
import org.jebtk.modern.panel.HBox;
import org.jebtk.modern.panel.ModernLineBorderPanel;
import org.jebtk.modern.panel.ModernPanel;
import org.jebtk.modern.panel.VBox;
import org.jebtk.modern.ribbon.RibbonButton;
import org.jebtk.modern.scrollpane.ModernScrollPane;
import org.jebtk.modern.text.ModernAutoSizeLabel;
import org.jebtk.modern.text.ModernClipboardNumericalTextField;
import org.jebtk.modern.text.ModernNumericalTextField;
import org.jebtk.modern.text.ModernSubHeadingLabel;
import org.jebtk.modern.text.ModernTextBorderPanel;
import org.jebtk.modern.window.ModernWindow;
import org.jebtk.modern.window.WindowWidgetFocusEvents;

public class FillGapsDialog extends ModernDialogTaskWindow {
  private static final long serialVersionUID = 1L;

  private Map<String, Path> mBedFileMap = null;

  private Map<String, Map<String, String>> mDescriptionMap;

  private Map<String, String> mNameMap = new TreeMap<String, String>();

  private ModernComboBox mAnnotationCombo = new ModernComboBox(new Dimension(300, ModernWidget.WIDGET_HEIGHT));

  private ModernList<String> mSampleList = new ModernList<String>();

  private ModernButtonWidget mLoadButton = new RibbonButton("Load...", AssetService.getInstance().loadIcon("open", 16));

  private ModernNumericalTextField mMeanZeroField = new ModernClipboardNumericalTextField(
      FillGapsModule.SEGMENT_MEAN_ZERO);

  private List<String> mSamples = null;

  public FillGapsDialog(ModernWindow parent, Map<String, Path> bedFileMap,
      Map<String, Map<String, String>> descriptionMap) {
    super(parent);

    mBedFileMap = bedFileMap;
    mDescriptionMap = descriptionMap;

    for (String name : mBedFileMap.keySet()) {
      mNameMap.put(mDescriptionMap.get(name).get("description"), name);
    }

    setTitle("Fill Gaps");

    createUi();

    setup();
  }

  private void setup() {
    addWindowListener(new WindowWidgetFocusEvents(mOkButton));

    mLoadButton.addClickListener(new ModernClickListener() {

      @Override
      public void clicked(ModernClickEvent e) {
        try {
          loadSamples();
        } catch (InvalidFormatException | IOException e1) {
          e1.printStackTrace();
        }
      }
    });

    setResizable(true);

    setSize(720, 520);

    UI.centerWindowToScreen(this);
  }

  private final void createUi() {
    Box content = VBox.create();

    content.add(new ModernSubHeadingLabel("Annotation"));

    content.add(ModernPanel.createVGap());

    for (String name : mNameMap.keySet()) {
      mAnnotationCombo.addScrollMenuItem(name);
    }

    content.add(mAnnotationCombo);

    content.add(UI.createVGap(30));

    content.add(new ModernSubHeadingLabel("Samples"));

    content.add(ModernPanel.createVGap());

    Box box = HBox.create();

    ModernLineBorderPanel panel = new ModernLineBorderPanel(new ModernScrollPane(mSampleList), new Dimension(500, 200));
    panel.setAlignmentY(TOP_ALIGNMENT);
    box.add(panel);

    box.add(ModernWidget.createHGap());

    Box box2 = VBox.create();
    box2.setAlignmentY(TOP_ALIGNMENT);
    box2.add(mLoadButton);

    box.add(box2);

    content.add(box);

    content.add(UI.createVGap(30));

    box2 = HBox.create();
    box2.add(new ModernAutoSizeLabel("Mean Zero", 100));
    box2.add(new ModernTextBorderPanel(mMeanZeroField, 100));

    content.add(box2);

    // content.setBorder(BorderService.getInstance().createBorder(10));

    setCard(content);
  }

  private void loadSamples() throws IOException, InvalidFormatException {
    Path file = ExcelDialog.open(mParent).xlsx().getFile(RecentFilesService.getInstance().getPwd());

    if (file == null) {
      return;
    }

    mSamples = CollectionUtils.sort(CollectionUtils.unique(Excel.getTextFromFile(file, true)));

    ModernListModel<String> model = new ModernListModel<String>();

    for (String sample : mSamples) {
      model.addValue(sample);
    }

    mSampleList.setModel(model);
  }

  public String getAnnotation() {
    return mNameMap.get(mAnnotationCombo.getText());
  }

  public List<String> getSamples() {
    return mSamples;
  }

  public double getMeanZero() throws ParseException {
    return mMeanZeroField.getDouble();
  }
}
