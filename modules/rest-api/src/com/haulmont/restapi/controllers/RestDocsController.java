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
import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.global.Resources;
import com.haulmont.cuba.core.sys.AppContext;
import io.swagger.models.*;
import io.swagger.models.parameters.*;
import io.swagger.models.properties.*;
import org.apache.commons.io.IOUtils;
import org.dom4j.Element;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController("cuba_RestDocsController")
@RequestMapping("/v2/rest_docs")
public class RestDocsController {

    protected static final String WEB_HOST_NAME = "cuba.webHostName";
    protected static final String WEB_PORT = "cuba.webPort";
    protected static final String WEB_CONTEXT_NAME = "cuba.webContextName";

    protected static final String PERSISTENCE_CONFIG = "cuba.persistenceConfig";
    protected static final String QUERIES_CONFIG = "cuba.rest.queriesConfig";
    protected static final String SERVICES_CONFIG = "cuba.rest.servicesConfig";

    protected static final String ENTITY_PATH = "/entities/%s";
    // read, update, delete
    protected static final String ENTITY_RUD_OPS = "/entities/%s/{entityId}";
    protected static final String ENTITY_SEARCH = "/entities/%s/search";

    protected static final String QUERY_PATH = "/queries/%s/%s";
    protected static final String SERVICE_PATH = "/services/%s/%s";

    protected static final String DEFINITIONS_PREFIX = "#/definitions/";
    protected static final String ENTITY_DEFINITION_PREFIX = DEFINITIONS_PREFIX + "entities_";
    protected static final String PARAMETERS_PREFIX = "#/parameters/";

    protected static final String QUERIES_TAG = "Queries";
    protected static final String ENTITIES_TAG = "Entities";
    protected static final String SERVICES_TAG = "Services";

    protected static final String ARRAY_SIGNATURE = "[]";

    @Inject
    protected Resources resources;

    // cache
    protected Swagger swagger = null;

