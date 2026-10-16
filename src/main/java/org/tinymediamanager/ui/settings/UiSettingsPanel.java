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
package org.tinymediamanager.ui.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.ReleaseInfo;
import org.tinymediamanager.core.DateField;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.button.DocsButton;
import org.tinymediamanager.ui.components.combobox.LocaleComboBox;
import org.tinymediamanager.ui.components.label.LinkLabel;
import org.tinymediamanager.ui.components.label.TmmLabel;
import org.tinymediamanager.ui.components.panel.CollapsiblePanel;
import org.tinymediamanager.ui.components.textfield.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.toast.TmmToastManager;

import net.miginfocom.swing.MigLayout;

/**
 * The class UiSettingsPanel is used to display some UI related settings
 * 
 * @author Manuel Laggner
 */
class UiSettingsPanel extends JPanel {
  private static final Logger        LOGGER             = LoggerFactory.getLogger(UiSettingsPanel.class);
  private static final Integer[]     DEFAULT_FONT_SIZES = { 10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 26, 28 };

  private final Settings             settings           = Settings.getInstance();
  private final List<LocaleComboBox> locales            = new ArrayList<>();

  private JComboBox                  cbLanguage;
  private LinkLabel                  lblLinkTranslate;
  private JComboBox                  cbFontSize;
  private JComboBox                  cbFontFamily;
  private JCheckBox                  chckbxStoreWindowPreferences;
  private JCheckBox                  chckbxRememberLastSelectedModule;
  private JComboBox                  cbTheme;
  private JCheckBox                  chckbxShowMemory;
  private JComboBox                  cbDatefield;
  private JRadioButton               rbFileSizeH;
  private JRadioButton               rbFileSizeM;
  private JRadioButton               rbFileSizeCalculationMB;
  private JRadioButton               rbFileSizeCalculationMiB;
  private JRadioButton               rbImageChooserLastFolder;
  private JRadioButton               rbImageChooserEntityFolder;
  private JSpinner                   spUpdateInterval;
  private JCheckBox                  chckbxAutomaticUpdates;
  private JLabel                     lblUpdateHint;
  private CollapsiblePanel           collapsiblePanelUpdate;
  private JLabel                     lblDateFormatT;
  private JComboBox                  cbDateFormat;
  private JComboBox                  cbTimeFormat;
  private JLabel                     lblTimeFormatT;

