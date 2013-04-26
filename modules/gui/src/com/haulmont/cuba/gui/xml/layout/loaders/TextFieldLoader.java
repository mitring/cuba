/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

/**
 * @author abramov
 * @version $Id$
 */
public class TextFieldLoader extends AbstractTextFieldLoader {

    public TextFieldLoader(Context context, LayoutLoaderConfig config, ComponentsFactory factory) {
        super(context, config, factory);
    }

    @Override
    public Component loadComponent(ComponentsFactory factory, Element element, Component parent)
            throws InstantiationException, IllegalAccessException {
        TextField component = (TextField) super.loadComponent(factory, element, parent);

        loadMaxLength(element, component);
        loadTrimming(element, component);

        String datatypeStr = element.attributeValue("datatype");
        if (!StringUtils.isEmpty(datatypeStr)) {
            Datatype datatype = Datatypes.get(datatypeStr);
            component.setDatatype(datatype);
        }

        component.setFormatter(loadFormatter(element));
        return component;
    }
}