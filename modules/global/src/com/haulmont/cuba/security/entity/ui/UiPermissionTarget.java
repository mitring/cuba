/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.security.entity.ui;

import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.impl.AbstractInstance;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;
import com.haulmont.cuba.core.global.MetadataProvider;
import com.haulmont.cuba.core.global.UuidProvider;

import java.util.UUID;

/**
 * <p>$Id$</p>
 *
 * @author artamonov
 */
@com.haulmont.chile.core.annotations.MetaClass(name = "sec$UiTarget")
@SystemLevel
public class UiPermissionTarget extends AbstractInstance
        implements Entity<String>, AssignableTarget {

    @MetaProperty(mandatory = true)
    private String id;

    @MetaProperty(mandatory = true)
    private String caption;

    @MetaProperty(mandatory = true)
    private String permissionValue;

    @MetaProperty(mandatory = true)
    private UiPermissionVariant permissionVariant = UiPermissionVariant.NOTSET;

    @MetaProperty(mandatory = true)
    private String screen;

    @MetaProperty(mandatory = true)
    private String component;

    private UUID uuid = UuidProvider.createUuid();

    public UiPermissionTarget(String id, String caption, String permissionValue) {
        this.id = id;
        this.caption = caption;
        this.permissionValue = permissionValue;
    }

    public UiPermissionTarget(String id, String caption, String permissionValue, UiPermissionVariant permissionVariant) {
        this.id = id;
        this.caption = caption;
        this.permissionValue = permissionValue;
        this.permissionVariant = permissionVariant;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public MetaClass getMetaClass() {
        return MetadataProvider.getSession().getClass(getClass());
    }

    @Override
    public String toString() {
        return caption;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    @Override
    public boolean isAssigned() {
        return permissionVariant != UiPermissionVariant.NOTSET;
    }

    @Override
    public String getPermissionValue() {
        return permissionValue;
    }

    public void setPermissionValue(String permissionValue) {
        this.permissionValue = permissionValue;
    }

    public String getScreen() {
        return screen;
    }

    public void setScreen(String screen) {
        this.screen = screen;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public UiPermissionVariant getPermissionVariant() {
        return permissionVariant;
    }

    public void setPermissionVariant(UiPermissionVariant permissionVariant) {
        this.permissionVariant = permissionVariant;
    }
}