  UiSettingsPanel() {
    LocaleComboBox actualLocale = null;
    LocaleComboBox fallbackLocale = null;
    Locale settingsLang = Utils.getLocaleFromLanguage(settings.getLanguage());
    for (Locale l : Utils.getLanguages()) {
      LocaleComboBox localeComboBox = new LocaleComboBox(l);
      locales.add(localeComboBox);
      if (l.equals(settingsLang)) {
        actualLocale = localeComboBox;
      }
      // match by langu only, if no direct match
      if (settingsLang.getLanguage().equals(l.getLanguage())) {
        fallbackLocale = localeComboBox;
      }
    }
    Collections.sort(locales);

    // ui init
    initComponents();
    initDataBindings();

    // data init
    if (actualLocale != null) {
      cbLanguage.setSelectedItem(actualLocale);
    }
    else {
      cbLanguage.setSelectedItem(fallbackLocale);
    }

    cbFontFamily.setSelectedItem(settings.getFontFamily());
    int index = cbFontFamily.getSelectedIndex();
    if (index < 0) {
      cbFontFamily.setSelectedItem("Dialog");
      index = cbFontFamily.getSelectedIndex();
    }
    if (index < 0) {
      cbFontFamily.setSelectedIndex(0);
    }
    cbFontSize.setSelectedItem(settings.getFontSize());
    index = cbFontSize.getSelectedIndex();
    if (index < 0) {
      cbFontSize.setSelectedIndex(0);
    }
    cbTheme.setSelectedItem(settings.getTheme());
    index = cbTheme.getSelectedIndex();
    if (index < 0) {
      cbTheme.setSelectedIndex(0);
    }

    lblLinkTranslate.addActionListener(arg0 -> {
      try {
        TmmUIHelper.browseUrl(lblLinkTranslate.getText());
      }
      catch (Exception e) {
        LOGGER.error("Could not open '{}' in browser - '{}'", lblLinkTranslate.getText(), e.getMessage());
        MessageManager.getInstance()
            .pushMessage(
                new Message(MessageLevel.ERROR, lblLinkTranslate.getText(), "message.erroropenurl", new String[] { ":", e.getLocalizedMessage() }));//$NON-NLS-2$
      }
    });

    ActionListener actionListener = e -> SwingUtilities.invokeLater(this::checkChanges);
    cbLanguage.addActionListener(actionListener);
    cbFontFamily.addActionListener(actionListener);
    cbFontSize.addActionListener(actionListener);
    cbTheme.addActionListener(actionListener);
    chckbxAutomaticUpdates.addActionListener(actionListener);

    settings.addPropertyChangeListener(evt -> {
      switch (evt.getPropertyName()) {
        case "theme":
          if (!settings.getTheme().equals(cbTheme.getSelectedItem())) {
            cbTheme.setSelectedItem(settings.getTheme());
          }
          break;

        case "fontSize":
          if (cbFontSize.getSelectedItem() != null && settings.getFontSize() != (Integer) cbFontSize.getSelectedItem()) {
            cbFontSize.setSelectedItem(settings.getFontSize());
          }
          break;

        case "fontFamily":
          if (!settings.getFontFamily().equals(cbFontFamily.getSelectedItem())) {
            cbFontFamily.setSelectedItem(settings.getFontFamily());
          }
          break;
      }
    });

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(rbImageChooserLastFolder);
    buttonGroup.add(rbImageChooserEntityFolder);

    if (settings.isImageChooserUseEntityFolder()) {
      rbImageChooserEntityFolder.setSelected(true);
    }
    else {
      rbImageChooserLastFolder.setSelected(true);
    }

    rbImageChooserLastFolder.addActionListener(actionListener);
    rbImageChooserEntityFolder.addActionListener(actionListener);

    buttonGroup = new ButtonGroup();
    buttonGroup.add(rbFileSizeH);
    buttonGroup.add(rbFileSizeM);

    if (settings.isFileSizeDisplayHumanReadable()) {
      rbFileSizeH.setSelected(true);
    }
    else {
      rbFileSizeM.setSelected(true);
    }

    rbFileSizeH.addActionListener(actionListener);
    rbFileSizeM.addActionListener(actionListener);

    buttonGroup = new ButtonGroup();
    buttonGroup.add(rbFileSizeCalculationMB);
    buttonGroup.add(rbFileSizeCalculationMiB);

    if (settings.isFileSizeBase10()) {
      rbFileSizeCalculationMB.setSelected(true);
    }
    else {
      rbFileSizeCalculationMiB.setSelected(true);
    }

    rbFileSizeCalculationMB.addActionListener(actionListener);
    rbFileSizeCalculationMiB.addActionListener(actionListener);

    if (!chckbxAutomaticUpdates.isSelected()) {
      lblUpdateHint.setText(TmmResourceBundle.getString("Settings.updatecheck.hint"));
    }

    // hide update related settings if we tmm.noupdate has been set
    if (!Globals.canCheckForUpdates() || ReleaseInfo.isNightly()) {
      collapsiblePanelUpdate.setVisible(false);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("hidemode 1", "[grow]", "[][15lp!][][15lp!][]"));
    {
      JPanel panelUiSettings = new JPanel();
      panelUiSettings.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][50lp][400lp]", "[][][][][5lp!][][][5lp!][][][]"));

      JLabel lblUiSettingsT = new TmmLabel(TmmResourceBundle.getString("Settings.ui"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelUiSettings, lblUiSettingsT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#general"));
      add(collapsiblePanel, "cell 0 0, growx, wmin 0");
      {
        JLabel lblLanguageT = new JLabel(TmmResourceBundle.getString("Settings.language"));
        panelUiSettings.add(lblLanguageT, "cell 1 0 3 1");

        cbLanguage = new JComboBox(locales.toArray());
        panelUiSettings.add(cbLanguage, "cell 1 0 3 1");

        JLabel lblLanguageHint = new JLabel(TmmResourceBundle.getString("tmm.helptranslate"));
        panelUiSettings.add(lblLanguageHint, "cell 2 1 2 1");

        lblLinkTranslate = new LinkLabel("https://www.reddit.com/r/tinyMediaManager/comments/kt2iyq/basic_information/");
        panelUiSettings.add(lblLinkTranslate, "cell 2 2 2 1, grow, wmin 0");
      }
      {
        JLabel lblThemeT = new JLabel(TmmResourceBundle.getString("Settings.uitheme"));
        panelUiSettings.add(lblThemeT, "cell 1 5 3 1");

        cbTheme = new JComboBox(new String[] { "Light", "Dark" });
        panelUiSettings.add(cbTheme, "cell 1 5 3 1");
      }
      {
        JLabel lblFontFamilyT = new JLabel(TmmResourceBundle.getString("Settings.fontfamily"));
        panelUiSettings.add(lblFontFamilyT, "cell 1 8 2 1");

        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        cbFontFamily = new JComboBox(env.getAvailableFontFamilyNames());
        panelUiSettings.add(cbFontFamily, "cell 3 8");

        JLabel lblFontSizeT = new JLabel(TmmResourceBundle.getString("Settings.fontsize"));
        panelUiSettings.add(lblFontSizeT, "cell 1 9 2 1");

        cbFontSize = new JComboBox(DEFAULT_FONT_SIZES);
        panelUiSettings.add(cbFontSize, "cell 3 9");

        JTextArea tpFontHint = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.fonts.hint"));
        panelUiSettings.add(tpFontHint, "cell 2 10 2 1,growx");
      }
    }

    {
      JPanel panelMisc = new JPanel();
      // 16lp ~ width of the
      panelMisc
          .setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][]", "[][][10lp!][][][10lp!][][][][10lp!][][][][10lp!][][][][10lp!][][]"));

      JLabel lblMiscT = new TmmLabel(TmmResourceBundle.getString("Settings.misc"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMisc, lblMiscT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#misc-settings"));
      add(collapsiblePanel, "cell 0 2,growx,wmin 0");
      {
        JLabel lblDatefield = new JLabel(TmmResourceBundle.getString("Settings.datefield"));
        panelMisc.add(lblDatefield, "cell 1 0 2 1");

        cbDatefield = new JComboBox(DateField.values());
        panelMisc.add(cbDatefield, "cell 1 0 2 1");

        JLabel lblDatefieldHint = new JLabel(TmmResourceBundle.getString("Settings.datefield.desc"));
        panelMisc.add(lblDatefieldHint, "cell 2 1");
      }
      {
        lblDateFormatT = new JLabel(TmmResourceBundle.getString("Settings.ui.date"));
        panelMisc.add(lblDateFormatT, "flowx,cell 1 3 2 1");

        cbDateFormat = new JComboBox(TmmDateFormat.DateFormatStyle.values());
        panelMisc.add(cbDateFormat, "cell 1 3 2 1");

        lblTimeFormatT = new JLabel(TmmResourceBundle.getString("Settings.ui.time"));
        panelMisc.add(lblTimeFormatT, "flowx,cell 1 4 2 1");

        cbTimeFormat = new JComboBox(TmmDateFormat.TimeFormatStyle.values());
        panelMisc.add(cbTimeFormat, "cell 1 4");
      }
      {
        JLabel lblFileSizeFormula = new JLabel(TmmResourceBundle.getString("Settings.filesize.formula"));
        panelMisc.add(lblFileSizeFormula, "cell 1 6 2 1");

        rbFileSizeCalculationMB = new JRadioButton(TmmResourceBundle.getString("Settings.filesize.1000"));
        panelMisc.add(rbFileSizeCalculationMB, "cell 2 7");

        rbFileSizeCalculationMiB = new JRadioButton(TmmResourceBundle.getString("Settings.filesize.1024"));
        panelMisc.add(rbFileSizeCalculationMiB, "cell 2 8");
      }
      {
        JLabel lblFileSizeT = new JLabel(TmmResourceBundle.getString("Settings.filesize"));
        panelMisc.add(lblFileSizeT, "cell 1 10 2 1");

        rbFileSizeH = new JRadioButton(TmmResourceBundle.getString("Settings.filesize.human"));
        panelMisc.add(rbFileSizeH, "cell 2 11");

        rbFileSizeM = new JRadioButton(TmmResourceBundle.getString("Settings.filesize.megabyte"));
        panelMisc.add(rbFileSizeM, "cell 2 12");
      }
      {
        JLabel lblImageChooserDefaultFolderT = new JLabel(TmmResourceBundle.getString("Settings.imagechooser.folder"));
        panelMisc.add(lblImageChooserDefaultFolderT, "cell 1 14 2 1");

        rbImageChooserLastFolder = new JRadioButton(TmmResourceBundle.getString("Settings.imagechooser.last"));
        panelMisc.add(rbImageChooserLastFolder, "cell 2 15");

        rbImageChooserEntityFolder = new JRadioButton(TmmResourceBundle.getString("Settings.imagechooser.entity"));
        panelMisc.add(rbImageChooserEntityFolder, "cell 2 16");
      }
      {
        chckbxStoreWindowPreferences = new JCheckBox(TmmResourceBundle.getString("Settings.storewindowpreferences"));
        panelMisc.add(chckbxStoreWindowPreferences, "cell 1 18 2 1");
      }
      {
        chckbxShowMemory = new JCheckBox(TmmResourceBundle.getString("Settings.showmemory"));
        panelMisc.add(chckbxShowMemory, "cell 1 19 2 1");
      }
      {
        chckbxRememberLastSelectedModule = new JCheckBox(TmmResourceBundle.getString("Settings.rememberlastmodule"));
        panelMisc.add(chckbxRememberLastSelectedModule, "cell 1 20 2 1");
      }
    }
    {
      JPanel panelUpdate = new JPanel();
      panelUpdate.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][]")); // 16lp ~ width of the

      JLabel lblUpdateT = new TmmLabel(TmmResourceBundle.getString("Settings.update"), H3);
      collapsiblePanelUpdate = new CollapsiblePanel(panelUpdate, lblUpdateT, true);
      collapsiblePanelUpdate.addExtraTitleComponent(new DocsButton("/settings#update"));
      add(collapsiblePanelUpdate, "cell 0 4,growx,wmin 0");

      {
        chckbxAutomaticUpdates = new JCheckBox(TmmResourceBundle.getString("Settings.updatecheck"));
        panelUpdate.add(chckbxAutomaticUpdates, "cell 1 0 2 1");
      }
      {
        JLabel lblUpdateInterval = new JLabel(TmmResourceBundle.getString("Settings.updatecheck.interval"));
        panelUpdate.add(lblUpdateInterval, "flowx,cell 2 1");

        spUpdateInterval = new JSpinner();
        spUpdateInterval.setModel(new SpinnerNumberModel(1, 1, 30, 1));
        panelUpdate.add(spUpdateInterval, "cell 2 1");
      }
      {
        lblUpdateHint = new JLabel("");
        TmmFontHelper.changeFont(lblUpdateHint, Font.BOLD);
        panelUpdate.add(lblUpdateHint, "cell 1 2 2 1");
      }
    }
  }

  /**
   * Check changes.
   */
  private void checkChanges() {
    LocaleComboBox loc = (LocaleComboBox) cbLanguage.getSelectedItem();
    if (loc != null) {
      Locale locale = loc.getLocale();
      Locale actualLocale = Utils.getLocaleFromLanguage(settings.getLanguage());
      if (!locale.equals(actualLocale)) {
        settings.setLanguage(locale.toString());
        TmmToastManager.showWarningToast(this, TmmResourceBundle.getString("Settings.language"),
            TmmResourceBundle.getString("Settings.languagehint"));
      }
    }

    // theme
    String theme = (String) cbTheme.getSelectedItem();
    if (!settings.getTheme().equals(theme)) {
      settings.setTheme(theme);
      try {
        TmmUIHelper.setTheme();
        TmmUIHelper.updateUI();
      }
      catch (Exception e) {
        TmmToastManager.showWarningToast(this, TmmResourceBundle.getString("Settings.uitheme"), TmmResourceBundle.getString("Settings.uitheme.hint"));
      }
    }

    // fonts
    String fontFamily = (String) cbFontFamily.getSelectedItem();
    Integer fontSize = (Integer) cbFontSize.getSelectedItem();
    if ((fontFamily != null && !fontFamily.equals(settings.getFontFamily())) || (fontSize != null && fontSize != settings.getFontSize())) {
      settings.setFontFamily(fontFamily);
      settings.setFontSize(fontSize);

      Font font = UIManager.getFont("defaultFont");
      Font newFont = new Font(fontFamily, font.getStyle(), fontSize);
      UIManager.put("defaultFont", newFont);

      TmmUIHelper.updateUI();
    }

    // image chooser folder
    settings.setImageChooserUseEntityFolder(rbImageChooserEntityFolder.isSelected());

    // file size calculation
    settings.setFileSizeBase10(rbFileSizeCalculationMB.isSelected());

    // file size display
    settings.setFileSizeDisplayHumanReadable(rbFileSizeH.isSelected());

    // update
    if (chckbxAutomaticUpdates.isSelected()) {
      lblUpdateHint.setText("");
    }
    else {
      lblUpdateHint.setText(TmmResourceBundle.getString("Settings.updatecheck.hint"));
    }
  }

  protected void initDataBindings() {
    Property settingsBeanProperty = BeanProperty.create("storeWindowPreferences");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty, chckbxStoreWindowPreferences,
        jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property settingsBeanProperty_1 = BeanProperty.create("showMemory");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_1, chckbxShowMemory,
        jCheckBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property settingsBeanProperty_7 = BeanProperty.create("rememberLastSelectedModule");
    AutoBinding autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_7,
        chckbxRememberLastSelectedModule, jCheckBoxBeanProperty);
    autoBinding_8.bind();
    //
    Property settingsBeanProperty_2 = BeanProperty.create("dateField");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_2, cbDatefield,
        jComboBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property jSpinnerBeanProperty = BeanProperty.create("enabled");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxAutomaticUpdates, jCheckBoxBeanProperty, spUpdateInterval,
        jSpinnerBeanProperty);
    autoBinding_3.bind();
    //
    Property settingsBeanProperty_3 = BeanProperty.create("enableAutomaticUpdate");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_3, chckbxAutomaticUpdates,
        jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property settingsBeanProperty_4 = BeanProperty.create("automaticUpdateInterval");
    Property jSpinnerBeanProperty_1 = BeanProperty.create("value");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_4, spUpdateInterval,
        jSpinnerBeanProperty_1);
    autoBinding_5.bind();
    //
    Property settingsBeanProperty_5 = BeanProperty.create("dateFormatStyle");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_5, cbDateFormat,
        jComboBoxBeanProperty);
    autoBinding_6.bind();
    //
    Property settingsBeanProperty_6 = BeanProperty.create("timeFormatStyle");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_6, cbTimeFormat,
        jComboBoxBeanProperty);
    autoBinding_7.bind();
  }
}
