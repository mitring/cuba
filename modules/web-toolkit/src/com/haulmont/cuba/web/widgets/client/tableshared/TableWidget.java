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
 */

package com.haulmont.cuba.web.widgets.client.tableshared;

import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ui.ShortcutActionHandler;
import com.vaadin.v7.client.ui.VScrollTable;

import java.util.List;

public interface TableWidget extends HasEnabled, ShortcutActionHandler.ShortcutActionHandlerOwner {
    boolean isCustomColumn(int colIndex);

    boolean isGenericRow(Widget rowWidget);

    String[] getVisibleColOrder();

    HasWidgets getRenderedRowByKey(String key);

    String getSortDescendingLabel();
    String getSortAscendingLabel();
    String getSortResetLabel();

    ApplicationConnection getClient();

    Widget getOwner();
    String getPaintableId();

    VScrollTable.RowRequestHandler getRowRequestHandler();

    VScrollTable.TableHead getHead();

    String getStylePrimaryName();

    String getColKeyByIndex(int index);

    int getColWidth(String colKey);

    void setColWidth(int colIndex, int w, boolean isDefinedWidth);

    boolean isTextSelectionEnabled();

    List<Widget> getRenderedRows();
}