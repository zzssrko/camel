/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.spi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.TypeConverter;

public interface BeanIntrospection {

    boolean isGetter(Method method);

    String getGetterShorthandName(Method method);

    String getSetterShorthandName(Method method);

    boolean isSetter(Method method, boolean allowBuilderPattern);

    boolean isSetter(Method method);

    /**
     * Will inspect the target for properties.
     * <p/>
     * Notice a property must have both a getter/setter method to be included.
     * Notice all <tt>null</tt> values won't be included.
     *
     * @param target         the target bean
     * @return the map with found properties
     */
    Map<String, Object> getNonNullProperties(Object target);

    /**
     * Will inspect the target for properties.
     * <p/>
     * Notice a property must have both a getter/setter method to be included.
     * Notice all <tt>null</tt> values will be included.
     *
     * @param target         the target bean
     * @param properties     the map to fill in found properties
     * @param optionPrefix   an optional prefix to append the property key
     * @return <tt>true</tt> if any properties was found, <tt>false</tt> otherwise.
     */
    boolean getProperties(Object target, Map<String, Object> properties, String optionPrefix);

    /**
     * Will inspect the target for properties.
     * <p/>
     * Notice a property must have both a getter/setter method to be included.
     *
     * @param target         the target bean
     * @param properties     the map to fill in found properties
     * @param optionPrefix   an optional prefix to append the property key
     * @param includeNull    whether to include <tt>null</tt> values
     * @return <tt>true</tt> if any properties was found, <tt>false</tt> otherwise.
     */
    boolean getProperties(Object target, Map<String, Object> properties, String optionPrefix, boolean includeNull);

    /**
     * Introspects the given class.
     *
     * @param clazz the class
     * @return the introspection result as a {@link ClassInfo} structure.
     */
    // TODO:
    //ClassInfo cacheClass(Class<?> clazz);

    boolean hasProperties(Map<String, Object> properties, String optionPrefix);

    Object getProperty(Object target, String propertyName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException;

    Object getOrElseProperty(Object target, String propertyName, Object defaultValue);

    Object getOrElseProperty(Object target, String propertyName, Object defaultValue, boolean ignoreCase);

    Method getPropertyGetter(Class<?> type, String propertyName) throws NoSuchMethodException;

    Method getPropertyGetter(Class<?> type, String propertyName, boolean ignoreCase) throws NoSuchMethodException;

    Method getPropertySetter(Class<?> type, String propertyName) throws NoSuchMethodException;

    boolean isPropertyIsGetter(Class<?> type, String propertyName);

    /**
     * @deprecated use {@link PropertyBindingSupport}
     */
    @Deprecated
    boolean setProperties(Object target, Map<String, Object> properties, String optionPrefix, boolean allowBuilderPattern) throws Exception;

    /**
     * @deprecated use {@link PropertyBindingSupport}
     */
    @Deprecated
    boolean setProperties(Object target, Map<String, Object> properties, String optionPrefix) throws Exception;

    Map<String, Object> extractProperties(Map<String, Object> properties, String optionPrefix);

    Map<String, Object> extractProperties(Map<String, Object> properties, String optionPrefix, boolean remove);

    @Deprecated
    Map<String, String> extractStringProperties(Map<String, Object> properties);

    /**
     * @deprecated use {@link PropertyBindingSupport}
     */
    @Deprecated
    boolean setProperties(CamelContext context, TypeConverter typeConverter, Object target, Map<String, Object> properties) throws Exception;

    /**
     * @deprecated use {@link PropertyBindingSupport}
     */
    @Deprecated
    boolean setProperties(TypeConverter typeConverter, Object target, Map<String, Object> properties) throws Exception;

    /**
     * @deprecated use {@link PropertyBindingSupport}
     */
    @Deprecated
    boolean setProperties(Object target, Map<String, Object> properties) throws Exception;

    /**
     * This method supports three modes to set a property:
     *
     * 1. Setting a Map property where the property name refers to a map via name[aKey] where aKey is the map key to use.
     *
     * 2. Setting a property that has already been resolved, this is the case when {@code context} and {@code refName} are
     * NULL and {@code value} is non-NULL.
     *
     * 3. Setting a property that has not yet been resolved, the property will be resolved based on the suitable methods
     * found matching the property name on the {@code target} bean. For this mode to be triggered the parameters
     * {@code context} and {@code refName} must NOT be NULL, and {@code value} MUST be NULL.
     */
    boolean setProperty(CamelContext context, TypeConverter typeConverter, Object target, String name, Object value, String refName,
                                      boolean allowBuilderPattern) throws Exception;

    /**
     * This method supports three modes to set a property:
     *
     * 1. Setting a Map property where the property name refers to a map via name[aKey] where aKey is the map key to use.
     *
     * 2. Setting a property that has already been resolved, this is the case when {@code context} and {@code refName} are
     * NULL and {@code value} is non-NULL.
     *
     * 3. Setting a property that has not yet been resolved, the property will be resolved based on the suitable methods
     * found matching the property name on the {@code target} bean. For this mode to be triggered the parameters
     * {@code context} and {@code refName} must NOT be NULL, and {@code value} MUST be NULL.
     */
    boolean setProperty(CamelContext context, TypeConverter typeConverter, Object target, String name, Object value, String refName,
                                      boolean allowBuilderPattern, boolean allowPrivateSetter, boolean ignoreCase) throws Exception;

    boolean isPropertyPlaceholder(CamelContext context, Object value);

    boolean setProperty(CamelContext context, Object target, String name, Object value) throws Exception;

    boolean setProperty(CamelContext context, TypeConverter typeConverter, Object target, String name, Object value) throws Exception;

    boolean setProperty(TypeConverter typeConverter, Object target, String name, Object value) throws Exception;

    @Deprecated
    boolean setProperty(Object target, String name, Object value, boolean allowBuilderPattern) throws Exception;

    @Deprecated
    boolean setProperty(Object target, String name, Object value) throws Exception;

}
