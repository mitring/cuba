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

package com.haulmont.cuba.core.sys;

import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.global.ScriptExecutionPolicy;
import com.haulmont.cuba.core.global.Scripting;
import com.haulmont.cuba.core.sys.javacl.JavaClassLoader;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceConnector;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractScripting implements Scripting {

    private final Logger log = LoggerFactory.getLogger(AbstractScripting.class);

    private static final Pattern IMPORT_PATTERN = Pattern.compile("\\bimport\\b\\s+");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("\\bpackage\\b\\s+.+");
    protected JavaClassLoader javaClassLoader;
    protected SpringBeanLoader springBeanLoader;
    protected String groovyClassPath;

    protected Set<String> imports = new HashSet<>();

    protected volatile GroovyScriptEngine gse;
    protected volatile CubaGroovyClassLoader gcl;
    protected GenericKeyedObjectPool<String, Script> pool;

    protected GlobalConfig globalConfig;

    public AbstractScripting(JavaClassLoader javaClassLoader, Configuration configuration, SpringBeanLoader springBeanLoader) {
        this.javaClassLoader = javaClassLoader;
        this.springBeanLoader = springBeanLoader;
        globalConfig = configuration.getConfig(GlobalConfig.class);
        groovyClassPath = globalConfig.getConfDir() + File.pathSeparator;

        String classPathProp = AppContext.getProperty("cuba.groovyClassPath");
        if (StringUtils.isNotBlank(classPathProp)) {
            String[] strings = classPathProp.split(";");
            for (String string : strings) {
                if (!groovyClassPath.contains(string.trim() + File.pathSeparator))
                    groovyClassPath = groovyClassPath + string.trim() + File.pathSeparator;
            }
        }

        String importProp = AppContext.getProperty("cuba.groovyEvaluatorImport");
        if (StringUtils.isNotBlank(importProp)) {
            String[] strings = importProp.split("[,;]");
            for (String string : strings) {
                imports.add(string.trim());
            }
        }
    }

    protected abstract String[] getScriptEngineRootPath();

    protected GroovyScriptEngine getGroovyScriptEngine() {
        if (gse == null) {
            synchronized (this) {
                if (gse == null) {
                    gse = new GroovyScriptEngine(new CubaResourceConnector(), getGroovyClassLoader());
                }
            }
        }
        return gse;
    }

    protected CubaGroovyClassLoader getGroovyClassLoader() {
        if (gcl == null) {
            synchronized (this) {
                if (gcl == null) {
                    CompilerConfiguration cc = new CompilerConfiguration();
                    cc.setClasspath(groovyClassPath);
                    cc.setRecompileGroovySource(true);
                    gcl = new CubaGroovyClassLoader(cc);
                }
            }
        }
        return gcl;
    }

    private synchronized GenericKeyedObjectPool<String, Script> getPool() {
        if (pool == null) {
            GenericKeyedObjectPoolConfig poolConfig = new GenericKeyedObjectPoolConfig();
            poolConfig.setMaxTotalPerKey(-1);
            poolConfig.setMaxIdlePerKey(globalConfig.getGroovyEvaluationPoolMaxIdle());
            pool = new GenericKeyedObjectPool<>(
                    new BaseKeyedPooledObjectFactory<String, Script>() {
                        @Override
                        public Script create(String key) {
                            return createScript(key);
                        }

                        @Override
                        public PooledObject<Script> wrap(Script value) {
                            return new DefaultPooledObject<>(value);
                        }
                    },
                    poolConfig
            );
        }
        return pool;
    }

    protected Script createScript(String text) {
        StringBuilder sb = new StringBuilder();
        for (String importItem : imports) {
            sb.append("import ").append(importItem).append("\n");
        }

        Matcher matcher = IMPORT_PATTERN.matcher(text);
        String result;
        if (matcher.find()) {
            StringBuffer s = new StringBuffer();
            matcher.appendReplacement(s, sb + "$0");
            result = matcher.appendTail(s).toString();
        } else {
            Matcher packageMatcher = PACKAGE_PATTERN.matcher(text);
            if (packageMatcher.find()) {
                StringBuffer s = new StringBuffer();
                packageMatcher.appendReplacement(s, "$0\n" + sb);
                result = packageMatcher.appendTail(s).toString();
            } else {
                result = sb.append(text).toString();
            }
        }

        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setClasspath(groovyClassPath);
        cc.setRecompileGroovySource(true);
        GroovyShell shell = new GroovyShell(javaClassLoader, new Binding(), cc);
        //noinspection UnnecessaryLocalVariable
        Script script = shell.parse(result);
        return script;
    }

    protected Binding createBinding(Map<String, Object> map) {
        Binding binding = new Binding();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            binding.setVariable(entry.getKey(), entry.getValue());
        }

        return binding;
    }

    @Override
    public <T> T evaluateGroovy(String text, Binding binding, ScriptExecutionPolicy... policies) {
        boolean useCompilationCache = policies == null ||
                !Arrays.asList(policies).contains(ScriptExecutionPolicy.DO_NOT_USE_COMPILE_CACHE);
        Script script = null;
        Object result;
        try {
            script = useCompilationCache ? getPool().borrowObject(text) : createScript(text);
            script.setBinding(binding);
            result = script.run();
        } catch (Exception e) {
            if (script != null && useCompilationCache) {
                try {
                    getPool().invalidateObject(text, script);
                } catch (Exception e1) {
                    log.warn("Error invalidating object in the pool", e1);
                }
            }
            if (e instanceof RuntimeException)
                throw ((RuntimeException) e);
            else
                throw new RuntimeException("Error evaluating Groovy expression", e);
        }
        if (useCompilationCache) {
            try {
                script.setBinding(null); // free memory
                getPool().returnObject(text, script);
            } catch (Exception e) {
                log.warn("Error returning object into the pool", e);
            }
        }
        //noinspection unchecked
        return (T) result;
    }

    @Override
    public <T> T evaluateGroovy(String text, Binding binding) {
        return evaluateGroovy(text, binding, (ScriptExecutionPolicy[]) null);
    }

    @Override
    public <T> T evaluateGroovy(String text, Map<String, Object> context) {
        Binding binding = createBinding(context);
        //noinspection unchecked
        return (T) evaluateGroovy(text, binding);
    }

    @Override
    public <T> T runGroovyScript(String name, Binding binding) {
        try {
            //noinspection unchecked
            return (T) getGroovyScriptEngine().run(name, binding);
        } catch (ResourceException e) {
            // Perhaps the Groovy source not found - it is possible when we run tests. Let's try to find a
            // compiled script in the classpath
            if (name.endsWith(".groovy"))
                name = name.substring(0, name.length() - 7);
            if (name.startsWith("/"))
                name = name.substring(1);
            name = name.replace("/", ".");

            Class scriptClass = loadClass(name);
            if (scriptClass != null && groovy.lang.Script.class.isAssignableFrom(scriptClass)) {
                try {
                    @SuppressWarnings("unchecked")
                    Constructor constructor = scriptClass.getDeclaredConstructor();
                    Script script = (Script) constructor.newInstance();
                    script.setBinding(binding);
                    //noinspection unchecked
                    return (T) script.run();
                } catch (InstantiationException | IllegalAccessException
                        | NoSuchMethodException | InvocationTargetException e1) {
                    throw new RuntimeException("Error instantiating Script object", e1);
                }
            }
            throw new RuntimeException("Error running Groovy script", e);
        } catch (ScriptException e) {
            throw new RuntimeException("Error running Groovy script", e);
        }
    }

    @Override
    public <T> T runGroovyScript(String name, Map<String, Object> context) {
        Binding binding = createBinding(context);
        //noinspection unchecked
        return (T) runGroovyScript(name, binding);
    }

    @Override
    public ClassLoader getClassLoader() {
        return getGroovyClassLoader();
    }

    @Override
    public Class<?> loadClass(String name) {
        try {
            return getGroovyClassLoader().loadClass(name, true, false);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public Class<?> loadClassNN(String name) {
        try {
            return getGroovyClassLoader().loadClass(name, true, false);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load class", e);
        }
    }

    @Override
    public boolean removeClass(String name) {
        return getGroovyClassLoader().removeClass(name) || javaClassLoader.removeClass(name);
    }

    @Override
    public void clearCache() {
        getGroovyClassLoader().clearCache();
        javaClassLoader.clearCache();
        getPool().clear();
        GroovyScriptEngine gse = getGroovyScriptEngine();
        try {
            Field scriptCacheField = gse.getClass().getDeclaredField("scriptCache");
            scriptCacheField.setAccessible(true);
            Map scriptCacheMap = (Map) scriptCacheField.get(gse);
            scriptCacheMap.clear();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //ignore the exception
        }
    }

    protected class CubaResourceConnector implements ResourceConnector {

        /**
         * This implementation works for sources located in conf directory or packed into JARs.
         * It will throw ResourceException for resources in class directories, which is the case for running tests.
         * @param resourceName          resource to load
         * @return                      connection to the resource
         * @throws ResourceException    if the requested resource can not be loaded
         */
        @Override
        public URLConnection getResourceConnection(String resourceName) throws ResourceException {
            URLConnection groovyScriptConn = null;
            StringBuilder errors = new StringBuilder();
            String[] rootPath = getScriptEngineRootPath();

            // First workaround for invocation from GroovyScriptEngine.isSourceNewer()
            for (String path : rootPath) {
                String substrResourceName = resourceName.substring(1);
                path = path.replace('\\', '/');
                if (substrResourceName.startsWith(path))
                    resourceName = substrResourceName;
                if (resourceName.startsWith(path)) {
                    // We came here from GroovyScriptEngine.isSourceNewer() and previously we've loaded the script
                    // from conf
                    File file = new File(resourceName);
                    if (file.exists()) {
                        try {
                            URL url = file.toURI().toURL();
                            groovyScriptConn = url.openConnection();
                            // Make sure we can open it, if we can't it doesn't exist.
                            groovyScriptConn.getInputStream();
                            break;
                        } catch (IOException e) {
                            groovyScriptConn = null;
                            errors.append(e.toString()).append("\n");
                        }
                    }
                }
            }
            if (groovyScriptConn != null)
                return groovyScriptConn;

            // Second workaround for invocation from GroovyScriptEngine.isSourceNewer()
            try {
                // Check if the resourceName is a valid URL. If it is and if we can open connection, use it
                if (resourceName.startsWith("file:") && resourceName.contains(".jar!"))
                    resourceName = "jar:" + resourceName;

                URL resourceUrl = new URL(resourceName);
                groovyScriptConn = resourceUrl.openConnection();
                // Make sure we can open it, if we can't it doesn't exist.
                groovyScriptConn.getInputStream();
            } catch (MalformedURLException e) {
                // Not an URL, just continue
            } catch (IOException e) {
                groovyScriptConn = null;
                errors.append(e.toString()).append("\n");
            }
            if (groovyScriptConn != null)
                return groovyScriptConn;

            // Next try to find a source in conf.
            String fileName = resourceName.endsWith(".groovy") ? resourceName : resourceName + ".groovy";
            for (String root : rootPath) {
                File file = new File(root, fileName);
                if (file.exists()) {
                    try {
                        URL url = file.toURI().toURL();
                        groovyScriptConn = url.openConnection();
                        // Make sure we can open it, if we can't it doesn't exist.
                        groovyScriptConn.getInputStream();
                        break;
                    } catch (IOException e) {
                        groovyScriptConn = null;
                        errors.append(e.toString()).append("\n");
                    }
                } else {
                    errors.append("File ").append(file).append(" doesn't exist").append("\n");
                }
            }
            if (groovyScriptConn != null)
                return groovyScriptConn;

            // Next try to find a source groovy file in the classpath
            URL url = getClass().getResource(fileName);
            if (url != null) {
                try {
                    groovyScriptConn = url.openConnection();
                    // Make sure we can open it, if we can't it doesn't exist.
                    groovyScriptConn.getInputStream();
                } catch (IOException e) {
                    groovyScriptConn = null;
                    errors.append(e.toString()).append("\n");
                }
            } else {
                errors.append("Classpath resource ").append(fileName).append(" doesn't exist").append("\n");
            }
            if (groovyScriptConn != null)
                return groovyScriptConn;

            errors.insert(0, "Unable to find resource " + resourceName + ":\n");
            throw new ResourceException(errors.toString());
        }
    }

    protected class CubaGroovyClassLoader extends GroovyClassLoader {

        public CubaGroovyClassLoader(CompilerConfiguration cc) {
            super(AbstractScripting.this.javaClassLoader, cc);
        }

        public boolean removeClass(String className) {
            Class clazz = getClassCacheEntry(className);
            removeClassCacheEntry(className);
            return clazz != null;
        }

        // This overridden method is almost identical to super, but prefers Groovy source over parent classloader class
        @Override
        public Class loadClass(String name, boolean lookupScriptFiles, boolean preferClassOverScript, boolean resolve) throws ClassNotFoundException, CompilationFailedException {
            // look into cache
            Class cls = getClassCacheEntry(name);

            // enable recompilation?
            boolean recompile = isRecompilable(cls);
            if (!recompile) return cls;

            ClassNotFoundException last = null;

            // prefer class if no recompilation
            if (cls != null && preferClassOverScript) return cls;

            // we want to recompile if needed
            if (lookupScriptFiles) {
                // try groovy file
                try {
                    // check if recompilation already happened.
                    final Class classCacheEntry = getClassCacheEntry(name);
                    if (classCacheEntry != cls) return classCacheEntry;
                    URL source = getResourceLoader().loadGroovySource(name);
                    // if recompilation fails, we want cls==null
                    Class oldClass = cls;
                    cls = null;
                    cls = recompile(source, name, oldClass);
                } catch (IOException ioe) {
                    last = new ClassNotFoundException("IOException while opening groovy source: " + name, ioe);
                } finally {
                    if (cls == null) {
                        removeClassCacheEntry(name);
                    } else {
                        setClassCacheEntry(cls);
                        springBeanLoader.updateContext(Collections.singletonList(cls));
                    }
                }
            }

            if (cls == null) {
                // try parent loader
                try {
                    Class parentClassLoaderClass = super.loadClass(name, false, true, resolve);
                    // return if the parent loader was successful
                    if (parentClassLoaderClass != null)
                        return parentClassLoaderClass;
                } catch (ClassNotFoundException cnfe) {
                    last = cnfe;
                } catch (NoClassDefFoundError ncdfe) {
                    if (ncdfe.getMessage().indexOf("wrong name") > 0) {
                        last = new ClassNotFoundException(name);
                    } else {
                        throw ncdfe;
                    }
                }
                // no class found, there should have been an exception before now
                if (last == null) throw new AssertionError(true);
                throw last;
            }
            return cls;
        }
    }
}