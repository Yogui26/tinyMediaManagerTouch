/*
 * Copyright 2012 - 2026 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.ui.components.table;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.CustomColumn;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.panels.AbstractModalInputPanel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link CustomColumnEditorPanel} is the popup to configure one freely configurable column of a table. It shows a field for every hierarchy
 * level the table supports.
 *
 * @param <C>
 *          the concrete type of the custom column bean
 * @author Manuel Laggner
 */
public class CustomColumnEditorPanel<C extends CustomColumn> extends AbstractModalInputPanel {
  private final C                          customColumnToEdit;
  private final List<CustomColumnLevel<C>> levels;

  private final JTextField                 tfTitle;
  private final List<JTextField>           patternFields = new ArrayList<>();

  /**
   * @param customColumnToEdit
   *          the column to configure
   * @param titleHintKey
   *          the resource bundle key of the tooltip shown for the header title field
   * @param levels
   *          the hierarchy levels this column carries a JMTE pattern for, in the order they should be shown
   * @param explorerFactory
   *          creates the JMTE explorer for the given owner window
   */
  public CustomColumnEditorPanel(C customColumnToEdit, String titleHintKey, List<CustomColumnLevel<C>> levels,
      Function<Window, JDialog> explorerFactory) {
    super();
    this.customColumnToEdit = customColumnToEdit;
    this.levels = levels;

    StringBuilder rowSpec = new StringBuilder();
    for (int i = 0; i < levels.size() + 2; i++) {
      rowSpec.append("[]");
    }
    setLayout(new MigLayout("", "[][400lp,grow]", rowSpec.toString()));

    int row = 0;

    JLabel lblTitleT = new JLabel(TmmResourceBundle.getString("metatag.title"));
    add(lblTitleT, "cell 0 " + row + ",alignx trailing");

    tfTitle = new JTextField();
    tfTitle.setToolTipText(TmmResourceBundle.getString(titleHintKey));
    add(tfTitle, "cell 1 " + row++ + ",growx");

    for (CustomColumnLevel<C> level : levels) {
      JLabel lblPatternT = new JLabel(level.getLabel());
      add(lblPatternT, "cell 0 " + row + ",alignx trailing");

      JTextField tfPattern = new JTextField();
      tfPattern.setToolTipText(level.getHint());
      add(tfPattern, "cell 1 " + row++ + ",growx");
      patternFields.add(tfPattern);
    }

    JButton btnJmteExplorer = new JButton(TmmResourceBundle.getString("jmteexplorer.title"));
    // the pattern is meant for a table column, not for the renamer - so no renamer profile import/export in the explorer
    btnJmteExplorer.addActionListener(e -> explorerFactory.apply(SwingUtilities.getWindowAncestor(this)).setVisible(true));
    add(btnJmteExplorer, "cell 1 " + row + ",alignx trailing");

    tfTitle.setText(StringUtils.defaultString(customColumnToEdit.getTitle()));
    for (int i = 0; i < levels.size(); i++) {
      patternFields.get(i).setText(StringUtils.defaultString(levels.get(i).getPattern(customColumnToEdit)));
    }

    SwingUtilities.invokeLater(this::requestFocusOnFirstEmptyPattern);
  }

  private void requestFocusOnFirstEmptyPattern() {
    for (JTextField tfPattern : patternFields) {
      if (StringUtils.isBlank(tfPattern.getText())) {
        tfPattern.requestFocus();
        return;
      }
    }
    if (!patternFields.isEmpty()) {
      patternFields.get(0).requestFocus();
    }
  }

  @Override
  protected void onClose() {
    // a column without any pattern at all would only be an empty one
    if (patternFields.stream().noneMatch(tfPattern -> StringUtils.isNotBlank(tfPattern.getText()))) {
      JOptionPane.showMessageDialog(this, TmmResourceBundle.getString("message.missingitems"));
      return;
    }

    customColumnToEdit.setTitle(tfTitle.getText());
    for (int i = 0; i < levels.size(); i++) {
      levels.get(i).setPattern(customColumnToEdit, patternFields.get(i).getText());
    }
    setVisible(false);
  }
}
