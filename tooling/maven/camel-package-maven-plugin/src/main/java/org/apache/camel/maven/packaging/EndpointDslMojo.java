/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.maven.packaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Generated;

import org.apache.camel.maven.packaging.generics.GenericsUtil;
import org.apache.camel.maven.packaging.model.ComponentModel;
import org.apache.camel.maven.packaging.model.ComponentOptionModel;
import org.apache.camel.maven.packaging.model.EndpointOptionModel;
import org.apache.camel.maven.packaging.srcgen.GenericType;
import org.apache.camel.maven.packaging.srcgen.GenericType.BoundType;
import org.apache.camel.maven.packaging.srcgen.JavaClass;
import org.apache.camel.maven.packaging.srcgen.Method;
import org.apache.camel.maven.packaging.srcgen.Property;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;
import org.apache.camel.spi.UriPath;
import org.apache.camel.spi.annotations.Component;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jboss.forge.roaster.model.util.Strings;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.springframework.boot.liquibase.SpringPackageScanClassResolver;

import static org.apache.camel.maven.packaging.AbstractGeneratorMojo.updateResource;
import static org.apache.camel.maven.packaging.JSonSchemaHelper.getSafeValue;
import static org.apache.camel.maven.packaging.PackageHelper.findCamelCoreDirectory;
import static org.apache.camel.maven.packaging.PackageHelper.loadText;

/**
 * Generate Spring Boot auto configuration files for Camel components and data
 * formats.
 */
