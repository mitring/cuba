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

import com.haulmont.cuba.gui.components.data.HasValueBinding;

import java.util.Collection;

/**
 * Base interface for "fields" - components intended to display and edit value of a certain entity attribute.
 */
public interface Field<T> extends DatasourceComponent<T>, HasValueBinding<T>, Component.HasCaption,
        Component.HasValue<T>, Component.Editable, Component.BelongToFrame, Component.Validatable, Component.HasIcon,
        Component.HasContextHelp {

    /**
     * @return whether the field must contain a non-null value
     */
    boolean isRequired();
    void setRequired(boolean required);

    String getRequiredMessage();
    /**
     * A message that will be displayed to user if the field is required but has null value
     */
    void setRequiredMessage(String msg);

    /*
     * vaadin8 move to HasValidators *
     */

    /**
     * Add {@link Validator} instance.
     */
    void addValidator(Validator validator);
    void removeValidator(Validator validator);

    /**
     * @return unmodifiable collection with Field validators
     */
    Collection<Validator> getValidators();

    /**
     * Field validator.<br>
     * Validators are invoked when {@link Validatable#validate()} is called.
     * Editor screen calls {@code validate()} on commit.
     */
    interface Validator {
        /**
         * @param value field value to validate
         * @throws ValidationException this exception must be thrown by the validator if the value is not valid
         */
        void validate(Object value) throws ValidationException;
    }
}