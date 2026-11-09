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

package org.tinymediamanager.ui.plaf;

import com.formdev.flatlaf.ui.FlatButtonBorder;
import org.tinymediamanager.ui.components.button.SplitButton;

import javax.swing.AbstractButton;
import javax.swing.UIManager;
import java.awt.Component;

/**
 * The class {@link TmmSplitButtonBorder} provides the border for the {@link SplitButton} panel.
 * <p>
 * Since FlatLaf 3.8 {@link FlatButtonBorder} returns an arc of zero for components that are not buttons. Because the split button is a panel,
 * this would paint it without rounded corners. This border returns the {@code SplitButton.arc} UI default for the panel instead.
 *
 * @author Manuel Laggner
 */
public class TmmSplitButtonBorder extends FlatButtonBorder {
    private final int splitButtonArc = UIManager.getInt("SplitButton.arc");

    /**
     * Returns the arc of the border.
     *
     * @param c the component
     * @return the arc of the border
     */
    @Override
    protected int getArc(Component c) {
        if (c instanceof AbstractButton) {
            return super.getArc(c);
        }
        return splitButtonArc;
    }
}
