package edu.columbia.rdf.matcalc.bio.toolbox.genepattern;

import java.awt.Dimension;
import java.nio.file.Path;
import java.text.ParseException;

import javax.swing.Box;

import org.jebtk.core.io.PathUtils;
import org.jebtk.core.text.TextUtils;
import org.jebtk.graphplot.figure.series.XYSeries;
import org.jebtk.graphplot.figure.series.XYSeriesGroup;
import org.jebtk.modern.AssetService;
import org.jebtk.modern.ModernWidget;
import org.jebtk.modern.UI;
import org.jebtk.modern.button.ModernButtonWidget;
import org.jebtk.modern.button.ModernCheckBox;
import org.jebtk.modern.combobox.ModernComboBox2;
import org.jebtk.modern.dialog.ModernDialogTaskWindow;
import org.jebtk.modern.event.ModernClickEvent;
import org.jebtk.modern.event.ModernClickListener;
import org.jebtk.modern.io.FileDialog;
import org.jebtk.modern.io.RecentFilesService;
import org.jebtk.modern.panel.HBox;
import org.jebtk.modern.panel.VBox;
import org.jebtk.modern.ribbon.RibbonButton;
import org.jebtk.modern.spinner.ModernCompactSpinner;
import org.jebtk.modern.text.ModernAutoSizeLabel;
import org.jebtk.modern.text.ModernTextBorderPanel;
import org.jebtk.modern.text.ModernTextField;
import org.jebtk.modern.window.ModernWindow;
import org.jebtk.modern.window.WindowWidgetFocusEvents;

import edu.columbia.rdf.matcalc.GroupsCombo;

public class PermuteDialog extends ModernDialogTaskWindow {
  private static final long serialVersionUID = 1L;

  private XYSeriesGroup mGroups;

  private GroupsCombo mGroup1Combo;

  private GroupsCombo mGroup2Combo;

  private ModernCompactSpinner mTextSize1 = new ModernCompactSpinner(1, 1000, 10);

  private ModernCompactSpinner mTextSize2 = new ModernCompactSpinner(1, 1000, 10);

  private ModernTextField mTextPrefix = new ModernTextField("cls_perm");

  private ModernCompactSpinner mTextPermutations = new ModernCompactSpinner(1, 100000, 10);

  private ModernTextField mTextLocation = new ModernTextField();

  private ModernCheckBox mCheckSampleWithReplacement = new ModernCheckBox("Sample with replacement");

  private ModernButtonWidget mChangeButton = new RibbonButton("Change...",
      AssetService.getInstance().loadIcon("open", 16));

  private Path mDir = null;

  public PermuteDialog(ModernWindow parent, XYSeriesGroup groups) {
    super(parent);

    mGroups = groups;

    setTitle("Permute CLS");

    createUi();

    setup();
  }

  private void setup() {
    addWindowListener(new WindowWidgetFocusEvents(mOkButton));

    mChangeButton.addClickListener(new ModernClickListener() {

      @Override
      public void clicked(ModernClickEvent e) {
        Path dir = FileDialog.openDir(mParent, RecentFilesService.getInstance().getPwd());

        if (dir != null) {
          mDir = dir;
          mTextLocation.setText(PathUtils.toString(dir));
        }
      }
    });

    mDir = RecentFilesService.getInstance().getPwd();
    mTextLocation.setEditable(false);
    mTextLocation.setText(PathUtils.toString(mDir));

    mTextPrefix.setText(TextUtils.replaceSpaces(mGroups.get(0).getName().trim()) + "_vs_"
        + TextUtils.replaceSpaces(mGroups.get(1).getName().trim()) + "_perm");

    mGroup2Combo.setSelectedIndex(1);

    setResizable(true);

    setSize(new Dimension(600, 360));

    UI.centerWindowToScreen(this);
  }

  private final void createUi() {
    mGroup1Combo = new GroupsCombo(mGroups);
    mGroup2Combo = new GroupsCombo(mGroups);

    Box box;

    Box content = VBox.create();

    box = HBox.create();
    box.add(new ModernAutoSizeLabel("Location", ModernWidget.STANDARD_SIZE));
    box.add(UI.createHGap(5));
    box.add(new ModernTextBorderPanel(mTextLocation, 300));
    box.add(UI.createHGap(5));
    box.add(mChangeButton);
    content.add(box);

    content.add(UI.createVGap(30));

    box = HBox.create();
    box.add(new ModernAutoSizeLabel("Group 1", ModernWidget.STANDARD_SIZE));
    box.add(UI.createHGap(5));
    box.add(mGroup1Combo);
    box.add(UI.createHGap(30));
    box.add(new ModernAutoSizeLabel("Size"));
    box.add(UI.createHGap(5));
    box.add(mTextSize1);
    content.add(box);
    content.add(UI.createVGap(5));

    box = HBox.create();
    box.add(new ModernAutoSizeLabel("Group 2", ModernWidget.STANDARD_SIZE));
    box.add(UI.createHGap(5));
    box.add(mGroup2Combo);
    box.add(UI.createHGap(30));
    box.add(new ModernAutoSizeLabel("Size"));
    box.add(UI.createHGap(5));
    box.add(mTextSize2);
    content.add(box);

    content.add(UI.createVGap(30));

    box = HBox.create();
    box.add(new ModernAutoSizeLabel("Prefix", ModernWidget.STANDARD_SIZE));
    box.add(UI.createHGap(5));
    box.add(new ModernTextBorderPanel(mTextPrefix, 200));
    content.add(box);

    content.add(UI.createVGap(30));

    box = HBox.create();
    box.add(new ModernAutoSizeLabel("Permutations", ModernWidget.STANDARD_SIZE));
    box.add(UI.createHGap(5));
    box.add(mTextPermutations);
    box.add(UI.createHGap(10));
    box.add(mCheckSampleWithReplacement);
    content.add(box);

    setContent(content);
  }

  public boolean getSampleWithReplacement() {
    return mCheckSampleWithReplacement.isSelected();
  }

  public int getSample1Size() throws ParseException {
    return mTextSize1.getIntValue();
  }

  public int getSample2Size() throws ParseException {
    return mTextSize2.getIntValue();
  }

  public String getPrefix() {
    return mTextPrefix.getText();
  }

  public int getPermutations() throws ParseException {
    return mTextPermutations.getIntValue();
  }

  /**
   * Gets the group1.
   *
   * @return the group1
   */
  public XYSeries getGroup1() {
    return getGroup(mGroups, mGroup1Combo);
  }

  /**
   * Gets the group2.
   *
   * @return the group2
   */
  public XYSeries getGroup2() {
    return getGroup(mGroups, mGroup2Combo);
  }

  private static XYSeries getGroup(XYSeriesGroup groups, ModernComboBox2 groupCombo) {
    if (groups == null || groups.getCount() == 0) {
      return null;
    }

    return groups.get(groupCombo.getSelectedIndex());
  }

  public Path getDir() {
    return mDir;
  }
}