@Mojo(name = "generate-endpoint-dsl", threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class EndpointDslMojo extends AbstractMojo {

    private static final Map<String, String> PRIMITIVEMAP;

    static {
        PRIMITIVEMAP = new HashMap<>();
        PRIMITIVEMAP.put("boolean", "java.lang.Boolean");
        PRIMITIVEMAP.put("char", "java.lang.Character");
        PRIMITIVEMAP.put("long", "java.lang.Long");
        PRIMITIVEMAP.put("int", "java.lang.Integer");
        PRIMITIVEMAP.put("integer", "java.lang.Integer");
        PRIMITIVEMAP.put("byte", "java.lang.Byte");
        PRIMITIVEMAP.put("short", "java.lang.Short");
        PRIMITIVEMAP.put("double", "java.lang.Double");
        PRIMITIVEMAP.put("float", "java.lang.Float");
    }

    private static final String[] IGNORE_MODULES = {/* Non-standard -> */ };

    /**
     * The maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * The project build directory
     */
    @Parameter(defaultValue = "${project.build.directory}")
    protected File buildDir;

    /**
     * The base directory
     */
    @Parameter(defaultValue = "${basedir}")
    protected File baseDir;

    DynamicClassLoader projectClassLoader;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Do not generate code for ignored module
        if (Arrays.asList(IGNORE_MODULES).contains(project.getArtifactId())) {
            getLog().info("Component auto-configuration will not be created: component contained in the ignore list");
            return;
        }

        executeAll();
    }

    private void executeAll() throws MojoExecutionException, MojoFailureException {
        Map<File, Supplier<String>> files = PackageHelper.findJsonFiles(buildDir, p -> p.isDirectory() || p.getName().endsWith(".json")).stream()
            .collect(Collectors.toMap(Function.identity(), s -> cache(() -> loadJson(s))));

        executeComponent(files);
    }

    private static String loadJson(File file) {
        try (InputStream is = new FileInputStream(file)) {
            return loadText(is);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static <T> Supplier<T> cache(Supplier<T> supplier) {
        return new Supplier<T>() {
            T value;

            @Override
            public T get() {
                if (value == null) {
                    value = supplier.get();
                }
                return value;
            }
        };
    }

    private void executeComponent(Map<File, Supplier<String>> jsonFiles) throws MojoExecutionException, MojoFailureException {
        // find the component names
        Set<String> componentNames = new TreeSet<>();
        findComponentNames(buildDir, componentNames);

        // create auto configuration for the components
        if (!componentNames.isEmpty()) {
            getLog().debug("Found " + componentNames.size() + " components");

            List<ComponentModel> allModels = new LinkedList<>();
            for (String componentName : componentNames) {
                String json = loadComponentJson(jsonFiles, componentName);
                if (json != null) {
                    ComponentModel model = generateComponentModel(componentName, json);
                    allModels.add(model);
                }
            }

            // Group the models by implementing classes
            Map<String, List<ComponentModel>> grModels = allModels.stream().collect(Collectors.groupingBy(ComponentModel::getJavaType));
            for (String componentClass : grModels.keySet()) {
                List<ComponentModel> compModels = grModels.get(componentClass);
                ComponentModel model = compModels.get(0); // They should be
                                                          // equivalent
                List<String> aliases = compModels.stream().map(ComponentModel::getScheme).sorted().collect(Collectors.toList());

                 String pkg = "org.apache.camel.model.endpoint";

                String overrideComponentName = null;
                if (aliases.size() > 1) {
                    // determine component name when there are multiple ones
                    overrideComponentName = model.getArtifactId().replace("camel-", "");
                }

                createEndpointDsl(pkg, model, overrideComponentName);
            }
        }
    }

    private void createEndpointDsl(String packageName, ComponentModel model, String overrideComponentName) throws MojoFailureException {
        int pos = model.getJavaType().lastIndexOf(".");
        String name = model.getJavaType().substring(pos + 1);
        name = name.replace("Component", "Endpoint");

        Class<?> realComponentClass = loadClass(model.getJavaType());
        Class<?> realEndpointClass = findEndpointClass(realComponentClass);

        final JavaClass javaClass = new JavaClass(getProjectClassLoader());
        javaClass.setPackage(packageName);
        javaClass.setName(name);

        Map<String, JavaClass> enumClasses = new HashMap<>();

        JavaClass commonClass = javaClass.addNestedType().setPublic().setStatic(true);
        commonClass.setName(name.replace("Endpoint", "Common"));
        commonClass.extendSuperType("EndpointConfiguration");
        generateDummyClass(commonClass.getCanonicalName());

        JavaClass consumerClass = javaClass.addNestedType().setPublic().setStatic(true);
        consumerClass.setName(name.replace("Endpoint", "Consumer"));
        consumerClass.extendSuperType(name.replace("Endpoint", "Common"));
        generateDummyClass(consumerClass.getCanonicalName());

        JavaClass producerClass = javaClass.addNestedType().setPublic().setStatic(true);
        producerClass.setName(name.replace("Endpoint", "Producer"));
        producerClass.extendSuperType(name.replace("Endpoint", "Common"));
        generateDummyClass(producerClass.getCanonicalName());

        String doc = "Generated by camel-package-maven-plugin - do not edit this file!";
        if (!Strings.isBlank(model.getDescription())) {
            doc = model.getDescription() + "\n\n" + doc;
        }
        javaClass.getJavaDoc().setText(doc);

        javaClass.addAnnotation(Generated.class.getName())
                .setStringValue("value", EndpointDslMojo.class.getName());

        for (EndpointOptionModel option : model.getEndpointOptions()) {

            JavaClass target = commonClass;
            if (option.getLabel() != null) {
                if (option.getLabel().contains("producer")) {
                    target = producerClass;
                } else if (option.getLabel().contains("consumer")) {
                    target = consumerClass;
                }
            }

            GenericType ogtype;
            GenericType gtype;
            try {
                Field field = findField(realComponentClass, realEndpointClass, option);
                ogtype = new GenericType(GenericsUtil.resolveType(realEndpointClass, field));
                gtype = getType(javaClass, enumClasses, option.getEnums(), ogtype.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


            Property prop = target.addProperty(gtype, option.getName());
            Method fluent = target.addMethod().setPublic().setName(option.getName())
                    .setReturnType(new GenericType(loadClass(target.getCanonicalName())))
                    .addParameter(PRIMITIVEMAP.containsKey(ogtype.toString()) ? ogtype : gtype, option.getName())
                    .setBody("this." + option.getName() + " = " + option.getName() + ";\n" +
                             "return (" + target.getName() + ") this;\n");
            if ("true".equals(option.getDeprecated())) {
                prop.getField().addAnnotation(Deprecated.class);
                prop.getAccessor().addAnnotation(Deprecated.class);
                prop.getMutator().addAnnotation(Deprecated.class);
                fluent.addAnnotation(Deprecated.class);
            }
            if (!Strings.isBlank(option.getDescription())) {
                String desc = option.getDescription();
                if (!desc.endsWith(".")) {
                    desc = desc + ".";
                }
                desc = desc + " The option is a " + option.getJavaType() + " type.";
                fluent.getJavaDoc().setFullText(desc);
            }
        }

        String fileName = packageName.replaceAll("\\.", "\\/") + "/" + name + ".java";
        writeSourceIfChanged(javaClass, fileName, false);
    }

    private Class<?> findEndpointClass(Class<?> componentClass) {
        String endpointName = componentClass.getCanonicalName().replaceFirst("Component", "Endpoint");
        if ("org.apache.camel.component.disruptor.vm.DisruptorVmEndpoint".equals(endpointName)) {
            endpointName = "org.apache.camel.component.disruptor.DisruptorEndpoint";
        } else if ("org.apache.camel.component.etcd.EtcdEndpoint".equals(endpointName)) {
            endpointName = "org.apache.camel.component.etcd.AbstractEtcdPollingEndpoint";
        } else if ("org.apache.camel.websocket.jsr356.JSR356WebSocketEndpoint".equals(endpointName)) {
            endpointName = "org.apache.camel.websocket.jsr356.JSR356Endpoint";
        }
        return loadClass(endpointName);
    }

    private Field findField(Class<?> realComponentClass, Class<?> realEndpointClass, EndpointOptionModel option) throws NoSuchFieldException {
        Field field = null;
        List<Class<?>> classes = new ArrayList<>();
        classes.add(realComponentClass);
        classes.add(realEndpointClass);
        while (!classes.isEmpty()) {
            Class cl = classes.remove(0);
            for (Field f : cl.getDeclaredFields()) {
                String n = f.getName();
                UriPath path = f.getAnnotation(UriPath.class);
                if (path != null && !Strings.isBlank(path.name())) {
                    n = path.name();
                }
                UriParam param = f.getAnnotation(UriParam.class);
                if (param != null && !Strings.isBlank(param.name())) {
                    n = param.name();
                }
                if (n.equals(option.getName())) {
                    field = f;
                    break;
                }
                if (f.getType().isAnnotationPresent(UriParams.class)) {
                    classes.add(f.getType());
                }
            }
            if (field != null) {
                break;
            }
            cl = cl.getSuperclass();
            if (cl != null) {
                classes.add(cl);
            }
        }
        if (field == null) {
            throw new NoSuchFieldException("Could not find field for option " + option.getName());
        }
        return field;
    }

    static boolean isPrimitive(String type) {
        return PRIMITIVEMAP.containsKey(type);
    }

    private Class<?> loadClass(String loadClassName) {
        Class<?> optionClass;
        String org = loadClassName;
        while (true) {
            try {
                optionClass = getProjectClassLoader().loadClass(loadClassName);
                break;
            } catch (ClassNotFoundException e) {
                int dotIndex = loadClassName.lastIndexOf('.');
                if (dotIndex == -1) {
                    throw new IllegalArgumentException(org);
                } else {
                    loadClassName = loadClassName.substring(0, dotIndex) + "$" + loadClassName.substring(dotIndex + 1);
                }
            }
        }
        return optionClass;
    }


    private GenericType getType(JavaClass javaClass, Map<String, JavaClass> enumClasses, String enums, String type) {
        type = type.trim();
        // Check if this is an array
        if (type.endsWith("[]")) {
            GenericType t = getType(javaClass, enumClasses, enums, type.substring(0, type.length() - 2));
            return new GenericType(Array.newInstance(t.getRawClass(), 0).getClass(), t);
        }
        // Check if this is a generic
        int genericIndex = type.indexOf('<');
        if (genericIndex > 0) {
            if (!type.endsWith(">")) {
                throw new IllegalArgumentException("Can not load type: " + type);
            }
            GenericType base = getType(javaClass, enumClasses, enums, type.substring(0, genericIndex));
            if (base.getRawClass() == Object.class) {
                return base;
            }
            String[] params = splitParams(type.substring(genericIndex + 1, type.length() - 1));
            GenericType[] types = new GenericType[params.length];
            for (int i = 0; i < params.length; i++) {
                types[i] = getType(javaClass, enumClasses, enums, params[i]);
            }
            return new GenericType(base.getRawClass(), types);
        }
        // Primitive
        if (isPrimitive(type)) {
            return new GenericType(loadClass(PRIMITIVEMAP.get(type)));
        }
        // Extends
        if (type.startsWith("? extends ")) {
            String raw = type.substring("? extends ".length());
            return new GenericType(loadClass(raw), BoundType.Extends);
        }
        // Super
        if (type.startsWith("? super ")) {
            String raw = type.substring("? extends ".length());
            return new GenericType(loadClass(raw), BoundType.Super);
        }
        // Wildcard
        if (type.equals("?")) {
            return new GenericType(Object.class, BoundType.Extends);
        }
        if (loadClass(type).isEnum() && !Strings.isBlank(enums) && !isCamelCoreType(type)) {
            String enumClassName = type.substring(type.lastIndexOf('.') + 1);
            if (enumClassName.contains("$")) {
                enumClassName = enumClassName.substring(enumClassName.indexOf('$') + 1);
            }
            JavaClass enumClass = enumClasses.get(enumClassName);
            if (enumClass == null) {
                enumClass = javaClass.addNestedType().setPublic().setStatic(true)
                                .setName(enumClassName).setEnum(true);
                enumClasses.put(enumClassName, enumClass);
                for (String value : enums.split(",")) {
                    enumClass.addValue(value
                            .replace('.', '_')
                            .replace('-', '_'));
                }
            }
            type = javaClass.getPackage() + "." + javaClass.getName() + "$" + enumClassName;
            return new GenericType(generateDummyClass(type));
        }
        if (!isCamelCoreType(type)) {
            getLog().debug("Substituting java.lang.Object to " + type);
            return new GenericType(Object.class);
        }
        return new GenericType(loadClass(type));
    }

    private String[] splitParams(String string) {
        List<String> params = new ArrayList<>();
        int cur = 0;
        int start = 0;
        int opened = 0;
        while (true) {
            int nextComma = string.indexOf(',', cur);
            int nextOpen = string.indexOf('<', cur);
            int nextClose = string.indexOf('>', cur);
            if (nextComma < 0) {
                params.add(string.substring(start));
                return params.toArray(new String[0]);
            } else if ((nextOpen < 0 || nextComma < nextOpen)
                    && (nextClose < 0 || nextComma < nextClose)
                    && opened == 0) {
                params.add(string.substring(start, nextComma));
                start = cur = nextComma + 1;
            } else if (nextOpen < 0) {
                if (--opened < 0) {
                    throw new IllegalStateException();
                }
                cur = nextClose + 1;
            } else if (nextClose < 0 || nextOpen < nextClose) {
                ++opened;
                cur = nextOpen + 1;
            } else {
                if (--opened < 0) {
                    throw new IllegalStateException();
                }
                cur = nextClose + 1;
            }
        }
    }

    private boolean isCamelCoreType(String type) {
        return type.startsWith("java.")
                || type.matches("org\\.apache\\.camel\\.(spi\\.)?([A-Za-z]+)");
    }

    protected DynamicClassLoader getProjectClassLoader() {
        if (projectClassLoader == null) {
            final List<?> classpathElements;
            try {
                classpathElements = project.getTestClasspathElements();
            } catch (org.apache.maven.artifact.DependencyResolutionRequiredException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            final URL[] urls = new URL[classpathElements.size()];
            int i = 0;
            for (Iterator<?> it = classpathElements.iterator(); it.hasNext(); i++) {
                try {
                    urls[i] = new File((String)it.next()).toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            projectClassLoader = new DynamicClassLoader(urls, tccl != null ? tccl : getClass().getClassLoader());
        }
        return projectClassLoader;
    }

    static class DynamicClassLoader extends URLClassLoader {
        public DynamicClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        public Class defineClass(String name, byte[] data) {
            return super.defineClass(name, data, 0, data.length);
        }
    }

    private Class generateDummyClass(String clazzName) {
        try {
            return getProjectClassLoader().loadClass(clazzName);
        } catch (ClassNotFoundException e) {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, clazzName.replace('.', '/'), null, "java/lang/Object", null);
            cw.visitEnd();
            return getProjectClassLoader().defineClass(clazzName, cw.toByteArray());
        }
    }

    private static String loadComponentJson(Map<File, Supplier<String>> jsonFiles, String componentName) {
        return loadJsonOfType(jsonFiles, componentName, "component");
    }

    private static String loadJsonOfType(Map<File, Supplier<String>> jsonFiles, String modelName, String type) {
        for (Map.Entry<File, Supplier<String>> entry : jsonFiles.entrySet()) {
            if (entry.getKey().getName().equals(modelName + ".json")) {
                String json = entry.getValue().get();
                if (json.contains("\"kind\": \"" + type + "\"")) {
                    return json;
                }
            }
        }
        return null;
    }

    private static ComponentModel generateComponentModel(String componentName, String json) {
        List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("component", json, false);

        ComponentModel component = new ComponentModel(true);
        component.setScheme(getSafeValue("scheme", rows));
        component.setSyntax(getSafeValue("syntax", rows));
        component.setAlternativeSyntax(getSafeValue("alternativeSyntax", rows));
        component.setTitle(getSafeValue("title", rows));
        component.setDescription(getSafeValue("description", rows));
        component.setFirstVersion(JSonSchemaHelper.getSafeValue("firstVersion", rows));
        component.setLabel(getSafeValue("label", rows));
        component.setDeprecated(getSafeValue("deprecated", rows));
        component.setDeprecationNote(getSafeValue("deprecationNote", rows));
        component.setConsumerOnly(getSafeValue("consumerOnly", rows));
        component.setProducerOnly(getSafeValue("producerOnly", rows));
        component.setJavaType(getSafeValue("javaType", rows));
        component.setGroupId(getSafeValue("groupId", rows));
        component.setArtifactId(getSafeValue("artifactId", rows));
        component.setVersion(getSafeValue("version", rows));

        rows = JSonSchemaHelper.parseJsonSchema("componentProperties", json, true);
        for (Map<String, String> row : rows) {
            ComponentOptionModel option = new ComponentOptionModel();
            option.setName(getSafeValue("name", row));
            option.setDisplayName(getSafeValue("displayName", row));
            option.setKind(getSafeValue("kind", row));
            option.setType(getSafeValue("type", row));
            option.setJavaType(getSafeValue("javaType", row));
            option.setDeprecated(getSafeValue("deprecated", row));
            option.setDeprecationNote(getSafeValue("deprecationNote", row));
            option.setDescription(getSafeValue("description", row));
            option.setDefaultValue(getSafeValue("defaultValue", row));
            option.setEnums(getSafeValue("enum", row));
            component.addComponentOption(option);
        }

        rows = JSonSchemaHelper.parseJsonSchema("properties", json, true);
        for (Map<String, String> row : rows) {
            EndpointOptionModel option = new EndpointOptionModel();
            option.setName(getSafeValue("name", row));
            option.setDisplayName(getSafeValue("displayName", row));
            option.setKind(getSafeValue("kind", row));
            option.setGroup(getSafeValue("group", row));
            option.setLabel(getSafeValue("label", row));
            option.setRequired(getSafeValue("required", row));
            option.setType(getSafeValue("type", row));
            option.setJavaType(getSafeValue("javaType", row));
            option.setEnums(getSafeValue("enum", row));
            option.setPrefix(getSafeValue("prefix", row));
            option.setMultiValue(getSafeValue("multiValue", row));
            option.setDeprecated(getSafeValue("deprecated", row));
            option.setDeprecationNote(getSafeValue("deprecationNote", row));
            option.setDefaultValue(getSafeValue("defaultValue", row));
            option.setDescription(getSafeValue("description", row));
            option.setEnumValues(getSafeValue("enum", row));
            component.addEndpointOption(option);
        }

        return component;
    }

    private void findComponentNames(File dir, Set<String> componentNames) {
        File f = new File(dir, "classes/META-INF/services/org/apache/camel/component");

        if (f.exists() && f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File file : files) {
                    // skip directories as there may be a sub .resolver
                    // directory
                    if (file.isDirectory()) {
                        continue;
                    }
                    String name = file.getName();
                    if (name.charAt(0) != '.') {
                        componentNames.add(name);
                    }
                }
            }
        }
    }

    private void writeSourceIfChanged(JavaClass source, String fileName, boolean innerClassesLast) throws MojoFailureException {
        writeSourceIfChanged(source.printClass(innerClassesLast), fileName);
    }

    private void writeSourceIfChanged(String source, String fileName) throws MojoFailureException {
        File core = findCamelCoreDirectory(project.getBasedir());

        File target = new File(new File(core, "src/main/java"), fileName);

        try {
            String header;
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("license-header-java.txt")) {
                header = loadText(is);
            }
            String code = header + source;
            getLog().debug("Source code generated:\n" + code);

            updateResource(null, target.toPath(), code);
        } catch (Exception e) {
            throw new MojoFailureException("IOError with file " + target, e);
        }
    }

}
