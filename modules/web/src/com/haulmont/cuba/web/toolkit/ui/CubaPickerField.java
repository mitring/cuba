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

package com.haulmont.cuba.web.toolkit.ui;

import com.haulmont.cuba.gui.components.PickerField;
import com.haulmont.cuba.gui.theme.ThemeConstants;
import com.haulmont.cuba.web.App;
import com.vaadin.event.Action;
import com.vaadin.server.*;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.TextField;
import com.vaadin.v7.data.util.converter.Converter;
import com.vaadin.v7.ui.Field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CubaPickerField extends com.vaadin.v7.ui.CustomField implements Action.Container {

    protected Field field;
    protected Converter captionFormatter;

    protected List<Button> buttons = new ArrayList<>(4);
    protected CubaCssActionsLayout container;

    // CAUTION used only for IE 9 layout, is null for another browsers
    // Fixes PL-8205
    protected CssLayout ie9InputWrapper = null;

    protected boolean useCustomField = false;
    protected boolean fieldReadOnly = true;

    protected boolean suppressTextChangeListener = false;

    public CubaPickerField() {
        init();

        initTextField();
        initLayout();
    }

    public CubaPickerField(com.vaadin.v7.ui.AbstractField field) {
        init();

        this.field = field;
        this.useCustomField = true;
        initLayout();
    }

    protected void init() {
        setPrimaryStyleName("c-pickerfield");
        setSizeUndefined();

        setValidationVisible(false);
        setShowBufferedSourceException(false);
        setShowErrorForDisabledState(false);
    }

    @Override
    protected Component initContent() {
        return container;
    }

    protected void initLayout() {
        container = new CubaCssActionsLayout();
        container.setPrimaryStyleName("c-pickerfield-layout");

        field.setWidth(100, Unit.PERCENTAGE);

        Page current = Page.getCurrent();
        if (current != null) {
            WebBrowser browser = current.getWebBrowser();
            if (browser != null
                    && (browser.isIE()
                    && browser.getBrowserMajorVersion() <= 10
                    || browser.isSafari())) {
                ie9InputWrapper = new CssLayout();
                ie9InputWrapper.setWidth(100, Unit.PERCENTAGE);
                ie9InputWrapper.setPrimaryStyleName("ie9-input-wrap");
                ie9InputWrapper.addComponent(field);

                container.addComponent(ie9InputWrapper);
            } else {
                container.addComponent(field);
            }
        } else {
            container.addComponent(field);
        }

        if (App.isBound()) {
            ThemeConstants theme = App.getInstance().getThemeConstants();
            setWidth(theme.get("cuba.web.CubaPickerField.width"));
        }

        setFocusDelegate(field);
    }

    protected void initTextField() {
        CubaTextField field = new CubaTextField();
        field.setStyleName("c-pickerfield-text");
        field.setReadOnlyFocusable(true);

        field.setReadOnly(true);
        field.setNullRepresentation("");

        addValueChangeListener(event -> {
            if (!suppressTextChangeListener) {
                updateTextRepresentation();
            }
        });

        this.field = field;
    }

    public boolean isFieldReadOnly() {
        return fieldReadOnly;
    }

    public void setFieldReadOnly(boolean fieldReadOnly) {
        this.fieldReadOnly = fieldReadOnly;

        if (!useCustomField) {
            getField().setReadOnly(isReadOnly() || fieldReadOnly);
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);

        if (!useCustomField) {
            getField().setReadOnly(readOnly || fieldReadOnly);
        }
    }

    @Override
    public void attach() {
        suppressTextChangeListener = true;

        super.attach();

        suppressTextChangeListener = false;

        // update text representation manually
        if (field instanceof TextField) {
            updateTextRepresentation();
        }
    }

    @Override
    public void setWidth(float width, Unit unit) {
        super.setWidth(width, unit);

        if (container != null) {
            if (width < 0) {
                container.setWidthUndefined();
                field.setWidthUndefined();
                if (ie9InputWrapper != null) {
                    ie9InputWrapper.setWidthUndefined();
                }
            } else {
                container.setWidth(100, Unit.PERCENTAGE);
                field.setWidth(100, Unit.PERCENTAGE);
                if (ie9InputWrapper != null) {
                    ie9InputWrapper.setWidth(100, Unit.PERCENTAGE);
                }
            }
        }
    }

    @Override
    public void setHeight(float height, Unit unit) {
        super.setHeight(height, unit);

        if (container != null) {
            if (height < 0) {
                container.setHeightUndefined();
                field.setHeightUndefined();
                if (ie9InputWrapper != null) {
                    ie9InputWrapper.setHeightUndefined();
                }
            } else {
                container.setHeight(100, Unit.PERCENTAGE);
                field.setHeight(100, Unit.PERCENTAGE);
                if (ie9InputWrapper != null) {
                    ie9InputWrapper.setHeight(100, Unit.PERCENTAGE);
                }
            }
        }
    }

    protected void updateTextRepresentation() {
        TextField textField = (TextField) field;

        suppressTextChangeListener = true;

        String value = getStringRepresentation();
        textField.setValue(value != null ? value : "");

        suppressTextChangeListener = false;
    }

    @SuppressWarnings("unchecked")
    protected String getStringRepresentation() {
        if (captionFormatter != null) {
            return (String) captionFormatter.convertToPresentation(getValue(), String.class, getLocale());
        }

        return String.valueOf(getValue());
    }

    public List<Button> getButtons() {
        return Collections.unmodifiableList(buttons);
    }

    public void addButton(Button button, int index) {
        button.setTabIndex(-1);
        button.setStyleName("c-pickerfield-button");

        buttons.add(index, button);
        container.addComponent(button, index + 1); // 0 - field
    }

    public void removeButton(Button button) {
        buttons.remove(button);
        container.removeComponent(button);
    }

    public Field getField() {
        return field;
    }

    public void addFieldListener(final PickerField.FieldListener listener) {
        field.addValueChangeListener(event -> {
            String text = (String) event.getProperty().getValue();

            if (!suppressTextChangeListener && !Objects.equals(getStringRepresentation(), text)) {
                suppressTextChangeListener = true;

                listener.actionPerformed(text, getValue());

                suppressTextChangeListener = false;

                // update text representation manually
                if (field instanceof TextField) {
                    updateTextRepresentation();
                }
            }
        });
    }

    @Override
    public Class getType() {
        return Object.class;
    }

    @Override
    public void focus() {
        field.focus();
    }

    @Override
    public void addActionHandler(Action.Handler actionHandler) {
        container.addActionHandler(actionHandler);
    }

    @Override
    public void removeActionHandler(Action.Handler actionHandler) {
        container.removeActionHandler(actionHandler);
    }

    @Override
    public ErrorMessage getErrorMessage() {
        ErrorMessage superError = super.getErrorMessage();
        if (!isReadOnly() && isRequired() && isEmpty()) {
            ErrorMessage error = AbstractErrorMessage.getErrorMessageForException(
                    new com.vaadin.v7.data.Validator.EmptyValueException(getRequiredError()));
            if (error != null) {
                return new CompositeErrorMessage(superError, error);
            }
        }

        return superError;
    }

    @Override
    public boolean isEmpty() {
        return getValue() == null;
    }

    public Converter getCaptionFormatter() {
        return captionFormatter;
    }

    public void setCaptionFormatter(Converter captionFormatter) {
        this.captionFormatter = captionFormatter;
    }

    @Override
    public void setTabIndex(int tabIndex) {
        field.setTabIndex(tabIndex);
    }

    @Override
    public int getTabIndex() {
        return field.getTabIndex();
    }

/*  vaadin8 implement
    @Override
    protected boolean fieldValueEquals(Object value1, Object value2) {
        // only if instance the same,
        // we can set instance of entity with the same id but different property values
        return value1 == value2;
    }
    */
}