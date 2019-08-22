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
package org.apache.camel.impl.engine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.camel.CamelContext;
import org.apache.camel.TypeConverter;
import org.apache.camel.spi.BeanIntrospection;
import org.apache.camel.support.IntrospectionSupport;
import org.apache.camel.support.service.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public class DefaultBeanIntrospection extends ServiceSupport implements BeanIntrospection {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultBeanIntrospection.class);

    private final AtomicLong invoked = new AtomicLong();

    @Override
    public long getInvokedCounter() {
        return invoked.get();
    }

    @Override
    public ClassInfo cacheClass(Class<?> clazz) {
        invoked.incrementAndGet();
        return IntrospectionSupport.cacheClass(clazz);
    }

    @Override
    public boolean isGetter(Method method) {
        return IntrospectionSupport.isGetter(method);
    }

    @Override
    public String getGetterShorthandName(Method method) {
        return IntrospectionSupport.getGetterShorthandName(method);
    }

    @Override
    public String getSetterShorthandName(Method method) {
        return IntrospectionSupport.getSetterShorthandName(method);
    }

    @Override
    public boolean isSetter(Method method, boolean allowBuilderPattern) {
        return IntrospectionSupport.isSetter(method, allowBuilderPattern);
    }

    @Override
    public boolean isSetter(Method method) {
        return IntrospectionSupport.isSetter(method);
    }

    @Override
    public Map<String, Object> getNonNullProperties(Object target) {
        return IntrospectionSupport.getNonNullProperties(target);
    }

    @Override
    public boolean getProperties(Object target, Map<String, Object> properties, String optionPrefix) {
        invoked.incrementAndGet();
        return IntrospectionSupport.getProperties(target, properties, optionPrefix);
    }

    @Override
    public boolean getProperties(Object target, Map<String, Object> properties, String optionPrefix, boolean includeNull) {
        invoked.incrementAndGet();
        return IntrospectionSupport.getProperties(target, properties, optionPrefix, includeNull);
    }

    @Override
    public boolean hasProperties(Map<String, Object> properties, String optionPrefix) {
        return IntrospectionSupport.hasProperties(properties, optionPrefix);
    }

    @Override
    public Object getProperty(Object target, String propertyName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        invoked.incrementAndGet();
        return IntrospectionSupport.getProperty(target, propertyName);
    }

    @Override
    public Object getOrElseProperty(Object target, String propertyName, Object defaultValue) {
        invoked.incrementAndGet();
        return IntrospectionSupport.getOrElseProperty(target, propertyName, defaultValue);
    }

    @Override
    public Object getOrElseProperty(Object target, String propertyName, Object defaultValue, boolean ignoreCase) {
        invoked.incrementAndGet();
        return IntrospectionSupport.getOrElseProperty(target, propertyName, defaultValue, ignoreCase);
    }

    @Override
    public Method getPropertyGetter(Class<?> type, String propertyName) throws NoSuchMethodException {
        invoked.incrementAndGet();
        return IntrospectionSupport.getPropertyGetter(type, propertyName);
    }

    @Override
    public Method getPropertyGetter(Class<?> type, String propertyName, boolean ignoreCase) throws NoSuchMethodException {
        invoked.incrementAndGet();
        return IntrospectionSupport.getPropertyGetter(type, propertyName, ignoreCase);
    }

    @Override
    public Method getPropertySetter(Class<?> type, String propertyName) throws NoSuchMethodException {
        invoked.incrementAndGet();
        return IntrospectionSupport.getPropertySetter(type, propertyName);
    }

    @Override
    public boolean isPropertyIsGetter(Class<?> type, String propertyName) {
        invoked.incrementAndGet();
        return IntrospectionSupport.isPropertyIsGetter(type, propertyName);
    }

    @Override
    @Deprecated
    public boolean setProperties(Object target, Map<String, Object> properties, String optionPrefix, boolean allowBuilderPattern) throws Exception {
        invoked.incrementAndGet();
        return IntrospectionSupport.setProperties(target, properties, optionPrefix, allowBuilderPattern);
    }

    @Override
    @Deprecated
    public boolean setProperties(Object target, Map<String, Object> properties, String optionPrefix) throws Exception {
        invoked.incrementAndGet();
        return IntrospectionSupport.setProperties(target, properties, optionPrefix);
    }

    @Override
    public Map<String, Object> extractProperties(Map<String, Object> properties, String optionPrefix) {
        return IntrospectionSupport.extractProperties(properties, optionPrefix);
    }

    @Override
    public Map<String, Object> extractProperties(Map<String, Object> properties, String optionPrefix, boolean remove) {
        return IntrospectionSupport.extractProperties(properties, optionPrefix, remove);
    }

    @Override
    @Deprecated
    public Map<String, String> extractStringProperties(Map<String, Object> properties) {
        return IntrospectionSupport.extractStringProperties(properties);
    }

    @Override
    @Deprecated
    public boolean setProperties(CamelContext context, TypeConverter typeConverter, Object target, Map<String, Object> properties) throws Exception {
        invoked.incrementAndGet();
        return IntrospectionSupport.setProperties(context, typeConverter, target, properties);
    }

    @Override
    @Deprecated
    public boolean setProperties(TypeConverter typeConverter, Object target, Map<String, Object> properties) throws Exception {
        invoked.incrementAndGet();
        return IntrospectionSupport.setProperties(typeConverter, target, properties);
    }

    @Override
    @Deprecated
    public boolean setProperties(Object target, Map<String, Object> properties) throws Exception {
        invoked.incrementAndGet();
        return IntrospectionSupport.setProperties(target, properties);
    }

    @Override
    public boolean setProperty(CamelContext context, TypeConverter typeConverter, Object target, String name, Object value, String refName, boolean allowBuilderPattern) throws Exception {
        invoked.incrementAndGet();
        return IntrospectionSupport.setProperty(context, typeConverter, target, name, value, refName, allowBuilderPattern);
    }

    @Override
    public boolean setProperty(CamelContext context, TypeConverter typeConverter, Object target, String name, Object value, String refName, boolean allowBuilderPattern, boolean allowPrivateSetter, boolean ignoreCase) throws Exception {
        invoked.incrementAndGet();
        return IntrospectionSupport.setProperty(context, typeConverter, target, name, value, refName, allowBuilderPattern, allowPrivateSetter, ignoreCase);
    }

    @Override
    public boolean setProperty(CamelContext context, Object target, String name, Object value) throws Exception {
        invoked.incrementAndGet();
        return IntrospectionSupport.setProperty(context, target, name, value);
    }

    @Override
    public boolean setProperty(CamelContext context, TypeConverter typeConverter, Object target, String name, Object value) throws Exception {
        invoked.incrementAndGet();
        return IntrospectionSupport.setProperty(context, typeConverter, target, name, value);
    }

    @Override
    public boolean setProperty(TypeConverter typeConverter, Object target, String name, Object value) throws Exception {
        invoked.incrementAndGet();
        return IntrospectionSupport.setProperty(typeConverter, target, name, value);
    }

    @Override
    @Deprecated
    public boolean setProperty(Object target, String name, Object value, boolean allowBuilderPattern) throws Exception {
        invoked.incrementAndGet();
        return IntrospectionSupport.setProperty(target, name, value, allowBuilderPattern);
    }

    @Override
    @Deprecated
    public boolean setProperty(Object target, String name, Object value) throws Exception {
        invoked.incrementAndGet();
        return IntrospectionSupport.setProperty(target, name, value);
    }

    @Override
    public Set<Method> findSetterMethods(Class<?> clazz, String name, boolean allowBuilderPattern, boolean allowPrivateSetter, boolean ignoreCase) {
        invoked.incrementAndGet();
        return IntrospectionSupport.findSetterMethods(clazz, name, allowBuilderPattern, allowPrivateSetter, ignoreCase);
    }

    @Override
    protected void doStart() throws Exception {
        // noop
    }

    @Override
    protected void doStop() throws Exception {
        IntrospectionSupport.stop();
        LOG.debug("BeanIntrospection invoked: {} times", getInvokedCounter());
    }
}
