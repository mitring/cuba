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
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetadataObject;
import com.haulmont.cuba.core.global.Metadata;
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
import javax.persistence.Entity;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SuppressWarnings("unchecked")
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
    protected static final String ENTITY_RUD_OPS = "/entities/%s/{entityId}";
    protected static final String ENTITY_SEARCH = "/entities/%s/search";

    protected static final String QUERIES_PER_ENTITY_PATH = "/queries/%s";
    protected static final String QUERY_PATH = "/queries/%s/%s";
    protected static final String QUERY_COUNT_PATH = "/queries/%s/%s/count";

    protected static final String SERVICE_PATH = "/services/%s/%s";

    protected static final String DEFINITIONS_PREFIX = "#/definitions/";
    protected static final String PARAMETERS_PREFIX = "#/parameters/";

    protected static final String ENTITY_DEFINITION_PREFIX = DEFINITIONS_PREFIX + "entities_";
    protected static final String GET_PARAM_NAME = "%s_%s_%s_%s";
    protected static final String POST_PARAM_NAME = "%s_%s_paramsObject_%s";

    protected static final String ARRAY_SIGNATURE = "[]";

    @Inject
    protected Resources resources;

    @Inject
    protected Metadata metadata;

    // cache
    protected Swagger swagger = null;

    protected Map<String, Parameter> parameters = new HashMap<>();
    protected Map<String, Model> definitions = new HashMap<>();

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
        Pair<List<Tag>, Map<String, Path>> tagsAndPaths = generatePaths();

        swagger = new Swagger()
                .host(getHost())
                .basePath(getBasePath())
                .consumes(APPLICATION_JSON_VALUE)
                .produces(APPLICATION_JSON_VALUE)
                .info(generateInfo())
                .paths(tagsAndPaths.getSecond())
                .tags(tagsAndPaths.getFirst());

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

    protected Pair<List<Tag>, Map<String, Path>> generatePaths() {
        Map<String, Path> paths = new LinkedHashMap<>();

        Pair<List<String>, Map<String, Path>> entities =
                generatePathsFromConfig(PERSISTENCE_CONFIG, this::loadPathsFromPersistenceConfig);
        paths.putAll(entities.getSecond());

        Pair<List<String>, Map<String, Path>> queries =
                generatePathsFromConfig(QUERIES_CONFIG, this::loadPathsFromQueriesConfig);
        paths.putAll(queries.getSecond());

        Pair<List<String>, Map<String, Path>> services =
                generatePathsFromConfig(SERVICES_CONFIG, this::loadPathsFromServicesConfig);
        paths.putAll(services.getSecond());

        return new Pair<>(generateTags(entities.getFirst(), queries.getFirst(), services.getFirst()), paths);
    }

    protected List<Tag> generateTags(List<String> entities, List<String> queryEntities, List<String> services) {
        List<Tag> tags = new ArrayList<>();

        for (String entity : entities) {
            tags.add(new Tag()
                    .name(entity)
                    .description("Entity CRUD operations"));
        }

        for (String queryEntity : queryEntities) {
            tags.add(new Tag()
                    .name(queryEntity + " Queries")
                    .description("Predefined queries execution"));
        }

        for (String service : services) {
            tags.add(new Tag()
                    .name(service)
                    .description("Middleware services execution"));
        }

        return tags;
    }

    /*
     * Entities
     */

    protected Pair<List<String>, Map<String, Path>> loadPathsFromPersistenceConfig(Element rootElement) {
        Element persistenceUnitEl = rootElement.element("persistence-unit");
        if (persistenceUnitEl == null) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }

        List<String> entities = new ArrayList<>();
        Map<String, Path> paths = new LinkedHashMap<>();

        for (Element eClass : ((List<Element>) persistenceUnitEl.elements("class"))) {
            String classFqn = (String) eClass.getData();
            // todo: temporary. Remove later
            if (!"com.haulmont.cuba.security.entity.User".equals(classFqn)) {
                continue;
            }

            Pair<String, Map<String, Path>> pair = generateEntityPaths(classFqn);
            entities.add(pair.getFirst());
            paths.putAll(pair.getSecond());
        }

        return new Pair<>(entities, paths);
    }

    protected Pair<String, Map<String, Path>> generateEntityPaths(String classFqn) {
        Pair<String, ModelImpl> entityModel = createEntityModel(ReflectionHelper.getClass(classFqn));

        Map<String, Path> entityPaths = new LinkedHashMap<>();

        entityPaths.putAll(generateEntityCRUDPaths(entityModel));

        Pair<String, Path> searchPath = generateEntityFilterPaths(entityModel);
        entityPaths.put(searchPath.getFirst(), searchPath.getSecond());

        return new Pair<>(entityModel.getFirst(), entityPaths);
    }

    protected Map<String, Path> generateEntityCRUDPaths(Pair<String, ModelImpl> entityModel) {
        Map<String, Path> crudPaths = new LinkedHashMap<>();

        Pair<String, Path> createPath = generateEntityCreatePath(entityModel);
        createPath.getSecond()
                .get(generateEntityBrowseOperation(entityModel));

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

    protected Pair<String, Path> generateEntityCreatePath(Pair<String, ModelImpl> entityModel) {
        Operation operation = new Operation()
                .tag(entityModel.getFirst())
                .produces(APPLICATION_JSON_VALUE)
                .summary("Creates new entity.")
                .description("The method expects a JSON with entity object in the request body. " +
                        "The entity object may contain references to other entities.")
                .response(201, new Response()
                        .description("Entity created. The created entity is returned in the response body.")
                        .schema(new RefProperty(ENTITY_DEFINITION_PREFIX + entityModel.getFirst())))
                .response(400, getErrorResponse("Bad request. For example, the entity may have a reference to the non-existing entity."))
                .response(403, getErrorResponse("Forbidden. The user doesn't have permissions to create the entity."))
                .response(404, getErrorResponse("Not found. MetaClass for the entity with the given name not found."));

        BodyParameter entityParam = new BodyParameter()
                .name("entityJson")
                .description("JSON object with the entity")
                .schema(new RefModel(ENTITY_DEFINITION_PREFIX + entityModel.getFirst()));
        entityParam.setRequired(true);

        operation.parameter(entityParam);

        return new Pair<>(
                String.format(ENTITY_PATH, entityModel.getFirst()),
                new Path().post(operation));
    }

    protected Pair<String, Path> generateEntityFilterPaths(Pair<String, ModelImpl> entityModel) {
        return new Pair<>(
                String.format(ENTITY_SEARCH, entityModel.getFirst()),
                new Path()
                        .get(generateEntitySearchOperation(entityModel, RequestMethod.GET))
                        .post(generateEntitySearchOperation(entityModel, RequestMethod.POST)));
    }

    protected Operation generateEntityBrowseOperation(Pair<String, ModelImpl> entityModel) {
        Operation operation = new Operation()
                .tag(entityModel.getFirst())
                .produces(APPLICATION_JSON_VALUE)
                .summary("Gets a list of entities.")
                .description("Gets a list of entities.")
                .response(200, new Response()
                        .description("Success. The list of entities is returned in the response body.")
                        .schema(new ArrayProperty(
                                new RefProperty(ENTITY_DEFINITION_PREFIX + entityModel.getFirst()))))
                .response(403, getErrorResponse("Forbidden. The user doesn't have permissions to read the entity."))
                .response(404, getErrorResponse("Not found. MetaClass for the entity with the given name not found."));

        operation.setParameters(generateEntityOptionalParams(false));

        return operation;
    }

    protected Operation generateEntityReadOperation(Pair<String, ModelImpl> entityModel) {
        Operation operation = new Operation()
                .tag(entityModel.getFirst())
                .produces(APPLICATION_JSON_VALUE)
                .summary("Gets a single entity by identifier")
                .description("Gets a single entity by identifier")
                .parameter(new PathParameter()
                        .name("entityId")
                        .description("Entity identifier")
                        .required(true)
                        .property(new StringProperty()))
                .response(200, new Response()
                        .description("Success. The entity is returned in the response body.")
                        .schema(new RefProperty(ENTITY_DEFINITION_PREFIX + entityModel.getFirst())))
                .response(403, getErrorResponse("Forbidden. The user doesn't have permissions to read the entity."))
                .response(404, getErrorResponse("Not found. MetaClass for the entity with the given name not found."));

        operation.getParameters().addAll(generateEntityOptionalParams(true));

        return operation;
    }

    protected Operation generateEntityUpdateOperation(Pair<String, ModelImpl> entityModel) {
        String entityRef = ENTITY_DEFINITION_PREFIX + entityModel.getFirst();

        BodyParameter entityParam = new BodyParameter()
                .name("entityJson")
                .description("JSON object with the entity")
                .schema(new RefModel(entityRef));
        entityParam.setRequired(true);

        PathParameter entityIdParam = new PathParameter()
                .name("entityId")
                .description("Entity identifier")
                .required(true)
                .property(new StringProperty());

        return new Operation()
                .tag(entityModel.getFirst())
                .produces(APPLICATION_JSON_VALUE)
                .summary("Updates the entity.")
                .description("Updates the entity. Only fields that are passed in the JSON object (the request body) are updated.")
                .parameter(entityIdParam)
                .parameter(entityParam)
                .response(200, new Response()
                        .description("Success. The updated entity is returned in the response body.")
                        .schema(new RefProperty(entityRef)))
                .response(403, getErrorResponse("Forbidden. The user doesn't have permissions to update the entity."))
                .response(404, getErrorResponse("Not found. MetaClass for the entity with the given name not found."));
    }

    protected Operation generateEntityDeleteOperation(Pair<String, ModelImpl> entityModel) {
        return new Operation()
                .tag(entityModel.getFirst())
                .produces(APPLICATION_JSON_VALUE)
                .summary("Deletes the entity.")
                .parameter(new PathParameter()
                        .name("entityId")
                        .description("Entity identifier")
                        .required(true)
                        .property(new StringProperty()))
                .response(200, new Response().description("Success. Entity was deleted."))
                .response(403, getErrorResponse("Forbidden. The user doesn't have permissions to delete the entity"))
                .response(404, getErrorResponse("Not found. MetaClass for the entity with the given name not found."));
    }

    protected Operation generateEntitySearchOperation(Pair<String, ModelImpl> entityModel, RequestMethod method) {
        Operation operation = new Operation()
                .tag(entityModel.getFirst())
                .produces(APPLICATION_JSON_VALUE)
                .summary("Find entities by filter conditions")
                .description("Finds entities by filter conditions. The filter is defined by JSON object that is passed as in URL parameter.")
                .response(200, new Response()
                        .description("Success. Entities that conforms filter conditions are returned in the response body.")
                        .schema(new RefProperty(ENTITY_DEFINITION_PREFIX + entityModel.getFirst())))
                .response(400, getErrorResponse("Bad request. For example, the condition value cannot be parsed."))
                .response(403, getErrorResponse("Forbidden. The user doesn't have permissions to read the entity."))
                .response(404, getErrorResponse("Not found. MetaClass for the entity with the given name not found."));

        operation.setParameters(generateEntityOptionalParams(false));

        if (RequestMethod.GET == method) {
            QueryParameter parameter = new QueryParameter()
                    .name("filter")
                    .required(true)
                    .property(new StringProperty().description("JSON with filter definition"));
            operation.parameter(parameter);
        } else {
            BodyParameter parameter = new BodyParameter()
                    .name("filter")
                    .schema(new ModelImpl()
                            .property("JSON with filter definition", new StringProperty()));
            parameter.setRequired(true);
            operation.parameter(parameter);
        }

        return operation;
    }

    protected List<Parameter> generateEntityOptionalParams(boolean singleEntity) {
        List<Parameter> singleEntityParams = Arrays.asList(
                new QueryParameter()
                        .name("dynamicAttributes")
                        .description("Specifies whether entity dynamic attributes should be returned.")
                        .property(new BooleanProperty()),
                new QueryParameter()
                        .name("returnNulls")
                        .description("Specifies whether null fields will be written to the result JSON.")
                        .property(new BooleanProperty()),
                new QueryParameter()
                        .name("view")
                        .description("Name of the view which is used for loading the entity. Specify this parameter " +
                                "if you want to extract entities with the view other than it is defined " +
                                "in the REST queries configuration file.")
                        .property(new StringProperty())
        );

        if (singleEntity) {
            return singleEntityParams;
        }

        List<Parameter> multipleEntityParams = new ArrayList<>(Arrays.asList(
                new QueryParameter()
                        .name("returnCount")
                        .description("Specifies whether the total count of entities should be returned in the " +
                                "'X-Total-Count' header.")
                        .property(new BooleanProperty()),
                new QueryParameter()
                        .name("offset")
                        .description("Position of the first result to retrieve.")
                        .property(new StringProperty()),
                new QueryParameter()
                        .name("limit")
                        .description("Number of extracted entities.")
                        .property(new StringProperty()),
                new QueryParameter()
                        .name("sort")
                        .description("Name of the field to be sorted by. If the name is preceding by the '+' " +
                                "character, then the sort order is ascending, if by the '-' character then " +
                                "descending. If there is no special character before the property name, then " +
                                "ascending sort will be used.")
                        .property(new StringProperty())
        ));
        multipleEntityParams.addAll(singleEntityParams);

        return multipleEntityParams;
    }

    protected Pair<String, ModelImpl> createEntityModel(Class<Object> entityClass) {
        Map<String, Property> properties = new LinkedHashMap<>();

        Map<String, MetaProperty> entityProperties = metadata.getClassNN(entityClass).getProperties()
                .stream()
                .collect(Collectors.toMap(MetadataObject::getName, p -> p));

        properties.put("id", getIdProperty(entityProperties.get("id")));

        StringProperty entityNameProperty = getEntityNameProperty(entityClass, properties);
        properties.put("_entityName", entityNameProperty);

        properties.put("_instanceName", getNamePatternProperty(entityClass));

        for (Map.Entry<String, MetaProperty> fieldEntry : entityProperties.entrySet()) {
            String fieldName = fieldEntry.getKey();
            MetaProperty metaProperty = fieldEntry.getValue();

            if ("id".equals(fieldName)) {
                continue;
            }

            String javaType = metaProperty.getJavaType().getSimpleName().toLowerCase();
            Property fieldProperty = getPropertyFromJavaType(javaType);
            properties.put(fieldName, fieldProperty);
        }

        ModelImpl entityModel = new ModelImpl();
        entityModel.setProperties(properties);

        String entityName = entityNameProperty.getDefault();
        String entityDefinitionRef = "entities_" + entityName;
        definitions.put(entityDefinitionRef, entityModel);

        return new Pair<>(entityName, entityModel);
    }

    protected Property getIdProperty(MetaProperty idProperty) {
        Class<?> idType = idProperty.getJavaType();
        String type = idType.getSimpleName().toLowerCase();
        return getPropertyFromJavaType(type);
    }

    protected Property getNamePatternProperty(Class<Object> entityClass) {
        Property namePatternProperty = new StringProperty();
        NamePattern namePatternAnnotation = entityClass.getAnnotation(NamePattern.class);
        if (namePatternAnnotation != null) {
            namePatternProperty.setDefault(namePatternAnnotation.value());
        }
        return namePatternProperty;
    }

    protected StringProperty getEntityNameProperty(Class<Object> entityClass, Map<String, Property> properties) {
        StringProperty entityNameProperty = new StringProperty();
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        if (entityAnnotation != null) {
            entityNameProperty.setDefault(entityAnnotation.name());
        }
        return entityNameProperty;
    }

    /*
     * Services
     */

    protected Pair<List<String>, Map<String, Path>> loadPathsFromServicesConfig(Element rootElement) {
        Set<String> services = new LinkedHashSet<>();
        Map<String, Path> paths = new LinkedHashMap<>();

        for (Element serviceEl : ((List<Element>) rootElement.elements("service"))) {
            String serviceName = serviceEl.attributeValue("name");
            services.add(serviceName);

            for (Element methodEl : ((List<Element>) serviceEl.elements("method"))) {
                String methodName = methodEl.attributeValue("name");

                paths.put(
                        String.format(SERVICE_PATH, serviceName, methodName),
                        generateServiceMethodPath(serviceName, methodName, parseParams(methodEl)));
            }
        }

        return new Pair<>(new ArrayList<>(services), paths);
    }

    protected Path generateServiceMethodPath(String serviceName, String methodName, List<Pair<String, String>> params) {
        return new Path()
                .get(generateServiceMethodOp(serviceName, methodName, params, RequestMethod.GET))
                .post(generateServiceMethodOp(serviceName, methodName, params, RequestMethod.POST));
    }

    protected Operation generateServiceMethodOp(String service, String method, List<Pair<String, String>> params,
                                                RequestMethod requestMethod) {
        Operation operation = new Operation()
                .tag(service)
                .produces(APPLICATION_JSON_VALUE)
                .summary(service + "#" + method)
                .description("Executes the service method. This request expects query parameters with the names defined " +
                        "in services configuration on the middleware.")
                .response(200, new Response()
                        .description("Returns the result of the method execution. It can be of simple datatype " +
                                "as well as JSON that represents an entity or entities collection.")
                        .schema(new StringProperty()))
                .response(204, new Response().description("No content. This status is returned when the service " +
                        "method was executed successfully but returns null or is of void type."))
                .response(403, getErrorResponse("Forbidden. The user doesn't have permissions to invoke the service method."));

        operation.setParameters(generateServiceMethodParams(service, method, params, requestMethod));

        return operation;
    }

    protected List<Parameter> generateServiceMethodParams(String service, String method, List<Pair<String, String>> params,
                                                          RequestMethod requestMethod) {
        if (RequestMethod.GET == requestMethod) {
            return params.stream()
                    .map(p -> generateGetServiceOperationParam(service, method, p, requestMethod))
                    .collect(Collectors.toList());
        } else {
            String paramName = String.format(POST_PARAM_NAME, service, method, requestMethod.name());

            parameters.put(paramName, generatePostOperationParam(params));

            return Collections.singletonList(new RefParameter(PARAMETERS_PREFIX + paramName));
        }
    }

    protected Parameter generateGetServiceOperationParam(String service, String method, Pair<String, String> param,
                                                         RequestMethod requestMethod) {
        String paramName = String.format(GET_PARAM_NAME, service, method, param.getFirst(), requestMethod.name());

        parameters.put(paramName, generateGetOperationParam(param));

        return new RefParameter(PARAMETERS_PREFIX + paramName);
    }

    /*
     * Queries
     */

    protected Pair<List<String>, Map<String, Path>> loadPathsFromQueriesConfig(Element rootElement) {
        Set<String> queryEntities = new LinkedHashSet<>();
        Map<String, Path> paths = new LinkedHashMap<>();

        for (Element query : ((List<Element>) rootElement.elements("query"))) {
            String entity = query.attributeValue("entity");
            queryEntities.add(entity);

            String queryName = query.attributeValue("name");

            List<Pair<String, String>> params = parseQueryParams(query);

            paths.put(
                    String.format(QUERY_PATH, entity, queryName),
                    generateQueryPath(entity, queryName, params));
        }

        return new Pair<>(new ArrayList<>(queryEntities), paths);
    }

    protected Path generateQueryPath(String entity, String queryName, List<Pair<String, String>> params) {
        return new Path()
                .get(generateQueryOperation(entity, queryName, params, RequestMethod.GET))
                .post(generateQueryOperation(entity, queryName, params, RequestMethod.POST));
    }

    protected Operation generateQueryOperation(String entityName, String queryName, List<Pair<String, String>> params,
                                               RequestMethod method) {
        Operation operation = new Operation()
                .tag(entityName + " Queries")
                .produces(APPLICATION_JSON_VALUE)
                .summary(queryName)
                .description("Executes a predefined query. Query parameters must be passed in the request body as JSON map.")
                .response(200, new Response().description("Success"))
                .response(403, getErrorResponse("Forbidden. A user doesn't have permissions to read the entity."))
                .response(404, getErrorResponse("Not found. MetaClass for the entity with the given name not found."));

        operation.setParameters(generateQueryOperationParams(entityName, queryName, params, method, true));

        return operation;
    }

    protected List<Parameter> generateQueryOperationParams(String entity, String query, List<Pair<String, String>> params,
                                                           RequestMethod requestMethod, boolean generateOptionalParams) {
        List<Parameter> optionalParams = new ArrayList<>();
        if (generateOptionalParams) {
            optionalParams.addAll(generateOptionalQueryParams());
        }

        if (RequestMethod.GET == requestMethod) {
            List<Parameter> queryParams = params.stream()
                    .map(p -> generateGetQueryOpParam(entity, query, p, requestMethod))
                    .collect(Collectors.toList());

            queryParams.addAll(optionalParams);

            return queryParams;
        } else {
            String paramName = String.format(POST_PARAM_NAME, entity, query, requestMethod.name());

            parameters.put(paramName, generatePostOperationParam(params));

            List<Parameter> queryParams = new ArrayList<>();
            queryParams.add(new RefParameter(PARAMETERS_PREFIX + paramName));
            queryParams.addAll(optionalParams);

            return queryParams;
        }
    }

    protected List<Parameter> generateOptionalQueryParams() {
        return Arrays.asList(
                new QueryParameter()
                        .name("dynamicAttributes")
                        .description("Specifies whether entity dynamic attributes should be returned.")
                        .property(new BooleanProperty()),
                new QueryParameter()
                        .name("returnCount")
                        .description("Specifies whether the total count of entities should be returned in the " +
                                "'X-Total-Count' header.")
                        .property(new BooleanProperty()),
                new QueryParameter()
                        .name("returnNulls")
                        .description("Specifies whether null fields will be written to the result JSON.")
                        .property(new BooleanProperty()),
                new QueryParameter()
                        .name("view")
                        .description("Name of the view which is used for loading the entity. Specify this parameter " +
                                "if you want to extract entities with the view other than it is defined in the REST " +
                                "queries configuration file.")
                        .property(new StringProperty()),
                new QueryParameter()
                        .name("offset")
                        .description("Position of the first result to retrieve.")
                        .property(new StringProperty()),
                new QueryParameter()
                        .name("limit")
                        .description("Number of extracted entities.")
                        .property(new StringProperty())
        );
    }

    protected Parameter generateGetQueryOpParam(String entity, String query, Pair<String, String> param,
                                                RequestMethod requestMethod) {
        String paramName = String.format(GET_PARAM_NAME, entity, query, param.getFirst(), requestMethod.name());

        parameters.put(paramName, generateGetOperationParam(param));

        return new RefParameter(PARAMETERS_PREFIX + paramName);
    }

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

    protected Pair<List<String>, Map<String, Path>> generatePathsFromConfig(final String config,
                                        Function<Element, Pair<List<String>, Map<String, Path>>> pathsGenerator) {
        String configFiles = AppContext.getProperty(config);
        if (configFiles == null || configFiles.isEmpty()) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }

        List<String> tagSources = new ArrayList<>();
        Map<String, Path> paths = new LinkedHashMap<>();

        Iterable<String> configs = Splitter.on(' ')
                .omitEmptyStrings()
                .trimResults()
                .split(configFiles);

        for (String configFile : configs) {
            Pair<List<String>, Map<String, Path>> pair =
                    generatePathsFromConfigResource(resources.getResource(configFile), pathsGenerator);

            tagSources.addAll(pair.getFirst());
            paths.putAll(pair.getSecond());
        }

        return new Pair<>(tagSources, paths);
    }

    protected Pair<List<String>, Map<String, Path>> generatePathsFromConfigResource(Resource resource,
                                        Function<Element, Pair<List<String>, Map<String, Path>>> pathsGenerator) {
        if (!resource.exists()) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }

        InputStream stream = null;
        try {
            stream = resource.getInputStream();
            Element rootElement = Dom4j.readDocument(stream).getRootElement();

            return pathsGenerator.apply(rootElement);
        } catch (IOException e) {
            throw new RuntimeException("Unable to generate paths from " + resource.getFilename(), e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

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

    protected Parameter generatePostOperationParam(List<Pair<String, String>> params) {
        BodyParameter parameter = new BodyParameter()
                .name("paramsObject");
        parameter.setRequired(true);

        ModelImpl parameterModel = new ModelImpl();
        for (Pair<String, String> param : params) {
            if (!param.getSecond().contains(ARRAY_SIGNATURE)) {
                parameterModel.addProperty(param.getFirst(), getPropertyFromJavaType(param.getSecond()));
            } else {
                String javaType = param.getSecond().replace(ARRAY_SIGNATURE, "");
                parameterModel.addProperty(param.getFirst(), new ArrayProperty(getPropertyFromJavaType(javaType)));
            }
        }
        parameter.schema(parameterModel);

        return parameter;
    }

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

    protected Property getErrorSchema() {
        return new ObjectProperty()
                .property("error", new StringProperty().description("Error message"))
                .property("details", new StringProperty().description("Detailed error description"));
    }

    protected Response getErrorResponse(String msg) {
        return new Response()
                .description(msg)
                .schema(getErrorSchema());
    }
}
