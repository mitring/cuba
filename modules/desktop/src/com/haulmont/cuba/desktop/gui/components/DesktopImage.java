/*
 * Copyright (c) 2008-2018 Haulmont.
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

package com.haulmont.cuba.desktop.gui.components;

import com.google.common.collect.ImmutableMap;
import com.haulmont.bali.events.EventRouter;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.gui.GuiDevelopmentException;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.WeakItemChangeListener;
import com.haulmont.cuba.gui.data.impl.WeakItemPropertyChangeListener;
import com.haulmont.cuba.gui.export.ByteArrayDataProvider;
import org.jdesktop.swingx.JXImageView;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

public class DesktopImage extends DesktopAbstractComponent<JXImageView> implements Image {

    protected static final Map<Class<? extends Resource>, Class<? extends Resource>> resourcesClasses;

    static {
        ImmutableMap.Builder<Class<? extends Resource>, Class<? extends Resource>> builder =
                new ImmutableMap.Builder<>();

        builder.put(UrlResource.class, DesktopUrlResource.class);
        builder.put(ClasspathResource.class, DesktopClasspathResource.class);
        builder.put(ThemeResource.class, DesktopThemeResource.class);
        builder.put(FileDescriptorResource.class, DesktopFileDescriptorResource.class);
        builder.put(FileResource.class, DesktopFileResource.class);
        builder.put(StreamResource.class, DesktopStreamResource.class);
        builder.put(RelativePathResource.class, DesktopRelativePathResource.class);

        resourcesClasses = builder.build();
    }

    protected Resource resource;

    protected Datasource datasource;
    protected MetaPropertyPath metaPropertyPath;

    protected Datasource.ItemPropertyChangeListener itemPropertyChangeListener;
    protected WeakItemPropertyChangeListener weakItemPropertyChangeListener;

    protected Datasource.ItemChangeListener itemChangeListener;
    protected WeakItemChangeListener weakItemChangeListener;

    // just stub
    protected ScaleMode scaleMode = ScaleMode.NONE;

    protected EventRouter eventRouter;

    protected Runnable resourceUpdateHandler;
    private MouseListener mouseListener;

    public DesktopImage() {
        impl = new JXImageView();
        impl.setDragEnabled(false);
        impl.setEditable(false);
        impl.setBackgroundPainter((g, object, width, height) ->
                g.setBackground(Color.gray));
    }

    @Override
    public void setDatasource(Datasource datasource, String property) {
        if ((datasource == null && property != null) || (datasource != null && property == null)) {
            throw new IllegalArgumentException("Datasource and property should be either null or not null at the same time");
        }

        if (datasource == this.datasource && ((metaPropertyPath != null && metaPropertyPath.toString().equals(property)) ||
                (metaPropertyPath == null && property == null))) {
            return;
        }

        if (this.datasource != null) {
            metaPropertyPath = null;

            impl.setImage((java.awt.Image) null);

            //noinspection unchecked
            this.datasource.removeItemPropertyChangeListener(weakItemPropertyChangeListener);
            weakItemPropertyChangeListener = null;

            //noinspection unchecked
            this.datasource.removeItemChangeListener(weakItemChangeListener);
            weakItemChangeListener = null;

            this.datasource = null;
        }

        if (datasource != null) {
            //noinspection unchecked
            this.datasource = datasource;

            metaPropertyPath = AppBeans.get(MetadataTools.class)
                    .resolveMetaPropertyPathNN(datasource.getMetaClass(), property);

            updateComponent();

            itemPropertyChangeListener = e -> {
                if (e.getProperty().equals(metaPropertyPath.toString())) {
                    updateComponent();
                }
            };
            weakItemPropertyChangeListener = new WeakItemPropertyChangeListener(datasource, itemPropertyChangeListener);
            //noinspection unchecked
            this.datasource.addItemPropertyChangeListener(weakItemPropertyChangeListener);

            itemChangeListener = e ->
                    updateComponent();

            weakItemChangeListener = new WeakItemChangeListener(datasource, itemChangeListener);
            //noinspection unchecked
            datasource.addItemChangeListener(weakItemChangeListener);
        }
    }

    protected void updateComponent() {
        Object propertyValue = InstanceUtils.getValueEx(datasource.getItem(), metaPropertyPath.getPath());
        Resource resource = createImageResource(propertyValue);

        updateValue(resource);
    }

    protected void updateValue(Resource value) {
        Resource oldValue = this.resource;
        if (oldValue != null) {
            ((DesktopAbstractResource) oldValue).setResourceUpdatedHandler(null);
        }

        this.resource = value;

        BufferedImage vResource = null;
        if (value != null && ((DesktopAbstractResource) value).hasSource()) {
            vResource = ((DesktopAbstractResource) value).getResource();
        }
        impl.setImage(vResource);

        if (value != null) {
            ((DesktopAbstractResource) value).setResourceUpdatedHandler(resourceUpdateHandler);
        }

        getEventRouter().fireEvent(SourceChangeListener.class, SourceChangeListener::sourceChanged,
                new SourceChangeEvent(this, oldValue, this.resource));
    }

    protected Resource createImageResource(Object propertyValue) {
        if (propertyValue == null) {
            return null;
        }

        if (propertyValue instanceof FileDescriptor) {
            FileDescriptorResource imageResource = createResource(FileDescriptorResource.class);
            imageResource.setFileDescriptor((FileDescriptor) propertyValue);
            return imageResource;
        }

        if (propertyValue instanceof byte[]) {
            StreamResource imageResource = createResource(StreamResource.class);
            Supplier<InputStream> streamSupplier = () ->
                    new ByteArrayDataProvider((byte[]) propertyValue).provide();
            imageResource.setStreamSupplier(streamSupplier);
            return imageResource;
        }

        throw new GuiDevelopmentException(
                "The Image component supports only FileDescriptor and byte[] datasource property value binding",
                getFrame().getId());
    }

    @Override
    public Datasource getDatasource() {
        return datasource;
    }

    @Override
    public MetaPropertyPath getMetaPropertyPath() {
        return metaPropertyPath;
    }

    // just stub
    @Override
    public ScaleMode getScaleMode() {
        return scaleMode;
    }

    // just stub
    @Override
    public void setScaleMode(ScaleMode scaleMode) {
        this.scaleMode = scaleMode;
    }

    protected EventRouter getEventRouter() {
        if (eventRouter == null) {
            eventRouter = new EventRouter();
        }
        return eventRouter;
    }

    @Override
    public void addClickListener(ClickListener listener) {
        getEventRouter().addListener(ClickListener.class, listener);

        if (mouseListener == null) {
            mouseListener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    getEventRouter().fireEvent(ClickListener.class, ClickListener::onClick,
                            new ClickEvent(DesktopImage.this, convertMouseEvent(e)));
                }
            };
        }
        impl.addMouseListener(mouseListener);
    }

    @Override
    public void removeClickListener(ClickListener listener) {
        getEventRouter().removeListener(ClickListener.class, listener);

        if (!getEventRouter().hasListeners(ClickListener.class)) {
            impl.removeMouseListener(mouseListener);
        }
    }

    @Override
    public Resource getSource() {
        return resource;
    }

    @Override
    public void setSource(Resource resource) {
        if (this.resource == null && resource == null || (this.resource != null && this.resource.equals(resource))) {
            return;
        }

        updateValue(resource);
    }

    @Override
    public <R extends Resource> R setSource(Class<R> type) {
        R resource = createResource(type);

        updateValue(resource);

        return resource;
    }

    @Override
    public <R extends Resource> R createResource(Class<R> type) {
        Class<? extends Resource> resourceClass = resourcesClasses.get(type);
        if (resourceClass == null) {
            throw new IllegalStateException(String.format("Can't find resource class for '%s'", type.getTypeName()));
        }

        try {
            return type.cast(resourceClass.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(String.format("Error creating the '%s' resource instance",
                    type.getTypeName()), e);
        }
    }

    @Override
    public void setAlternateText(String alternateText) {
        impl.setToolTipText(alternateText);
    }

    @Override
    public String getAlternateText() {
        return impl.getToolTipText();
    }

    @Override
    public void addSourceChangeListener(SourceChangeListener listener) {
        getEventRouter().addListener(SourceChangeListener.class, listener);
    }

    @Override
    public void removeSourceChangeListener(SourceChangeListener listener) {
        getEventRouter().removeListener(SourceChangeListener.class, listener);
    }

    @Override
    public String getDescription() {
        return impl.getToolTipText();
    }

    @Override
    public void setDescription(String description) {
        impl.setToolTipText(description);
    }

    protected MouseEventDetails convertMouseEvent(MouseEvent event) {
        MouseEventDetails details = new MouseEventDetails();

        MouseEventDetails.MouseButton button;
        switch (event.getButton()) {
            case 1:
                button = MouseEventDetails.MouseButton.LEFT;
                break;
            case 2:
                button = MouseEventDetails.MouseButton.RIGHT;
                break;
            case 3:
                button = MouseEventDetails.MouseButton.MIDDLE;
                break;
            default:
                button = MouseEventDetails.MouseButton.LEFT;
        }
        details.setButton(button);

        details.setClientX(event.getXOnScreen());
        details.setClientY(event.getYOnScreen());

        details.setAltKey(event.isAltDown());
        details.setCtrlKey(event.isControlDown());
        details.setMetaKey(event.isMetaDown());
        details.setShiftKey(event.isShiftDown());
        details.setDoubleClick(event.getClickCount() == 2);

        details.setRelativeX(event.getX());
        details.setRelativeY(event.getY());

        return details;
    }
}
