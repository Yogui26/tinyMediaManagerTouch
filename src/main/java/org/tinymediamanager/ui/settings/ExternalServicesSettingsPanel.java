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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.DateField;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.license.License;
import org.tinymediamanager.thirdparty.simkl.Simkl;
import org.tinymediamanager.thirdparty.simkl.SimklAuthResult;
import org.tinymediamanager.thirdparty.simkl.model.SimklDeviceCodeResponse;
import org.tinymediamanager.thirdparty.trakttv.TraktTv;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.button.DocsButton;
import org.tinymediamanager.ui.components.label.LinkLabel;
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
  private static final Logger LOGGER                  = LoggerFactory.getLogger(ExternalServicesSettingsPanel.class);

  /** the seconds to wait between two Simkl device flow polls, when the API does not tell us */
  private static final int    SIMKL_POLL_INTERVAL_SEC = 5;

  /** give up the Simkl device flow earlier than the 15 minutes a device code stays valid */
  private static final int    SIMKL_MAX_WAIT_SEC      = 180;

  /** the seconds to add to the poll interval, when Simkl answers with "slow_down" */
  private static final int    SIMKL_SLOW_DOWN_SEC     = 5;

  /** how many failed device flow polls in a row are accepted before the sign-in gives up */
  private static final int    SIMKL_MAX_POLL_ERRORS   = 3;

  private final Settings      settings                = Settings.getInstance();

  private JButton             btnGetTraktPin;
  private JButton             btnTestTraktConnection;
  private JLabel              lblTraktStatus;
  private JTextField          tfMdbListApiKey;
  private JComboBox           cbTraktDate;

  private JButton             btnConnectSimkl;
  private JButton             btnTestSimklConnection;
  private JButton             btnDisconnectSimkl;
  private JLabel              lblSimklStatus;
  private JCheckBox           chckbxTraktTv;
  private JCheckBox           chckbxSimklCom;

  ExternalServicesSettingsPanel() {
    // UI init
    initComponents();

    // data init
    if (License.getInstance().isValidLicense()) {
      chckbxTraktTv.setEnabled(true);
      chckbxSimklCom.setEnabled(true);

      if (StringUtils.isNoneBlank(TraktTv.getInstance().getAccessToken(), TraktTv.getInstance().getRefreshToken())) {
        lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.good"));
      }
      else {
        lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.bad"));
      }

      updateSimklStatus();
    }
    else {
      chckbxTraktTv.setEnabled(false);
      chckbxSimklCom.setEnabled(false);
    }

    btnGetTraktPin.addActionListener(e -> getTraktPin());
    btnTestTraktConnection.addActionListener(e -> {
      try {
        TraktTv.getInstance().refreshAccessToken();
        TmmToastManager.showSuccessToast(this, TmmResourceBundle.getString("Settings.trakttv"),
            TmmResourceBundle.getString("Settings.trakt.testconnection.good"));
      }
      catch (Exception e1) {
        TmmToastManager.showErrorToast(this, TmmResourceBundle.getString("Settings.trakttv"),
            TmmResourceBundle.getString("Settings.trakt.testconnection.bad"));

        // no connection possible, clear the tokens
        TraktTv.getInstance().setAccessToken("");
        TraktTv.getInstance().setRefreshToken("");
      }
    });

    btnConnectSimkl.addActionListener(e -> connectSimkl());
    btnTestSimklConnection.addActionListener(e -> {
      if (Simkl.getInstance().testConnection()) {
        TmmToastManager.showSuccessToast(this, TmmResourceBundle.getString("Settings.simkl"),
            TmmResourceBundle.getString("Settings.simkl.testconnection.good"));
      }
      else {
        // a broken grant has already been cleared by the API calls themselves - a transient error must not throw away the tokens
        TmmToastManager.showErrorToast(this, TmmResourceBundle.getString("Settings.simkl"),
            TmmResourceBundle.getString("Settings.simkl.testconnection.bad"));
      }
      updateSimklStatus();
    });

    btnDisconnectSimkl.addActionListener(e -> {
      // revoking the grant is a network call - keep it off the EDT
      Thread disconnect = new Thread(() -> {
        Simkl.getInstance().disconnect();
        SwingUtilities.invokeLater(this::updateSimklStatus);
        SwingUtilities.invokeLater(() -> TmmToastManager.showToast(ExternalServicesSettingsPanel.this, TmmResourceBundle.getString("Settings.simkl"),
            TmmResourceBundle.getString("Settings.simkl.disconnected")));
      }, "SimklDisconnect");
      disconnect.setDaemon(true);
      disconnect.start();
    });
  }

  private void getTraktPin() {
    TraktTv.getInstance().setAccessToken("");
    TraktTv.getInstance().setRefreshToken("");

    TraktTv traktTv = TraktTv.getInstance();

    String deviceCode = "";
    String url;
    // open the pin url in a browser
    try {
      Map<String, String> response = traktTv.getDeviceCode();
      deviceCode = response.get("DEVICE_CODE");
      String authUrl = response.get("AUTH_URL");
      String userCode = response.get("USER_CODE");
      if (StringUtils.isNoneBlank(deviceCode, authUrl, userCode)) {
        url = authUrl + "/" + userCode;
        TmmUIHelper.browseUrl(url);
      }
      else {
        throw new Exception("No verification url and user code received");
      }
    }
    catch (Exception ex) {
      // browser could not be opened, show a dialog box
      TmmToastManager.showErrorToast(this, TmmResourceBundle.getString("Settings.trakttv"),
          TmmResourceBundle.getString("Settings.trakt.getpin.error"));
      LOGGER.error("Error occurred while trying to get Trakt.tv access code - '{}'", ex.getMessage());
      return;
    }

    // let the user insert the pin
    String accessToken = "";
    String refreshToken = "";

    int retryCount = 0;
    while (retryCount < 5) {
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(new JLabel(TmmResourceBundle.getString("Settings.trakt.getpin.desc")), BorderLayout.NORTH);
      LinkLabel linkLabel = new LinkLabel(url);
      linkLabel.addActionListener(e -> {
        try {
          TmmUIHelper.browseUrl(url);
        }
        catch (Exception ignored) {
          // ignored
        }
      });
      linkLabel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

      panel.add(linkLabel, BorderLayout.CENTER);
      panel.add(new JLabel(TmmResourceBundle.getString("Settings.trakt.getpin.desc2")), BorderLayout.SOUTH);

      int answer = JOptionPane.showConfirmDialog(MainWindow.getFrame(), panel, TmmResourceBundle.getString("Settings.trakttv"),
          JOptionPane.OK_CANCEL_OPTION);

      // user clicked abort
      if (answer == JOptionPane.OK_OPTION) {
        // try to get access token
        try {
          Map<String, String> tokens = traktTv.getToken(deviceCode);
          accessToken = tokens.get("accessToken") == null ? "" : tokens.get("accessToken");
          refreshToken = tokens.get("refreshToken") == null ? "" : tokens.get("refreshToken");

          if (StringUtils.isNoneBlank(accessToken, refreshToken)) {
            break;
          }

          retryCount++;
          TmmToastManager.showErrorToast(this, TmmResourceBundle.getString("Settings.trakttv"),
              TmmResourceBundle.getString("Settings.trakt.getpin.error"));
        }
        catch (Exception ignored) {
          // ignored
        }
      }
      else if (answer == JOptionPane.CANCEL_OPTION) {
        lblTraktStatus.setText(null);
        return;
      }
    }

    if (StringUtils.isNoneBlank(accessToken, refreshToken)) {
      TraktTv.getInstance().setAccessToken(accessToken);
      TraktTv.getInstance().setRefreshToken(refreshToken);
      lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.good"));
      TmmToastManager.showSuccessToast(this, TmmResourceBundle.getString("Settings.trakttv"),
          TmmResourceBundle.getString("Settings.trakt.getpin.success"));
    }
    else {
      JOptionPane.showMessageDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.trakt.getpin.problem"),
          TmmResourceBundle.getString("Settings.trakt.getpin"), JOptionPane.ERROR_MESSAGE);
      lblTraktStatus.setText(TmmResourceBundle.getString("Settings.trakt.status.bad"));
    }
  }

  private void connectSimkl() {
    Simkl simkl = Simkl.getInstance();

    // step 1: ask Simkl.com for a device code
    SimklDeviceCodeResponse deviceCode;
    try {
      deviceCode = simkl.getDeviceCode();
    }
    catch (Exception ex) {
      TmmToastManager.showErrorToast(this, TmmResourceBundle.getString("Settings.simkl"),
          TmmResourceBundle.getString("Settings.simkl.connect.error"));
      LOGGER.error("Error occurred while trying to get the Simkl.com device code - '{}'", ex.getMessage());
      return;
    }

    // the pre-filled URL saves the user from typing the code
    final String url = StringUtils.isNotBlank(deviceCode.verification_uri_complete) ? deviceCode.verification_uri_complete
        : StringUtils.trimToEmpty(deviceCode.verification_uri);
    if (StringUtils.isBlank(url)) {
      TmmToastManager.showErrorToast(this, TmmResourceBundle.getString("Settings.simkl"),
          TmmResourceBundle.getString("Settings.simkl.connect.error"));
      LOGGER.error("No verification URL received from Simkl.com");
      return;
    }

    // step 2: the user approves in the browser - opening it is best effort, since the link is shown in the dialog as well
    try {
      TmmUIHelper.browseUrl(url);
    }
    catch (Exception e) {
      LOGGER.info("Could not open the browser for the Simkl.com approval page - '{}'", e.getMessage());
    }

    final AtomicBoolean aborted = new AtomicBoolean(false);
    final AtomicBoolean cancelled = new AtomicBoolean(false);
    final AtomicBoolean approved = new AtomicBoolean(false);
    final AtomicReference<String> failureKey = new AtomicReference<>();
    final AtomicReference<Thread> pollerRef = new AtomicReference<>();

    final JDialog dialog = new JDialog(MainWindow.getFrame(), TmmResourceBundle.getString("Settings.simkl"), true);
    dialog.setMinimumSize(new Dimension(250, 250));
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    final JLabel statusLabel = new JLabel(TmmResourceBundle.getString("Settings.simkl.connect.waiting"));
    statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

    JPanel details = new JPanel(new MigLayout("insets 0, hidemode 3", "[grow]"));
    details.add(new JLabel(TmmResourceBundle.getString("Settings.simkl.connect.code") + ": " + StringUtils.trimToEmpty(deviceCode.user_code)),
        "cell 0 0");

    LinkLabel linkLabel = new LinkLabel(url);
    linkLabel.addActionListener(e -> {
      try {
        TmmUIHelper.browseUrl(url);
      }
      catch (Exception ignored) {
        // ignored
      }
    });
    linkLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
    details.add(linkLabel, "cell 0 1");
    details.add(statusLabel, "cell 0 2, alignx right");

    JTextArea description = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.simkl.connect.desc"));
    description.setLineWrap(true);
    description.setWrapStyleWord(true);
    description.setColumns(50);

    JPanel content = new JPanel(new BorderLayout(0, 10));
    content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    content.add(description, BorderLayout.NORTH);
    content.add(details, BorderLayout.CENTER);

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
    btnCancel.addActionListener(e -> {
      cancelled.set(true);
      aborted.set(true);
      // wake the poller out of its wait, so it stops right away instead of after the next interval
      Thread poller = pollerRef.get();
      if (poller != null) {
        poller.interrupt();
      }
      dialog.dispose();
    });
    buttons.add(btnCancel);
    content.add(buttons, BorderLayout.SOUTH);

    dialog.setContentPane(content);
    dialog.pack();
    dialog.setLocationRelativeTo(MainWindow.getFrame());

    // step 3: poll for the approval in the background, so the dialog stays responsive and closes automatically
    Thread poller = new Thread(() -> pollSimklApproval(deviceCode, statusLabel, aborted, approved, failureKey, dialog), "SimklDeviceFlow");
    poller.setDaemon(true);
    pollerRef.set(poller);
    poller.start();

    dialog.setVisible(true);
    aborted.set(true);
    poller.interrupt();

    if (approved.get()) {
      TmmToastManager.showSuccessToast(this, TmmResourceBundle.getString("Settings.simkl"),
          TmmResourceBundle.getString("Settings.simkl.connect.success"));
      if (!Strings.CI.contains(simkl.getScope(), "media:write")) {
        // Simkl silently downgrades an unknown scope to read-only
        TmmToastManager.showWarningToast(this, TmmResourceBundle.getString("Settings.simkl"),
            TmmResourceBundle.getString("Settings.simkl.scope.warning"));
      }
    }
    else if (!cancelled.get()) {
      // a deliberate cancel is no error - otherwise name the reason the poller ran into
      String reasonKey = failureKey.get();
      TmmToastManager.showErrorToast(this, TmmResourceBundle.getString("Settings.simkl"),
          TmmResourceBundle.getString(reasonKey != null ? reasonKey : "Settings.simkl.connect.problem"));
    }
    updateSimklStatus();
  }

  /**
   * Polls the Simkl.com token endpoint until the user has approved the device flow request. "Not decided yet" and "slow down" are waited out, a
   * problem on Simkl's side is retried a few times, and every other rejection ends the loop - polling a rejected device code cannot succeed. Simkl
   * reports neither a declined nor an abandoned request, so our own deadline is the only signal for "nothing was approved".
   */
  private void pollSimklApproval(SimklDeviceCodeResponse deviceCode, JLabel statusLabel, AtomicBoolean aborted, AtomicBoolean approved,
      AtomicReference<String> failureKey, JDialog dialog) {
    long intervalSec = parseSeconds(deviceCode.interval, SIMKL_POLL_INTERVAL_SEC);
    long deadline = System.currentTimeMillis() + Math.min(parseSeconds(deviceCode.expires_in, SIMKL_MAX_WAIT_SEC), SIMKL_MAX_WAIT_SEC) * 1000L;

    Simkl simkl = Simkl.getInstance();
    String statusKey = "Settings.simkl.connect.timeout";
    int consecutiveErrors = 0;

    while (!aborted.get() && System.currentTimeMillis() < deadline) {
      if (!sleep(intervalSec * 1000L)) {
        // interrupted - the user closed the dialog
        break;
      }
      if (aborted.get()) {
        break;
      }

      SimklAuthResult result;
      try {
        result = simkl.pollForV2Token(deviceCode.device_code);
      }
      catch (Exception e) {
        if (++consecutiveErrors >= SIMKL_MAX_POLL_ERRORS) {
          LOGGER.error("Simkl.com could not be reached {} times in a row - giving up the device flow - '{}'", consecutiveErrors, e.getMessage());
          statusKey = "Settings.simkl.connect.error";
          break;
        }
        LOGGER.warn("Could not poll the Simkl.com device flow - '{}'", e.getMessage());
        continue;
      }

      if (result.isApproved()) {
        if (aborted.get()) {
          // the user pressed cancel while this poll was on its way - do not silently connect him
          break;
        }
        simkl.storeV2Tokens(result.tokens);
        approved.set(true);
        statusKey = "Settings.simkl.connect.success";
        break;
      }

      if (result.isSlowDown()) {
        // the poll timer is reset by the attempt itself - wait longer before the next poll
        consecutiveErrors = 0;
        intervalSec += SIMKL_SLOW_DOWN_SEC;
        continue;
      }

      if (result.isPending()) {
        // the user has not approved the request yet
        consecutiveErrors = 0;
        continue;
      }

      // everything else is final for this device code
      if (result.isExpired()) {
        statusKey = "Settings.simkl.connect.expired";
      }
      else if (result.isAccessDenied()) {
        statusKey = "Settings.simkl.connect.denied";
      }
      else if (result.isInvalidClient()) {
        LOGGER.error("Simkl.com rejected the app credentials for the device flow - this is not fixable by retrying");
        statusKey = "Settings.simkl.connect.error";
      }
      else if (result.httpStatus >= 500) {
        // a problem on Simkl's side is worth another attempt, but not an endless wait
        if (++consecutiveErrors < SIMKL_MAX_POLL_ERRORS) {
          LOGGER.warn("Simkl.com answered the device flow poll with '{}' - trying again", result.errorCode);
          continue;
        }
        statusKey = "Settings.simkl.connect.error";
      }
      else {
        LOGGER.warn("Simkl.com rejected the device flow poll with '{}' - stopping", result.errorCode);
        statusKey = "Settings.simkl.connect.error";
      }
      break;
    }

    if (aborted.get()) {
      return;
    }

    final String messageKey = statusKey;
    SwingUtilities.invokeLater(() -> statusLabel.setText(TmmResourceBundle.getString(messageKey)));
    if (!approved.get()) {
      // remember why this did not work out, so the toast can name the reason instead of a generic error
      failureKey.set(messageKey);
      // give the user a moment to read the message before the dialog closes
      sleep(1500L);
    }
    SwingUtilities.invokeLater(dialog::dispose);
  }

  private void updateSimklStatus() {
    Simkl simkl = Simkl.getInstance();

    if (simkl.hasLegacyV1Token()) {
      lblSimklStatus.setText(TmmResourceBundle.getString("Settings.simkl.status.legacy"));
    }
    else if (simkl.hasV2Tokens() && StringUtils.isNotBlank(simkl.getRefreshToken())) {
      lblSimklStatus.setText(TmmResourceBundle.getString("Settings.simkl.status.good"));
    }
    else {
      lblSimklStatus.setText(TmmResourceBundle.getString("Settings.simkl.status.bad"));
    }
  }

  private long parseSeconds(String value, long fallback) {
    try {
      long seconds = Long.parseLong(StringUtils.trimToEmpty(value));
      return seconds > 0 ? seconds : fallback;
    }
    catch (NumberFormatException e) {
      return fallback;
    }
  }

  private boolean sleep(long millis) {
    try {
      Thread.sleep(millis);
      return true;
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
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

      chckbxTraktTv = new JCheckBox(TmmResourceBundle.getString("Settings.trakt.enable"));
      panelTrakt.add(chckbxTraktTv, "cell 1 0 2 1");
      {
        btnGetTraktPin = new JButton(TmmResourceBundle.getString("Settings.trakt.getpin"));
        panelTrakt.add(btnGetTraktPin, "cell 2 1");

        btnTestTraktConnection = new JButton(TmmResourceBundle.getString("Settings.trakt.testconnection"));
        panelTrakt.add(btnTestTraktConnection, "cell 2 1");
      }
      {
        lblTraktStatus = new JLabel("");
        panelTrakt.add(lblTraktStatus, "cell 2 2");
      }

      JLabel lblTraktDateT = new TmmLabel(TmmResourceBundle.getString("Settings.trakt.date"));
      panelTrakt.add(lblTraktDateT, "flowx,cell 2 3");

      cbTraktDate = new JComboBox(DateField.values());
      panelTrakt.add(cbTraktDate, "cell 2 3");
    }
    {
      JPanel panelSimkl = new JPanel();
      panelSimkl.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][]"));

      JLabel lblSimklT = new TmmLabel(TmmResourceBundle.getString("Settings.simkl"), H3);

      if (!License.getInstance().isValidLicense()) {
        lblSimklT.setText("*PRO* " + lblSimklT.getText());
      }

      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelSimkl, lblSimklT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/settings#simkl"));
      add(collapsiblePanel, "cell 0 2,growx, wmin 0");

      chckbxSimklCom = new JCheckBox(TmmResourceBundle.getString("Settings.simkl.enable"));
      panelSimkl.add(chckbxSimklCom, "cell 1 0 2 1");
      {
        btnConnectSimkl = new JButton(TmmResourceBundle.getString("Settings.simkl.connect"));
        panelSimkl.add(btnConnectSimkl, "cell 2 1");

        btnTestSimklConnection = new JButton(TmmResourceBundle.getString("Settings.simkl.testconnection"));
        panelSimkl.add(btnTestSimklConnection, "cell 2 1");

        btnDisconnectSimkl = new JButton(TmmResourceBundle.getString("Settings.simkl.disconnect"));
        panelSimkl.add(btnDisconnectSimkl, "cell 2 1");
      }
      {
        lblSimklStatus = new JLabel("");
        panelSimkl.add(lblSimklStatus, "cell 2 2");
      }
    }
    {
      JPanel panelMdbList = new JPanel();
      panelMdbList.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[]"));
      JLabel lblMdbListT = new TmmLabel(TmmResourceBundle.getString("Settings.external.rating.mdblist"), H3);

      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMdbList, lblMdbListT, true);
      add(collapsiblePanel, "cell 0 4,growx, wmin 0");
      {
        JLabel lblMdbListApiKeyT = new JLabel(TmmResourceBundle.getString("Settings.api.key"));
        panelMdbList.add(lblMdbListApiKeyT, "cell 1 0 2 1");

        tfMdbListApiKey = new JTextField();
        panelMdbList.add(tfMdbListApiKey, "cell 1 0 2 1");
        tfMdbListApiKey.setColumns(30);

        JTextArea tpMediaPlayer = new ReadOnlyTextArea(TmmResourceBundle.getString("Settings.external.rating.mdblist.hint"));
        panelMdbList.add(tpMediaPlayer, "cell 1 1 2 1,growx, wmin 0");
        TmmFontHelper.changeFont(tpMediaPlayer, L2);
      }
    }
    initDataBindings();
  }

  protected void initDataBindings() {
    Property settingsBeanProperty = BeanProperty.create("traktDateField");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty, cbTraktDate,
        jComboBoxBeanProperty);
    autoBinding.bind();
    //
    Property settingsBeanProperty_1 = BeanProperty.create("mdbListApiKey");
    Property jTextFieldBeanProperty = BeanProperty.create("text");
    AutoBinding autobinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_1, tfMdbListApiKey,
        jTextFieldBeanProperty);
    autobinding_1.bind();
    //
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    Property jButtonBeanProperty = BeanProperty.create("enabled");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxTraktTv, jCheckBoxBeanProperty, btnTestTraktConnection,
        jButtonBeanProperty);
    autoBinding_2.bind();
    //
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxTraktTv, jCheckBoxBeanProperty, cbTraktDate,
        jButtonBeanProperty);
    autoBinding_3.bind();
    //
    Property settingsBeanProperty_2 = BeanProperty.create("traktTvEnabled");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_2, chckbxTraktTv,
        jCheckBoxBeanProperty);
    autoBinding_6.bind();
    //
    Property settingsBeanProperty_3 = BeanProperty.create("simklComEnabled");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_3, chckbxSimklCom,
        jCheckBoxBeanProperty);
    autoBinding_7.bind();
    //
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxSimklCom, jCheckBoxBeanProperty, btnConnectSimkl,
        jButtonBeanProperty);
    autoBinding_1.bind();
    //
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxSimklCom, jCheckBoxBeanProperty, btnTestSimklConnection,
        jButtonBeanProperty);
    autoBinding_4.bind();
    //
    AutoBinding autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxSimklCom, jCheckBoxBeanProperty, btnDisconnectSimkl,
        jButtonBeanProperty);
    autoBinding_8.bind();
    //
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxTraktTv, jCheckBoxBeanProperty, btnGetTraktPin,
        jButtonBeanProperty);
    autoBinding_5.bind();
  }
}
