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
import io.swagger.models.parameters.RefParameter;
import io.swagger.models.properties.*;
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
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController("cuba_OpenAPIDocumentationController")
@RequestMapping("/v2/rest_docs")
public class OpenAPIDocumentationController {

    protected static final String WEB_HOST_NAME = "cuba.webHostName";
    protected static final String WEB_PORT = "cuba.webPort";
    protected static final String WEB_CONTEXT_NAME = "cuba.webContextName";

    protected static final String QUERIES_CONFIG = "cuba.rest.queriesConfig";
    protected static final String SERVICES_CONFIG = "cuba.rest.servicesConfig";

    protected static final String QUERY_PATH = "/queries/%s/%s";
    protected static final String SERVICE_PATH = "/services/%s/%s";

    protected static final String ARRAY_SIGNATURE = "[]";

    @Inject
    protected Resources resources;

    // cache
    protected Swagger swagger = null;

    protected Map<String, Parameter> parameters = new HashMap<>();

    protected boolean entitiesArePresent = false;
    protected boolean queriesArePresent = false;
    protected boolean servicesArePresent = false;

    @RequestMapping(value = "/rest.json", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
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

    @RequestMapping(value = "/rest.yml", method = RequestMethod.GET, produces = "application/yaml")
    public String getSwaggerYaml() {
        checkSwagger();

        ObjectMapper jsonWriter = new ObjectMapper();
        jsonWriter.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            JsonNode jsonNode = jsonWriter.readTree(
                    jsonWriter.writeValueAsBytes(swagger));

            return new YAMLMapper()
                    .writeValueAsString(jsonNode)
                    .substring("---".length() + 1);
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while generating Swagger documentation", e);
        }
    }

    protected void checkSwagger() {
        /*
         * todo: use condition "swagger == null"
         */
        swagger = new Swagger()
                .host(getHost())
                .basePath(getBasePath())
                .consumes(APPLICATION_JSON_VALUE)
                .produces(APPLICATION_JSON_VALUE)
                .info(generateInfo())
                .paths(generatePaths())
                .tags(generateTags(entitiesArePresent, queriesArePresent, servicesArePresent));

        swagger.setParameters(parameters);
    }

    protected String getHost() {
        return AppContext.getProperty(WEB_HOST_NAME) + ":" + AppContext.getProperty(WEB_PORT);
    }

    protected String getBasePath() {
        return "/" + AppContext.getProperty(WEB_CONTEXT_NAME) + "/rest/v2";
    }

    protected Info generateInfo() {
        return new Info()
                .version("0.1")
                .title("Project REST API")
                .description("Generated Swagger documentation")
                .license(new License()
                        .name("Apache 2.0")
                        .url("http://www.apache.org/licenses/LICENSE-2.0.html"));
    }

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

        paths.putAll(generateQueryPaths());
        paths.putAll(generateServicesPaths());

