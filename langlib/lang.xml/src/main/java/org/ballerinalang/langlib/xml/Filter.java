/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.langlib.xml;

import org.ballerinalang.jvm.BRuntime;
import org.ballerinalang.jvm.scheduling.Strand;
import org.ballerinalang.jvm.values.FPValue;
import org.ballerinalang.jvm.values.XMLSequence;
import org.ballerinalang.jvm.values.XMLValue;
import org.ballerinalang.jvm.values.api.BXML;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Native implementation of lang.xml:filter(map&lt;Type&gt;, function).
 *
 * @since 1.0
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "lang.xml", functionName = "filter",
        args = {
                @Argument(name = "x", type = TypeKind.XML),
                @Argument(name = "func", type = TypeKind.FUNCTION)},
        returnType = {@ReturnType(type = TypeKind.XML)},
        isPublic = true
)
public class Filter {

    public static XMLValue filter(Strand strand, XMLValue x, FPValue<Object, Boolean> func) {
        if (x.isSingleton()) {
            AtomicReference<XMLValue> xmlValue = new AtomicReference<>(new XMLSequence());
            Object[] args = new Object[]{strand, x, true};
            BRuntime.getCurrentRuntime().invokeFunctionPointerAsync(func, strand, args, future -> {
                if((Boolean)future.result){
                    xmlValue.set(x);
                }
            }, () -> true, () -> xmlValue);
            return new XMLSequence();
        }

        List<BXML> elements = new ArrayList<>();
        int size = x.size();
        BRuntime.getCurrentRuntime()
                .invokeFunctionPointerAsyncForCollection(func, strand, size,
                                                     index -> new Object[]{strand, x.getItem(index), true},
                                                         (index, future) -> {
                                         if ((Boolean) future.result) {
                                             elements.add(x.getItem(index));
                                         }
                                     }, () -> new XMLSequence(elements));
        return new XMLSequence(elements);
    }
}
