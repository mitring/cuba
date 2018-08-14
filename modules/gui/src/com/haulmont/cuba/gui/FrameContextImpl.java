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
package com.haulmont.cuba.gui;

import com.haulmont.chile.core.datatypes.impl.EnumClass;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.compatibility.ComponentValueListenerWrapper;
import com.haulmont.cuba.gui.components.sys.ValuePathHelper;
import com.haulmont.cuba.gui.data.ValueListener;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

public class FrameContextImpl implements FrameContext {

    private final Frame frame;
    private Map<String, Object> params;

    public FrameContextImpl(Frame window, Map<String, Object> params) {
        this.frame = window;
        this.params = params;

        frame.getComponents();
    }

    public Collection<String> getParameterNames() {
        List<String> names = new ArrayList<>();
        for (String s : params.keySet()) {
            names.add(s.substring("param$".length()));
        }
        return names;
    }

    public <T> T getParameterValue(String property) {
        //noinspection unchecked
        return (T) params.get("param$" + property);
    }

    @Override
    public Frame getFrame() {
        return frame;
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public <T> T getParamValue(String param) {
        return (T) params.get(param);
    }

    @Override
    public <T> T getValue(String property) {
        final String[] elements = ValuePathHelper.parse(property);
        String[] path = elements;

        Component component = frame.getComponent(property);
        while (component == null && path.length > 1) {
            // in case of property contains a drill-down part
            path = ArrayUtils.subarray(path, 0, path.length - 1);
            component = frame.getComponent(ValuePathHelper.format(path));
        }

        if (component == null || component instanceof Frame
                || ((component instanceof Component.Wrapper) && ((Component.Wrapper) component).getComponent() instanceof Frame))
        {
            // if component not found or found a frame, try to search in the parent frame
            if (frame.getFrame() != null && frame.getFrame() != frame)
                return frame.getFrame().getContext().getValue(property);
            else
                return null;
        }

        final Object value = getValue(component);
        if (value == null)
            return null;

        if (path.length == elements.length) {
            //noinspection unchecked
            return (T) value;
        } else {
            final java.util.List<String> propertyPath = Arrays.asList(elements).subList(path.length, elements.length);
            final String[] properties = propertyPath.toArray(new String[0]);

            if (value instanceof Instance) {
                //noinspection RedundantTypeArguments
                return InstanceUtils.<T>getValueEx(((Instance) value), properties);
            } else if (value instanceof EnumClass) {
                if (properties.length == 1 && "id".equals(properties[0])) {
                    //noinspection unchecked
                    return (T) ((EnumClass) value).getId();
                } else {
                    throw new UnsupportedOperationException(String.format("Can't get property '%s' of enum %s", propertyPath, value));
                }
            } else {
                return null;
            }
        }
    }

    protected <T> T getValue(Component component) {
        if (component instanceof HasValue) {
            //noinspection RedundantTypeArguments
            return (T) ((HasValue) component).getValue();
        } else if (component instanceof ListComponent) {
            ListComponent list = (ListComponent) component;
            //noinspection unchecked
            return list.isMultiSelect() ? (T)list.getSelected() : (T)list.getSingleSelected();
        } else {
            return null;
        }
    }

    @Override
    public void setValue(String property, Object value) {
        final Component component = frame.getComponent(property);
        if (component instanceof HasValue) {
            ((HasValue) component).setValue(value);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void addValueListener(String componentName, ValueListener listener) {
        addValueChangeListener(componentName, new ComponentValueListenerWrapper(listener));
    }

    @Override
    public void removeValueListener(String componentName, ValueListener listener) {
        removeValueChangeListener(componentName, new ComponentValueListenerWrapper(listener));
    }

    @Override
    public void addValueChangeListener(String componentName, HasValue.ValueChangeListener listener) {
        Component component = frame.getComponent(componentName);
        if (component == null)
            throw new RuntimeException("Component not found: " + componentName);
        if (component instanceof HasValue) {
            ((HasValue) component).addValueChangeListener(listener);
        } else if (component instanceof ListComponent) {
            throw new UnsupportedOperationException("List component is not supported yet");
        } else {
            throw new RuntimeException("Unable to add listener to the component " + component);
        }
    }

    @Override
    public void removeValueChangeListener(String componentName, HasValue.ValueChangeListener listener) {
        Component component = frame.getComponent(componentName);
        if (component == null)
            throw new RuntimeException("Component not found: " + componentName);
        if (component instanceof HasValue) {
            ((HasValue) component).removeValueChangeListener(listener);
        } else if (component instanceof ListComponent) {
            throw new UnsupportedOperationException("List component is not supported yet");
        } else {
            throw new RuntimeException("Unable to add listener to the component " + component);
        }
    }
}