        return paths;
    }

    protected Map<String, Path> generateServicesPaths() {
        String servicesConfigFiles = AppContext.getProperty(SERVICES_CONFIG);
        if (servicesConfigFiles == null || servicesConfigFiles.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Path> servicesPaths = new HashMap<>();

        for (String servicesConfig : Splitter.on(' ').omitEmptyStrings().trimResults().split(servicesConfigFiles)) {
            Resource configResource = resources.getResource(servicesConfig);
            if (configResource.exists()) {
                InputStream stream = null;
                try {
                    stream = configResource.getInputStream();
                    Element rootElement = Dom4j.readDocument(stream).getRootElement();

                    servicesPaths.putAll(loadPathsFromServicesConfig(rootElement));
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read queries config from " + servicesPaths, e);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }
        }
        servicesArePresent = !servicesPaths.isEmpty();

        return servicesPaths;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Path> loadPathsFromServicesConfig(Element rootElement) {
        Map<String, Path> paths = new HashMap<>();

        for (Element serviceEl : ((List<Element>) rootElement.elements("service"))) {
            String serviceName = serviceEl.attributeValue("name");

            for (Element methodEl : ((List<Element>) serviceEl.elements("method"))) {
                String methodName = methodEl.attributeValue("name");

                List<Pair<String, String>> methodParams = parseParams(methodEl);

                paths.put(
                        String.format(SERVICE_PATH, serviceName, methodName),
                        generateServiceMethodPath(serviceName, methodName, methodParams));
            }
        }

        return paths;
    }

    protected Path generateServiceMethodPath(String serviceName, String methodName, List<Pair<String, String>> params) {
        return new Path()
                .get(generateServiceMethodOp(serviceName, methodName, params, RequestMethod.GET))
                .post(generateServiceMethodOp(serviceName, methodName, params, RequestMethod.POST));
    }

    // todo: make responses shared
    protected Operation generateServiceMethodOp(String serviceName, String methodName, List<Pair<String, String>> params, RequestMethod method) {
        Operation operation = new Operation()
                .tag("Services")
                .summary(serviceName + "#" + methodName)
                .description("Executes the service method. This request expects query parameters with the names defined " +
                        "in services configuration on the middleware")

                .response(200, new Response()
                        .description("Returns the result of the method execution. It can be of simple " +
                                "datatype as well as JSON that represents an entity or entities collection.")
                        .schema(new StringProperty()))

                .response(204, new Response()
                        .description("No content. This status is returned when the service method was executed " +
                                "successfully but returns null or is of void type."))

                .response(403, new Response()
                        .description("Forbidden. The user doesn't have permissions to invoke the service method.")
                        .schema(new ObjectProperty()
                                .property("error", new StringProperty().description("Error message"))
                                .property("details", new StringProperty().description("Detailed error description"))
                        ));

        params.forEach(param ->
                operation.addParameter(
                        generateServiceMethodParam(serviceName, methodName, param, method)
                ));

        return operation;
    }

    protected Parameter generateServiceMethodParam(String serviceName, String methodName, Pair<String, String> param, RequestMethod method) {
        String paramName = serviceName + "_"
                + methodName + "_"
                + param.getFirst() + "_"
                + method.name();

        String ref = "#/parameters/" + paramName;

        if (RequestMethod.GET == method) {
            parameters.put(paramName, generateGetOperationParam(param));
        } else {
            parameters.put(paramName, generatePostOperationParam(param));
        }

        return new RefParameter(ref);
    }

    protected Map<String, Path> generateQueryPaths() {
        String queriesConfigFiles = AppContext.getProperty(QUERIES_CONFIG);
        if (queriesConfigFiles == null || queriesConfigFiles.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Path> queriesPaths = new HashMap<>();

        for (String queriesConfig : Splitter.on(' ').omitEmptyStrings().trimResults().split(queriesConfigFiles)) {
            Resource configResource = resources.getResource(queriesConfig);
            if (configResource.exists()) {
                InputStream stream = null;
                try {
                    stream = configResource.getInputStream();
                    Element rootElement = Dom4j.readDocument(stream).getRootElement();

                    queriesPaths.putAll(loadPathsFromQueriesConfig(rootElement));
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read queries config from " + queriesConfig, e);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }
        }

        queriesArePresent = !queriesPaths.isEmpty();

        return queriesPaths;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Path> loadPathsFromQueriesConfig(Element rootElement) {
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
                .get(generateQueryOperation(entity, queryName, RequestMethod.GET, params))
                .post(generateQueryOperation(entity, queryName, RequestMethod.POST, params));
    }

    protected Operation generateQueryOperation(String entityName, String queryName, RequestMethod method, List<Pair<String, String>> params) {
        Operation operation = new Operation()
                .tag("Queries")
                .summary(queryName)
                .description("Executes a predefined query. Query parameters must be passed in the request body as JSON map")
                .response(200, new Response().description("Success"))
                .response(403, new Response().description("Forbidden. A user doesn't have permissions to read the entity: " + entityName))
                .response(404, new Response().description("MetaClass not found for the entity: " + entityName));

        params.forEach(param ->
                operation.addParameter(
                        generateQueryOperationParam(entityName, queryName, param, method)));

        return operation;
    }

    private Parameter generateQueryOperationParam(String entityName, String queryName, Pair<String, String> param, RequestMethod method) {
        String paramName = entityName + "_"
                + queryName + "_"
                + param.getFirst() + "_"
                + method.name();

        String ref = "#/parameters/" + paramName;

        if (RequestMethod.GET == method) {
            parameters.put(paramName, generateGetOperationParam(param));
        } else {
            parameters.put(paramName, generatePostOperationParam(param));
        }

        return new RefParameter(ref);
    }

    protected QueryParameter generateGetOperationParam(Pair<String, String> param) {
        QueryParameter parameter = new QueryParameter()
                .name(param.getFirst())
                .required(true);

        if (!param.getSecond().contains(ARRAY_SIGNATURE)) {
            parameter.type("string");
        } else {
            parameter.type("array")
                    .items(new StringProperty());
        }
        return parameter;
    }

    protected BodyParameter generatePostOperationParam(Pair<String, String> param) {
        BodyParameter parameter = new BodyParameter()
                .name(param.getFirst());
        parameter.setRequired(true);

        if (!param.getSecond().contains(ARRAY_SIGNATURE)) {
            parameter.schema(
                    new ModelImpl()
                            .property(
                                    param.getFirst(),
                                    getParamProperty(param.getSecond())
                            ));
        } else {
            String javaType = param.getSecond().replace(ARRAY_SIGNATURE, "");
            parameter.schema(
                    new ArrayModel()
                            .items(getParamProperty(javaType)));
        }

        return parameter;
    }

    protected Property getParamProperty(String javaType) {
        switch (javaType) {
            case "boolean":
                return new BooleanProperty().example(true);
            case "float":
            case "double":
                return new DoubleProperty().example("3.14");
            case "byte":
            case "short":
            case "integer":
                return new IntegerProperty().example(42);
            case "long":
                return new LongProperty().example(Long.MAX_VALUE >> 4);
            case "date":
                return new DateProperty().example("2005-14-10T13:17:42.16Z");
            case "uuid":
                UUIDProperty uuidProp = new UUIDProperty();
                uuidProp.setExample("19474a3b-99b5-482e-9e77-852be9adf817");
                return uuidProp;
            case "string":
            default:
                return new StringProperty().example("Hello World!");
        }
    }

    @SuppressWarnings("unchecked")
    protected List<Pair<String, String>> parseQueryParams(Element queryEl) {
        Element paramsEl = queryEl.element("params");
        if (paramsEl == null) {
            return Collections.emptyList();
        }

        return parseParams(paramsEl);
    }

    @SuppressWarnings("unchecked")
    protected List<Pair<String, String>> parseParams(Element paramsEl) {
        return ((List<Element>) paramsEl.elements("param"))
                .stream()
                .map(this::parseParam)
                .collect(Collectors.toList());
    }

    protected Pair<String, String> parseParam(Element paramEl) {
        String name = paramEl.attributeValue("name");

        String type = paramEl.attributeValue("type");
        if (type == null || type.isEmpty()) {
            type = "string";
        } else {
            type = type.substring(type.lastIndexOf(".") + 1)
                    .toLowerCase();
        }

        return new Pair<>(name, type);
    }
}
