/*
 * Copyright (c) 2008-2016 Haulmont.
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
 *
 */
package com.haulmont.cuba.gui.components;

public interface GroupBoxLayout
        extends ExpandingLayout,
                OrderedContainer,
                Component.HasIcon, Component.HasCaption, HasBorder, HasSpacing, HasOuterMargin, HasOrientation,
                Collapsable, Component.BelongToFrame, HasSettings, ShortcutNotifier {

    String NAME = "groupBox";

    /**
     * Set layout style as Vaadin Panel
     * @param showAsPanel
     */
    void setShowAsPanel(boolean showAsPanel);
    /**
     * @return true if layout looks like Vaadin Panel
     */
    boolean isShowAsPanel();
}