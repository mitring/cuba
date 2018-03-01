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

package com.haulmont.restapi.controllers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Splitter;
import com.haulmont.bali.datastruct.Pair;
import com.haulmont.bali.util.Dom4j;
import com.haulmont.cuba.core.global.Resources;
import com.haulmont.cuba.core.sys.AppContext;
import io.swagger.models.*;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.QueryParameter;
import org.apache.commons.io.IOUtils;
import org.dom4j.Element;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController("cuba_OpenAPIDocumentationController")
@RequestMapping("/v2/openapi_docs")
public class OpenAPIDocumentationController {

    protected static final String WEB_CONTEXT_NAME = "cuba.webContextName";

    protected static final String QUERIES_CONFIG = "cuba.rest.queriesConfig";
    protected static final String SERVICES_CONFIG = "cuba.rest.servicesConfig";

    protected static final String QUERY_PATH = "/queries/%s/%s";

    @Inject
    protected Resources resources;

    // cache
    protected Swagger swagger = null;

    @RequestMapping(value = "/openapi.json", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    public String getSwaggerJson() {
        checkSwagger();

        ObjectMapper jsonWriter = new ObjectMapper();
        jsonWriter.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return jsonWriter.writeValueAsString(swagger);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("An error occurred while generating Swagger documentation", e);
        }
    }

    @RequestMapping(value = "/openapi.yml", method = RequestMethod.GET, produces = "application/yaml")
    public String getSwaggerYaml() {
        checkSwagger();

        ObjectMapper jsonWriter = new ObjectMapper();
        jsonWriter.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            JsonNode jsonNode = jsonWriter.readTree(
                    jsonWriter.writeValueAsBytes(swagger));

            return new YAMLMapper()
                    .writeValueAsString(jsonNode)
                    .replace("---", "");
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while generating Swagger documentation", e);
        }
    }

    protected void checkSwagger() {
        /*
         * todo: use condition "swagger == null"
         */
        swagger = new Swagger()
                .basePath(getBasePath())
                .consumes(APPLICATION_JSON_VALUE)
                .produces(APPLICATION_JSON_VALUE)
                .info(generateInfo())
                .paths(generatePaths())
                .tags(generateTags(false, true, false));
    }

    protected String getBasePath() {
        return "/" + AppContext.getProperty(WEB_CONTEXT_NAME) + "/rest/v2";
    }

    protected Info generateInfo() {
        return new Info()
                .version("6.9")
                .title("CUBA Platform REST API")
                .description("Placeholder description")
                .license(new License()
                        .name("Apache 2.0")
                        .url("http://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    // todo: remove
    @SuppressWarnings("SameParameterValue")
    protected List<Tag> generateTags(boolean entitiesArePresent, boolean queriesArePresent, boolean servicesArePresent) {
        List<Tag> tags = new ArrayList<>();

        if (entitiesArePresent) {
            tags.add(new Tag()
                    .name("Entities")
                    .description("CRUD entities operations"));
        }

        if (queriesArePresent) {
            tags.add(new Tag()
                    .name("Queries")
                    .description("Predefined queries execution"));
        }

        if (servicesArePresent) {
            tags.add(new Tag()
                    .name("Services")
                    .description("Middleware services execution"));
        }

        return tags;
    }

    protected Map<String, Path> generatePaths() {
        Map<String, Path> paths = new HashMap<>();

        generateQueryPaths().forEach(paths::put);

        return paths;
    }

    protected Map<String, Path> generateQueryPaths() {
        String queriesConfigFiles = AppContext.getProperty(QUERIES_CONFIG);
        if (queriesConfigFiles == null || queriesConfigFiles.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Path> queriesPaths = new HashMap<>();

        for (String queryConfig : Splitter.on(' ').omitEmptyStrings().trimResults().split(queriesConfigFiles)) {
            Resource configResource = resources.getResource(queryConfig);
            if (configResource.exists()) {
                InputStream stream = null;
                try {
                    stream = configResource.getInputStream();
                    Element rootElement = Dom4j.readDocument(stream).getRootElement();

                    queriesPaths.putAll(loadPathsFromQueryConfig(rootElement));
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read queries config from " + queryConfig, e);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }
        }

        return queriesPaths;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Path> loadPathsFromQueryConfig(Element rootElement) {
        Map<String, Path> paths = new HashMap<>();

        for (Element query : ((List<Element>) rootElement.elements("query"))) {
            String entity = query.attributeValue("entity");
            String queryName = query.attributeValue("name");

            paths.put(
                    String.format(QUERY_PATH, entity, queryName),
                    generateQueryPath(entity, queryName, parseQueryParams(query)));
        }

        return paths;
    }

    protected Path generateQueryPath(String entity, String queryName, List<Pair<String, String>> params) {
        return new Path()
                .get(generateQueryOperation(RequestMethod.GET, params))
                .post(generateQueryOperation(RequestMethod.POST, params));
    }

    protected Operation generateQueryOperation(RequestMethod method, List<Pair<String, String>> params) {
        Operation operation = new Operation()
                .tag("Queries")
                .summary("Execute a query")
                .description("Executes a predefined query. Query parameters must be passed in the request body as JSON map")
                .response(200, new Response().description("Success"))
                .response(403, new Response().description("Forbidden. A user doesn't have permissions to read the entity"))
                .response(404, new Response().description("MetaClass not found"));

        params.forEach(param ->
                operation.addParameter(
                        generateQueryOperationParam(param, method)));

        return operation;
    }

    // todo: consider multiple parameters
    private Parameter generateQueryOperationParam(Pair<String, String> param, RequestMethod method) {
        if (RequestMethod.GET == method) {
            return new QueryParameter()
                    .name(param.getFirst())
                    .type("string");
        } else {
            return new BodyParameter()
                    .name(param.getFirst())
                    .schema(new ModelImpl()
                            .type(param.getSecond()));
        }
    }

    // todo: consider arrays
    @SuppressWarnings("unchecked")
    protected List<Pair<String, String>> parseQueryParams(Element query) {
        Element paramsEl = query.element("params");
        if (paramsEl == null
                || paramsEl.elements() == null
                || paramsEl.elements().isEmpty()) {
            return Collections.emptyList();
        }

        List<Pair<String, String>> params = new ArrayList<>();
        for (Element param : (List<Element>) paramsEl.elements("param")) {
            String name = param.attributeValue("name");
            String type = param.attributeValue("type");
            type = type.substring(type.lastIndexOf(".") + 1).toLowerCase();

            params.add(new Pair<>(name, type));
        }

        return params;
    }
}
