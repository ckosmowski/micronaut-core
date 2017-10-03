/*
 * Copyright 2017 original authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.particleframework.core.type;

import java.util.Map;
import java.util.Optional;

/**
 * An interface for types that hold and can resolve type variables
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface TypeVariableResolver {
    /**
     * @return Obtain a map of the type parameters for the argument
     */
    Map<String,Argument<?>> getTypeVariables();

    /**
     * @return Return the first type parameter if it is present
     */
    default Optional<Argument<?>> getFirstTypeVariable() {
        return getTypeVariables().values().stream().findFirst();
    }

    /**
     * @return Return the first type parameter if it is present
     */
    default Optional<Argument<?>> getTypeVariable(String name) {
        Argument<?> argument = getTypeVariables().get(name);
        return argument != null ? Optional.of(argument) : Optional.empty();
    }
}
