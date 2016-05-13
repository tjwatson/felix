/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.resolver.util;

import java.util.IdentityHashMap;
import java.util.Map;

import org.osgi.resource.Requirement;

public class OpenHashMapList extends OpenHashMap<Requirement, CandidateSelector> {

    public OpenHashMapList() {
        super();
    }

    public OpenHashMapList(int initialCapacity) {
        super(initialCapacity);
    }

    public OpenHashMapList deepClone(boolean shareSelectors) {
        Map<CandidateSelector, CandidateSelector> sharedSelectors = new IdentityHashMap<CandidateSelector, CandidateSelector>();
        OpenHashMapList copy = (OpenHashMapList) super.clone();
        Object[] values = copy.value;
        for (int i = values.length; i-- > 0;) {
            if (values[i] != null) {
                if (shareSelectors)
                {
                    if (sharedSelectors.containsKey(values[i]))
                    {
                        values[i] = sharedSelectors.get(values[i]);
                    }
                    else
                    {
                        CandidateSelector selectorCopy = ((CandidateSelector) values[i]).copy();
                        sharedSelectors.put((CandidateSelector) values[i], selectorCopy);
                        values[i] = selectorCopy;
                    }
                }
                else
                {
                    values[i] = ((CandidateSelector) values[i]).copy();
                }
            }
        }
        return copy;
    }

}
