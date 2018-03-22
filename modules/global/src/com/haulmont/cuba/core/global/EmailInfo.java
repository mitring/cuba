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

package com.haulmont.cuba.core.global;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Contains email details: list of recipients, from address, caption, body and attachments.
 * See constructors for more information.
 *
 * @see com.haulmont.cuba.core.app.EmailService
 */
public class EmailInfo implements Serializable {

    private static final long serialVersionUID = -382773435130109083L;

    /**
     * Recipient email addresses separated with "," or ";" symbol.
     */
    private String addresses;
    private String caption;
    private String from;
    private String templatePath;
    private Map<String, Serializable> templateParameters;
    private String body;
    private ContentBodyType bodyType;
    private EmailAttachment[] attachments;
    private List<EmailHeader> headers;

    /**
     * Deprecated. Use {@link #EmailInfo(String, String, String, ContentBodyType, String, Map, EmailAttachment...)}
     * instead.
     * <p>
     *
     * Constructor. Example usage:
     * <pre>
     *     EmailInfo emailInfo = new EmailInfo(
                "john.doe@company.com,jane.roe@company.com",
                "Company news",
                "do-not-reply@company.com",
                "com/company/sample/email_templates/news.txt",
                Collections.singletonMap("some_var", some_value)
            );
     * </pre>
     *
     * @param addresses             comma or semicolon separated list of addresses
     * @param caption               email subject
     * @param from                  "from" address. If null, a default provided by {@code cuba.email.fromAddress} app property is used.
     * @param templatePath          path to a Freemarker template which is used to create the message body. The template
     *                              is loaded through {@link Resources} in the <b>core</b> module.
     * @param templateParameters    map of parameters to be passed to the template
     * @param attachments           email attachments. Omit this parameter if there are no attachments.
     */
    @Deprecated
    public EmailInfo(String addresses, String caption, @Nullable String from, String templatePath,
                     Map<String, Serializable> templateParameters, EmailAttachment... attachments) {
        this.addresses = addresses;
        this.caption = caption;
        this.templatePath = templatePath;
        this.attachments = attachments;
        this.templateParameters = templateParameters;
        this.from = from;
    }

    /**
     * Constructor. Example usage:
     * <pre>
     *     EmailInfo emailInfo = new EmailInfo(
                "john.doe@company.com,jane.roe@company.com",
                "Company news",
                "do-not-reply@company.com",
                ContentBodyType.HTML,
                "com/company/sample/email_templates/news.txt",
                Collections.singletonMap("some_var", some_value)
           );
     * </pre>
     *
     * @param addresses             comma or semicolon separated list of addresses
     * @param caption               email subject
     * @param from                  "from" address. If null, a default provided by {@code cuba.email.fromAddress} app property is used.
     * @param bodyType              email body like text or html
     * @param templatePath          path to a Freemarker template which is used to create the message body. The template
     *                              is loaded through {@link Resources} in the <b>core</b> module.
     * @param templateParameters    map of parameters to be passed to the template
     * @param attachments           email attachments. Omit this parameter if there are no attachments.
     */
    public EmailInfo(String addresses, String caption, @Nullable String from, ContentBodyType bodyType,
                     String templatePath, Map<String, Serializable> templateParameters,
                     EmailAttachment... attachments) {
        this.addresses = addresses;
        this.caption = caption;
        this.templatePath = templatePath;
        this.attachments = attachments;
        this.templateParameters = templateParameters;
        this.from = from;
        this.bodyType = bodyType;
    }

    /**
     * Deprecated. Use {@link #EmailInfo(String, String, String, String, ContentBodyType, EmailAttachment...)}
     * instead.
     * <p>
     *
     * Constructor.
     * <pre>
     *     EmailInfo emailInfo = new EmailInfo(
                "john.doe@company.com,jane.roe@company.com",
                "Company news",
                null,
                "Some content"
            );
     * </pre>
     *
     * @param addresses             comma or semicolon separated list of addresses
     * @param caption               email subject
     * @param from                  "from" address. If null, a default provided by {@code cuba.email.fromAddress} app property is used.
     * @param body                  email body
     * @param attachments           email attachments. Omit this parameter if there are no attachments.
     */
    @Deprecated
    public EmailInfo(String addresses, String caption, @Nullable String from, String body, EmailAttachment... attachments) {
        this.addresses = addresses;
        this.caption = caption;
        this.body = body;
        this.attachments = attachments;
        this.from = from;
    }

    /**
     * Constructor.
     * <pre>
     *     EmailInfo emailInfo = new EmailInfo(
                "john.doe@company.com,jane.roe@company.com",
                "Company news",
                null,
                "Some content",
                ContentBodyType.TEXT
           );
     * </pre>
     *
     * @param addresses             comma or semicolon separated list of addresses
     * @param caption               email subject
     * @param from                  "from" address. If null, a default provided by {@code cuba.email.fromAddress} app property is used.
     * @param body                  email body
     * @param bodyType              email body type like text or html
     * @param attachments           email attachments. Omit this parameter if there are no attachments.
     */
    public EmailInfo(String addresses, String caption, @Nullable String from, String body, ContentBodyType bodyType,
                     EmailAttachment... attachments) {
        this.addresses = addresses;
        this.caption = caption;
        this.body = body;
        this.bodyType = bodyType;
        this.attachments = attachments;
        this.from = from;
    }

    /**
     * Deprecated. Use {@link #EmailInfo(String, String, String, ContentBodyType)} instead.
     *
     * Constructor. The "from" address is taken from the {@code cuba.email.fromAddress} app property.
     * <pre>
     *     EmailInfo emailInfo = new EmailInfo(
                "john.doe@company.com,jane.roe@company.com",
                "Company news",
                "Some content"
            );
     * </pre>
     *
     * @param addresses             comma or semicolon separated list of addresses
     * @param caption               email subject
     * @param body                  email body
     */
    @Deprecated
    public EmailInfo(String addresses, String caption, String body) {
        this.addresses = addresses;
        this.caption = caption;
        this.body = body;
    }

    /**
     * Constructor. The "from" address is taken from the {@code cuba.email.fromAddress} app property.
     * <pre>
     *     EmailInfo emailInfo = new EmailInfo(
               "john.doe@company.com,jane.roe@company.com",
               "Company news",
               "Some content",
               ContentBodyType.TEXT
           );
     * </pre>
     *
     * @param addresses             comma or semicolon separated list of addresses
     * @param caption               email subject
     * @param body                  email body
     * @param bodyType              email body type like text or html
     */
    public EmailInfo(String addresses, String caption, String body, ContentBodyType bodyType) {
        this.addresses = addresses;
        this.caption = caption;
        this.body = body;
        this.bodyType = bodyType;
    }

    public String getAddresses() {
        return addresses;
    }

    public void setAddresses(String addresses) {
        this.addresses = addresses;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public EmailAttachment[] getAttachments() {
        return attachments;
    }

    public void setAttachments(EmailAttachment[] attachments) {
        this.attachments = attachments;
    }

    public Map<String, Serializable> getTemplateParameters() {
        return templateParameters;
    }

    public void setTemplateParameters(Map<String, Serializable> templateParameters) {
        this.templateParameters = templateParameters;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public List<EmailHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(List<EmailHeader> headers) {
        this.headers = headers;
    }

    public void addHeader(String name, String value) {
        if (this.headers == null)
            this.headers = new ArrayList<>();
        this.headers.add(new EmailHeader(name, value));
    }

    public ContentBodyType getBodyType() {
        return bodyType;
    }

    public void setBodyType(ContentBodyType bodyType) {
        this.bodyType = bodyType;
    }
}