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

import com.haulmont.cuba.gui.components.autocomplete.AutoCompleteSupport;
import com.haulmont.cuba.gui.components.autocomplete.Suggester;
import org.apache.commons.lang3.StringUtils;

public interface SourceCodeEditor extends Field<String>, Component.Focusable {
    String NAME = "sourceCodeEditor";

    @Deprecated
    enum Mode implements HighlightMode {
        Java("java"),
        HTML("html"),
        XML("xml"),
        Groovy("groovy"),
        SQL("sql"),
        JavaScript("javascript"),
        Properties("properties"),
        CSS("css"),
        SCSS("scss"),
        Text("text");

        protected String id;

        Mode(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static Mode parse(String name) {
            if (StringUtils.isEmpty(name)) {
                return Text;
            }

            for (Mode mode : values()) {
                if (StringUtils.equalsIgnoreCase(name, mode.name())) {
                    return mode;
                }
            }

            return Text;
        }
    }

    HighlightMode getMode();
    void setMode(HighlightMode mode);

    Suggester getSuggester();
    void setSuggester(Suggester suggester);

    AutoCompleteSupport getAutoCompleteSupport();

    void setShowGutter(boolean showGutter);
    boolean isShowGutter();

    void setShowPrintMargin(boolean showPrintMargin);
    boolean isShowPrintMargin();

    /**
     * Set print margin position in symbols
     *
     * @param printMarginColumn print margin position in symbols
     */
    void setPrintMarginColumn(int printMarginColumn);
    /**
     * @return print margin position in symbols
     */
    int getPrintMarginColumn();

    void setHighlightActiveLine(boolean highlightActiveLine);
    boolean isHighlightActiveLine();

    /**
     * Enables Tab key handling as tab symbol.
     * If handleTabKey is false then Tab/Shift-Tab key press will change focus to next/previous field.
     */
    void setHandleTabKey(boolean handleTabKey);
    /**
     * @return if Tab key handling is enabled
     */
    boolean isHandleTabKey();

    @SuppressWarnings("unchecked")
    @Override
    String getValue();

    /**
     * Returns a string representation of the value.
     */
    String getRawValue();

    /**
     * Reset the stack of undo/redo redo operations.
     */
    void resetEditHistory();
}