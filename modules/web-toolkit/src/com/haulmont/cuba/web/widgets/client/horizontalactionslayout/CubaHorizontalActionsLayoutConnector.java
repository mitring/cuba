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

package com.haulmont.cuba.web.widgets.client.horizontalactionslayout;

import com.haulmont.cuba.web.widgets.CubaHorizontalActionsLayout;
import com.haulmont.cuba.web.widgets.client.orderedactionslayout.CubaOrderedActionsLayoutConnector;
import com.vaadin.shared.ui.Connect;
import com.vaadin.shared.ui.orderedlayout.HorizontalLayoutState;

@Connect(value = CubaHorizontalActionsLayout.class, loadStyle = Connect.LoadStyle.EAGER)
public class CubaHorizontalActionsLayoutConnector extends CubaOrderedActionsLayoutConnector {

    @Override
    public CubaHorizontalActionsLayoutWidget getWidget() {
        return (CubaHorizontalActionsLayoutWidget)  super.getWidget();
    }

    @Override
    public HorizontalLayoutState getState() {
        return (HorizontalLayoutState) super.getState();
    }
}