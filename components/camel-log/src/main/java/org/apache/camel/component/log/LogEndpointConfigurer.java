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
package org.apache.camel.component.log;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.PropertyConfigurer;

public class LogEndpointConfigurer implements PropertyConfigurer<Object> {

    private final Map<String, Supplier<Object>> readPlaceholders = new HashMap<>();
    private final Map<String, Consumer<Object>> writePlaceholders = new HashMap<>();

    public LogEndpointConfigurer(final Object target, final CamelContext camelContext) {
        final LogEndpoint endpoint = (LogEndpoint) target;

        readPlaceholders.put("loggerName", endpoint::getLoggerName);
        writePlaceholders.put("loggerName", o -> endpoint.setLoggerName(camelContext.getTypeConverter().convertTo(String.class, o)));
        readPlaceholders.put("groupSize", endpoint::getGroupSize);
        writePlaceholders.put("groupSize", o -> endpoint.setGroupSize(camelContext.getTypeConverter().convertTo(Integer.class, o)));
    }

    @Override
    public Map<String, Supplier<Object>> getReadPropertyPlaceholderOptions(CamelContext camelContext) {
        return readPlaceholders;
    }

    @Override
    public Map<String, Consumer<Object>> getWritePropertyPlaceholderOptions(CamelContext camelContext) {
        return writePlaceholders;
    }
}