    protected Map<String, Parameter> parameters = new HashMap<>();
    protected Map<String, Model> definitions = new HashMap<>();

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
                    .disable(WRITE_DOC_START_MARKER)
                    .writeValueAsString(jsonNode);
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
        swagger.setDefinitions(definitions);
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
                    .name(ENTITIES_TAG)
                    .description("CRUD entities operations"));
        }

        if (queriesArePresent) {
            tags.add(new Tag()
                    .name(QUERIES_TAG)
                    .description("Predefined queries execution"));
        }

        if (servicesArePresent) {
            tags.add(new Tag()
                    .name(SERVICES_TAG)
                    .description("Middleware services execution"));
        }

        return tags;
    }

    protected Map<String, Path> generatePaths() {
        Map<String, Path> paths = new HashMap<>();

        paths.putAll(generateEntitiesPaths());
        paths.putAll(generateQueryPaths());
        paths.putAll(generateServicesPaths());

        return paths;
    }

    /*
     * Entities
     */

    protected Map<String, Path> generateEntitiesPaths() {
        String persistenceConfigFiles = AppContext.getProperty(PERSISTENCE_CONFIG);
        if (persistenceConfigFiles == null || persistenceConfigFiles.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Path> entitiesPaths = new HashMap<>();

        for (String persistenceConfig : Splitter.on(' ').omitEmptyStrings().trimResults().split(persistenceConfigFiles)) {
            Resource configResource = resources.getResource(persistenceConfig);
            if (configResource.exists()) {
                InputStream stream = null;
                try {
                    stream = configResource.getInputStream();
                    Element rootElement = Dom4j.readDocument(stream).getRootElement();

                    entitiesPaths.putAll(loadPathsFromPersistenceConfig(rootElement));
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read entities from " + persistenceConfig, e);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }
        }
        entitiesArePresent = !entitiesPaths.isEmpty();

        return entitiesPaths;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Path> loadPathsFromPersistenceConfig(Element rootElement) {
        Element persistenceUnitEl = rootElement.element("persistence-unit");
        if (persistenceUnitEl == null) {
            return Collections.emptyMap();
        }

        Map<String, Path> paths = new HashMap<>();
        for (Element eClass : ((List<Element>) persistenceUnitEl.elements("class"))) {
            String classFqn = (String) eClass.getData();
            // todo: temporary. Remove later
            if (!"com.haulmont.cuba.security.entity.User".equals(classFqn)) {
                continue;
            }

            paths.putAll(generateEntityPaths(classFqn));
        }

        return paths;
    }

    protected Map<String, Path> generateEntityPaths(String classFqn) {
        Pair<String, ModelImpl> entityModel = createEntityModel(ReflectionHelper.getClass(classFqn));

        Map<String, Path> entityPaths = new HashMap<>();

        entityPaths.putAll(generateEntityBrowsePath(entityModel));
        entityPaths.putAll(generateEntityCRUDPaths(entityModel));

        Pair<String, Path> searchPath = generateEntityFilterPaths(entityModel);
        entityPaths.put(searchPath.getFirst(), searchPath.getSecond());

        return entityPaths;
    }

    protected Pair<String, Path> generateEntityFilterPaths(Pair<String, ModelImpl> entityModel) {
        return new Pair<>(
                String.format(ENTITY_SEARCH, entityModel.getFirst()),
                new Path()
                        .get(generateEntitySearchOperation(entityModel, RequestMethod.GET))
                        .post(generateEntitySearchOperation(entityModel, RequestMethod.POST)));
    }

    protected Operation generateEntitySearchOperation(Pair<String, ModelImpl> entityModel, RequestMethod method) {
        Operation operation = new Operation()
                .tag(ENTITIES_TAG)
                .produces(APPLICATION_JSON_VALUE)
                .response(200, new Response()
                        .description("Success. Entities that conforms filter conditions are returned in the response body.")
                        .schema(new RefProperty(ENTITY_DEFINITION_PREFIX + entityModel.getFirst())))
                .response(400, new Response().description("Bad request. For example, the condition value cannot be parsed."))
                .response(403, new Response().description("Forbidden. The user doesn't have permissions to read the entity"))
                .response(404, new Response().description("Not found. MetaClass for the entity with the given name not found"));

        if (RequestMethod.GET == method) {
            QueryParameter parameter = new QueryParameter()
                    .name("Filter")
                    .property(new StringProperty().description("JSON with filter definition"));
            operation.parameter(parameter);
        } else {
            BodyParameter parameter = new BodyParameter()
                    .name("Filter")
                    .schema(new ModelImpl().property("JSON with filter definition", new StringProperty()));
            operation.parameter(parameter);
        }

        return operation;
    }

    protected Map<String, Path> generateEntityCRUDPaths(Pair<String, ModelImpl> entityModel) {
        Map<String, Path> crudPaths = new HashMap<>();

        Pair<String, Path> createPath = generateEntityCreatePath(entityModel);
        crudPaths.put(createPath.getFirst(), createPath.getSecond());

        Pair<String, Path> rudPath = generateEntityRUDPaths(entityModel);
        crudPaths.put(rudPath.getFirst(), rudPath.getSecond());

        return crudPaths;
    }

    protected Pair<String, Path> generateEntityRUDPaths(Pair<String, ModelImpl> entityModel) {
        return new Pair<>(
                String.format(ENTITY_RUD_OPS, entityModel.getFirst()),
                new Path()
                        .delete(generateEntityDeleteOperation(entityModel))
                        .get(generateEntityReadOperation(entityModel))
                        .put(generateEntityUpdateOperation(entityModel)));
    }

    protected Operation generateEntityUpdateOperation(Pair<String, ModelImpl> entityModel) {
        String entityRef = ENTITY_DEFINITION_PREFIX + entityModel.getFirst();

        BodyParameter bodyParam = new BodyParameter()
                .name("Entity")
                .schema(new RefModel(entityRef));
        bodyParam.setRequired(true);

        return new Operation()
                .tag(ENTITIES_TAG)
                .produces(APPLICATION_JSON_VALUE)
                .parameter(new PathParameter()
                        .name("entityId")
                        .description("Entity identifier")
                        .required(true)
                        .property(new StringProperty()))
                .parameter(bodyParam)
                .response(200, new Response()
                        .description("Success. The updated entity is returned in the response body.")
                        .schema(new RefProperty(entityRef)))
                .response(403, new Response().description("Forbidden. The user doesn't have permissions to update the entity"))
                .response(404, new Response().description("MetaClass not found or entity with the given identifier not found."));
    }

    protected Operation generateEntityReadOperation(Pair<String, ModelImpl> entityModel) {
        return new Operation()
                .tag(ENTITIES_TAG)
                .produces(APPLICATION_JSON_VALUE)
                .parameter(new PathParameter()
                        .name("entityId")
                        .description("Entity identifier")
                        .property(new StringProperty()))
                .response(200, new Response()
                        .description("Success. The entity is returned in the response body.")
                        .schema(new RefProperty(ENTITY_DEFINITION_PREFIX + entityModel.getFirst())))
                .response(403, new Response().description("Forbidden. The user doesn't have permissions to read the entity"))
                .response(404, new Response().description("MetaClass not found or entity with the five identifier not found."));
    }

    protected Operation generateEntityDeleteOperation(Pair<String, ModelImpl> entityModel) {
        return new Operation()
                .tag(ENTITIES_TAG)
                .produces(APPLICATION_JSON_VALUE)
                .parameter(new PathParameter()
                        .name("entityId")
                        .description("Entity identifier")
                        .property(new StringProperty()))
                .response(200, new Response().description("Success. Entity was deleted."))
                .response(403, new Response().description("Forbidden. The user doesn't have permissions to delete the entity"))
                .response(404, new Response().description("MetaClass not found or entity with the given identifier not found."));
    }

    protected Pair<String, Path> generateEntityCreatePath(Pair<String, ModelImpl> entityModel) {
        Operation createOp = new Operation()
                .tag(ENTITIES_TAG)
                .produces(APPLICATION_JSON_VALUE)
                .response(201, new Response()
                        .description("Entity created. The created entity is returned in the response body.")
                        .schema(new RefProperty(ENTITY_DEFINITION_PREFIX + entityModel.getFirst())));

        return new Pair<>(
                String.format(ENTITY_PATH, entityModel.getFirst()),
                new Path().post(createOp));
    }

    protected Map<String, Path> generateEntityBrowsePath(Pair<String, ModelImpl> entityModel) {
        Operation browseOp = new Operation()
                .tag(ENTITIES_TAG)
                .produces(APPLICATION_JSON_VALUE)
                .response(200, new Response()
                        .description("Success. The list of entities is returned in the response body.")
                        .schema(new RefProperty(ENTITY_DEFINITION_PREFIX + entityModel.getFirst())));

        return Collections.singletonMap(
                String.format(ENTITY_PATH, entityModel.getFirst()),
                new Path().get(browseOp));
    }

    protected Pair<String, ModelImpl> createEntityModel(Class<Object> entityClass) {
        Map<String, Property> properties = new TreeMap<>();

        try {
            Field idField = entityClass.getDeclaredField("id");
            String idType = idField.getType().getSimpleName().toLowerCase();
            Property idProperty = getPropertyFromJavaType(idType);

            properties.put("id", idProperty);
        } catch (NoSuchFieldException ignored) {
        }

        String entityName = "";
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        if (entityAnnotation != null) {
            entityName = entityAnnotation.name();
            properties.put("_entityName", new StringProperty()._default(entityName));
        }

        NamePattern namePatternAnnotation = entityClass.getAnnotation(NamePattern.class);
        if (namePatternAnnotation != null) {
            String namePattern = namePatternAnnotation.value();
            properties.put("_instanceName", new StringProperty().example(namePattern));
        }

        for (Field field : entityClass.getDeclaredFields()) {
            if ("id".equals(field.getName()) || field.getAnnotation(Column.class) == null) {
                continue;
            }

            String fieldName = field.getName();
            Property fieldProperty = getPropertyFromJavaType(field.getType().getSimpleName().toLowerCase());

            properties.put(fieldName, fieldProperty);
        }

        ModelImpl entityModel = new ModelImpl();
        entityModel.setProperties(properties);

        String entityDefinitionRef = "entities_" + entityName;
        definitions.put(entityDefinitionRef, entityModel);

        return new Pair<>(entityName, entityModel);
    }

    /*
     * Services
     */

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
                    throw new RuntimeException("Unable to read queries config from " + servicesConfig, e);
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

                paths.put(
                        String.format(SERVICE_PATH, serviceName, methodName),
                        generateServiceMethodPath(serviceName, methodName, parseParams(methodEl)));
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
                .tag(SERVICES_TAG)
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

        String ref = PARAMETERS_PREFIX + paramName;

        if (RequestMethod.GET == method) {
            parameters.put(paramName, generateGetOperationParam(param));
        } else {
            parameters.put(paramName, generatePostOperationParam(param));
        }

        return new RefParameter(ref);
    }

    /*
     * Queries
     */

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
                .tag(QUERIES_TAG)
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

    protected Parameter generateQueryOperationParam(String entityName, String queryName, Pair<String, String> param, RequestMethod method) {
        String paramName = entityName + "_"
                + queryName + "_"
                + param.getFirst() + "_"
                + method.name();

        if (RequestMethod.GET == method) {
            parameters.put(paramName, generateGetOperationParam(param));
        } else {
            parameters.put(paramName, generatePostOperationParam(param));
        }

        return new RefParameter(PARAMETERS_PREFIX + paramName);
    }

    @SuppressWarnings("unchecked")
    protected List<Pair<String, String>> parseQueryParams(Element queryEl) {
        Element paramsEl = queryEl.element("params");
        if (paramsEl == null) {
            return Collections.emptyList();
        }

        return parseParams(paramsEl);
    }

    /*
     * Common
     */

    protected Parameter generateGetOperationParam(Pair<String, String> param) {
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

    protected Parameter generatePostOperationParam(Pair<String, String> param) {
        BodyParameter parameter = new BodyParameter()
                .name(param.getFirst());
        parameter.setRequired(true);

        if (!param.getSecond().contains(ARRAY_SIGNATURE)) {
            parameter.schema(
                    new ModelImpl()
                            .property(param.getFirst(), getPropertyFromJavaType(param.getSecond())));
        } else {
            String javaType = param.getSecond().replace(ARRAY_SIGNATURE, "");
            parameter.schema(
                    new ArrayModel()
                            .items(getPropertyFromJavaType(javaType)));
        }

        return parameter;
    }

    protected Property getPropertyFromJavaType(String javaType) {
        switch (javaType) {
            case "boolean":
                return new BooleanProperty().example(true);
            case "float":
            case "double":
                return new DoubleProperty().example("3.14");
            case "byte":
            case "short":
            case "int":
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
                return new StringProperty().example("String value example");
        }
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
