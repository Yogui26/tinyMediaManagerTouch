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
import static org.tinymediamanager.ui.TmmFontHelper.L2;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.tinymediamanager.core.DateField;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.ai.AiException;
import org.tinymediamanager.core.ai.AiManager;
import org.tinymediamanager.license.License;
import org.tinymediamanager.thirdparty.trakttv.TraktTv;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.button.DocsButton;
import org.tinymediamanager.ui.components.button.SquareIconButton;
import org.tinymediamanager.ui.components.label.TmmLabel;
import org.tinymediamanager.ui.components.panel.CollapsiblePanel;
import org.tinymediamanager.ui.components.textfield.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.toast.TmmToastManager;

import net.miginfocom.swing.MigLayout;

/**
 * The class ExternalServicesSettingsPanel. Handle all settings for the external services
 * 
 * @author Manuel Laggner
 */
class ExternalServicesSettingsPanel extends JPanel {
  private final Settings settings = Settings.getInstance();

  private JButton        btnGetTraktPin;
  private JButton        btnTestTraktConnection;
  private JLabel         lblTraktStatus;
  private JPasswordField pfMdbListApiKey;
  private JComboBox      cbTraktDate;

  private JTextField     tfAiEndpointUrl;
  private JPasswordField pfAiApiKey;
  private JButton        btnTestAiConnection;
  private JCheckBox      chckbxAiRateLimit;
  private JSpinner       spAiMaxCallsPerMinute;
  private JSpinner       spAiMaxCallsPerHour;
  private JSpinner       spAiMinIntervalSeconds;
  private JLabel         lblAiTestResult;
  private JTextField     tfAiModel;

  ExternalServicesSettingsPanel() {
    // UI init
    initComponents();

    // data init
    if (License.getInstance().isValidLicense()
        && StringUtils.isNoneBlank(Settings.getInstance().getTraktAccessToken(), Settings.getInstance().getTraktRefreshToken())) {
      lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.good"));
    }
    else {
      lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.bad"));
    }

    btnGetTraktPin.addActionListener(e -> getTraktPin());
    btnGetTraktPin.setEnabled(License.getInstance().isValidLicense());
    btnTestTraktConnection.addActionListener(e -> {
      try {
        TraktTv.getInstance().refreshAccessToken();
        TmmToastManager.showSuccessToast(this, TmmResourceBundle.getString("Settings.trakttv"),
            TmmResourceBundle.getString("Settings.trakt.testconnection.good"));
      }
      catch (Exception e1) {
        TmmToastManager.showErrorToast(this, TmmResourceBundle.getString("Settings.trakttv"),
            TmmResourceBundle.getString("Settings.trakt.testconnection.bad"));
      }
    });
    btnTestTraktConnection.setEnabled(License.getInstance().isValidLicense());
    btnTestAiConnection.addActionListener(e -> testAiConnection());
  }

