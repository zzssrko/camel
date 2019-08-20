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
package org.apache.camel.support.component;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.EndpointPropertyConfigurer;

public abstract class EndpointPropertyConfigurerSupport implements EndpointPropertyConfigurer {

    public static <T> T property(CamelContext camelContext, Class<T> type, Object value) {
        // if the type is not string based and the value is a bean reference, then we need to lookup
        // the bean from the registry
        if (value instanceof String && String.class != type) {
            // is it a reference parameter
            String text = value.toString();
            if (text.startsWith("#")) {
                String ref = text.startsWith("#bean:") ? text.substring(6) : text.substring(1);
                Object obj = camelContext.getRegistry().lookupByName(ref);
                if (obj != null) {
                    value = obj;
                }
            }
        }
        return camelContext.getTypeConverter().convertTo(type, value);
    }

}