  private void getTraktPin() {
    // open the pin url in a browser
    try {
      TmmUIHelper.browseUrl("https://trakt.tv/pin/799");
    }
    catch (Exception e1) {
      // browser could not be opened, show a dialog box
      TmmToastManager.showErrorToast(this, TmmResourceBundle.getString("Settings.trakttv"),
          TmmResourceBundle.getString("Settings.trakt.getpin.fallback"));
    }

    // let the user insert the pin
    String pin = JOptionPane.showInputDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.trakt.getpin.entercode"));

    // user clicked abort
    if (pin == null || pin.isEmpty()) {
      return;
    }

    // try to get the tokens
    String accessToken = "";
    String refreshToken = "";
    try {
      Map<String, String> tokens = TraktTv.getInstance().authenticateViaPin(pin);
      accessToken = tokens.get("accessToken") == null ? "" : tokens.get("accessToken");
      refreshToken = tokens.get("refreshToken") == null ? "" : tokens.get("refreshToken");
    }
    catch (Exception ignored) {
      // ignored
    }

    if (StringUtils.isNoneBlank(accessToken, refreshToken)) {
      Settings.getInstance().setTraktAccessToken(accessToken);
      Settings.getInstance().setTraktRefreshToken(refreshToken);
      lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.good"));
    }
    else {
      JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.trakt.getpin.problem"),
          TmmResourceBundle.getString("Settings.trakt.getpin"), JOptionPane.ERROR_MESSAGE);

      if (StringUtils.isNoneBlank(Settings.getInstance().getTraktAccessToken(), Settings.getInstance().getTraktRefreshToken())) {
        // we got an error, but we already have old setted-up tokens, so display msg accordingly
        lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.good"));
      }
      else {
        lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.bad"));
      }
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[grow]", "[][15lp!][][15lp!][]"));
    {
      JPanel panelTrakt = new JPanel();
      panelTrakt.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][10lp!][]")); // 16lp ~ width of the

      JLabel lblTraktT = new TmmLabel(TmmResourceBundle.getString("Settings.trakt"), H3);

      if (!License.getInstance().isValidLicense()) {
        lblTraktT.setText("*PRO* " + lblTraktT.getText());
      }

      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelTrakt, lblTraktT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#trakttv"));
      add(collapsiblePanel, "cell 0 0,growx, wmin 0");
      {
        lblTraktStatus = new JLabel("");
        panelTrakt.add(lblTraktStatus, "cell 1 0 2 1");
      }
      {
        btnGetTraktPin = new JButton(TmmResourceBundle.getString("Settings.trakt.getpin"));
        panelTrakt.add(btnGetTraktPin, "cell 1 1 2 1");

        btnTestTraktConnection = new JButton(TmmResourceBundle.getString("Settings.trakt.testconnection"));
        panelTrakt.add(btnTestTraktConnection, "cell 1 1 2 1");
      }

      JLabel lblTraktDateT = new TmmLabel(TmmResourceBundle.getString("Settings.trakt.date"));
      panelTrakt.add(lblTraktDateT, "flowx,cell 1 3 2 1");

      cbTraktDate = new JComboBox(DateField.values());
      panelTrakt.add(cbTraktDate, "cell 1 3 2 1");
    }
    {
      JPanel panelMdbList = new JPanel();
      panelMdbList.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[]"));
      JLabel lblMdbListT = new TmmLabel(TmmResourceBundle.getString("Settings.external.rating.mdblist"), H3);

      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMdbList, lblMdbListT, true);
      add(collapsiblePanel, "cell 0 2,growx, wmin 0");
      {
        JLabel lblMdbListApiKeyT = new JLabel(TmmResourceBundle.getString("Settings.api.key"));
        panelMdbList.add(lblMdbListApiKeyT, "flowx,cell 1 0 2 1");

        pfMdbListApiKey = new JPasswordField();
        panelMdbList.add(pfMdbListApiKey, "cell 1 0 2 1");
        pfMdbListApiKey.setColumns(30);

        JButton btnCopyMdblistApiKey = new SquareIconButton(IconManager.COPY);
        btnCopyMdblistApiKey.setToolTipText(TmmResourceBundle.getString("Settings.apikey.copy.desc"));
        btnCopyMdblistApiKey.addActionListener(l -> {
          Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
          clipboard.setContents(new StringSelection(new String(pfMdbListApiKey.getPassword())), null);
          TmmToastManager.showSuccessToast(this, TmmResourceBundle.getString("Settings.apikey.copy.done"));
        });
        panelMdbList.add(btnCopyMdblistApiKey, "cell 1 0 2 1");

        JTextArea tpMediaPlayer = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.external.rating.mdblist.hint"));
        panelMdbList.add(tpMediaPlayer, "cell 1 1 2 1,growx, wmin 0");
        TmmFontHelper.changeFont(tpMediaPlayer, L2);
      }

    }
    {
      JPanel panelAi = new JPanel();
      panelAi.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][][300lp,grow][300lp,grow]", "[][][][][15lp!][][grow]"));
      JLabel lblAiT = new TmmLabel(TmmResourceBundle.getString("Settings.ai"), H3);
      if (!License.getInstance().isValidLicense()) {
        lblAiT.setText("*PRO* " + lblAiT.getText());
      }
      CollapsiblePanel collapsibleAi = new CollapsiblePanel(panelAi, lblAiT, true);
      collapsibleAi.addExtraTitleComponent(new DocsButton("/settings#ai"));
      add(collapsibleAi, "cell 0 4,growx, wmin 0");

      {
        JLabel lblEndpointUrl = new JLabel(TmmResourceBundle.getString("Settings.ai.endpointUrl"));
        panelAi.add(lblEndpointUrl, "cell 1 0 2 1,alignx right");
        tfAiEndpointUrl = new JTextField();
        tfAiEndpointUrl.setColumns(40);
        panelAi.add(tfAiEndpointUrl, "cell 3 0,growx");

        JLabel lblApiKey = new JLabel(TmmResourceBundle.getString("Settings.ai.apiKey"));
        panelAi.add(lblApiKey, "cell 1 1 2 1,alignx right");
        pfAiApiKey = new JPasswordField();
        pfAiApiKey.setColumns(30);
        panelAi.add(pfAiApiKey, "cell 3 1,growx");

        JButton btnCopyAiApiKey = new SquareIconButton(IconManager.COPY);
        btnCopyAiApiKey.setToolTipText(TmmResourceBundle.getString("Settings.apikey.copy.desc"));
        btnCopyAiApiKey.addActionListener(l -> {
          Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
          clipboard.setContents(new StringSelection(new String(pfAiApiKey.getPassword())), null);
          TmmToastManager.showSuccessToast(this, TmmResourceBundle.getString("Settings.apikey.copy.done"));
        });
        panelAi.add(btnCopyAiApiKey, "cell 4 1");

        JLabel lblAiModel = new JLabel(TmmResourceBundle.getString("Settings.ai.model"));
        panelAi.add(lblAiModel, "cell 1 2 2 1,alignx right");

        tfAiModel = new JTextField();
        panelAi.add(tfAiModel, "cell 3 2,growx");
        tfAiModel.setColumns(40);

        btnTestAiConnection = new JButton(TmmResourceBundle.getString("Settings.ai.testConnection"));
        btnTestAiConnection.setEnabled(License.getInstance().isValidLicense());
        panelAi.add(btnTestAiConnection, "cell 4 2");

        chckbxAiRateLimit = new JCheckBox(TmmResourceBundle.getString("Settings.ai.rateLimitEnabled"));
        panelAi.add(chckbxAiRateLimit, "cell 1 5 4 1");

        JPanel panel = new JPanel();
        panelAi.add(panel, "cell 2 6 3 1,grow");
        panel.setLayout(new MigLayout("insets 0", "[][]", "[][][]"));

        JLabel lblMaxPerMin = new JLabel(TmmResourceBundle.getString("Settings.ai.maxCallsPerMinute"));
        panel.add(lblMaxPerMin, "cell 0 0");
        spAiMaxCallsPerMinute = new JSpinner(new SpinnerNumberModel(50, 1, 200, 1));
        panel.add(spAiMaxCallsPerMinute, "cell 1 0,growx");

        JLabel lblMaxPerHour = new JLabel(TmmResourceBundle.getString("Settings.ai.maxCallsPerHour"));
        panel.add(lblMaxPerHour, "cell 0 1");
        spAiMaxCallsPerHour = new JSpinner(new SpinnerNumberModel(1000, 1, 5000, 10));
        panel.add(spAiMaxCallsPerHour, "cell 1 1,growx");

        JLabel lblMinInterval = new JLabel(TmmResourceBundle.getString("Settings.ai.minIntervalSeconds"));
        panel.add(lblMinInterval, "cell 0 2");
        spAiMinIntervalSeconds = new JSpinner(new SpinnerNumberModel(1, 0, 60, 1));
        panel.add(spAiMinIntervalSeconds, "cell 1 2,growx");
      }

      lblAiTestResult = new JLabel("");
      panelAi.add(lblAiTestResult, "cell 3 3 2 1");
    }
    initDataBindings();
  }

  private void testAiConnection() {
    btnTestAiConnection.setEnabled(false);
    lblAiTestResult.setText(TmmResourceBundle.getString("Settings.ai.testing"));

    new Thread(() -> {
      AiManager.getInstance().resetClient();
      try {
        boolean ok = AiManager.getInstance().testConnection();
        SwingUtilities.invokeLater(() -> {
          if (ok) {
            lblAiTestResult.setText(TmmResourceBundle.getString("Settings.ai.testSuccess"));
          }
          else {
            lblAiTestResult.setText(TmmResourceBundle.getString("Settings.ai.testFailed"));
          }
          btnTestAiConnection.setEnabled(true);
        });
      }
      catch (AiException e) {
        SwingUtilities.invokeLater(() -> {
          lblAiTestResult.setText(TmmResourceBundle.getString("Settings.ai.testFailed") + ": " + e.getMessage());
          btnTestAiConnection.setEnabled(true);
        });
      }
    }).start();
  }

  protected void initDataBindings() {
    Property settingsBeanProperty = BeanProperty.create("traktDateField");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty, cbTraktDate,
        jComboBoxBeanProperty);
    autoBinding.bind();
    //
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    Property jSpinnerBeanProperty = BeanProperty.create("enabled");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxAiRateLimit, jCheckBoxBeanProperty, spAiMaxCallsPerMinute,
        jSpinnerBeanProperty);
    autoBinding_1.bind();
    //
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxAiRateLimit, jCheckBoxBeanProperty, spAiMaxCallsPerHour,
        jSpinnerBeanProperty);
    autoBinding_2.bind();
    //
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxAiRateLimit, jCheckBoxBeanProperty, spAiMinIntervalSeconds,
        jSpinnerBeanProperty);
    autoBinding_3.bind();
    //
    Property settingsBeanProperty_2 = BeanProperty.create("aiEndpointUrl");
    Property jTextFieldBeanProperty_1 = BeanProperty.create("text");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_2, tfAiEndpointUrl,
        jTextFieldBeanProperty_1);
    autoBinding_4.bind();
    //
    Property settingsBeanProperty_1 = BeanProperty.create("aiApiKey");
    Property jPasswordFieldBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_1, pfAiApiKey,
        jPasswordFieldBeanProperty);
    autoBinding_5.bind();
    //
    Property settingsBeanProperty_3 = BeanProperty.create("mdbListApiKey");
    Property jPasswordFieldBeanProperty_1 = BeanProperty.create("text");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_3, pfMdbListApiKey,
        jPasswordFieldBeanProperty_1);
    autoBinding_6.bind();
    //
    Property settingsBeanProperty_4 = BeanProperty.create("aiModel");
    Property jTextFieldBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_4, tfAiModel,
        jTextFieldBeanProperty);
    autoBinding_7.bind();
  }
}
