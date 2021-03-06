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

package org.ballerinalang.jvm.values.utils;

import org.ballerinalang.jvm.CycleUtils;
import org.ballerinalang.jvm.TypeChecker;
import org.ballerinalang.jvm.scheduling.Scheduler;
import org.ballerinalang.jvm.types.AttachedFunction;
import org.ballerinalang.jvm.types.BObjectType;
import org.ballerinalang.jvm.types.BType;
import org.ballerinalang.jvm.types.TypeTags;
import org.ballerinalang.jvm.values.AbstractObjectValue;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.MapValueImpl;
import org.ballerinalang.jvm.values.RefValue;
import org.ballerinalang.jvm.values.api.BLink;
import org.ballerinalang.jvm.values.api.BString;

/**
 * This class contains the methods that deals with Strings.
 *
 * @since 1.0
 */
public class StringUtils {

    private static final String STR_CYCLE = "...";
    /**
     * Returns the human-readable string value of Ballerina values.
     * 
     * @param value The value on which the function is invoked
     * @param parent The link to the parent node
     * @return String value of the value
     */
    public static String getStringValue(Object value, BLink parent) {
        if (value == null) {
            return "";
        }

        BType type = TypeChecker.getType(value);

        //TODO: bstring - change to type tag check
        if (value instanceof BString) {
            return ((BString) value).getValue();
        }

        if (type.getTag() < TypeTags.JSON_TAG) {
            return String.valueOf(value);
        }

        CycleUtils.Node node = new CycleUtils.Node(value, parent);

        if (node.hasCyclesSoFar()) {
            return STR_CYCLE;
        }

        if (type.getTag() == TypeTags.MAP_TAG || type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            MapValueImpl mapValue = (MapValueImpl) value;
            return mapValue.stringValue(parent);
        }

        if (type.getTag() == TypeTags.ARRAY_TAG || type.getTag() == TypeTags.TUPLE_TAG) {
            ArrayValue arrayValue = (ArrayValue) value;
            return arrayValue.stringValue(parent);
        }

        if (type.getTag() == TypeTags.TABLE_TAG) {
            return ((RefValue) value).informalStringValue(parent);
        }

        if (type.getTag() == TypeTags.OBJECT_TYPE_TAG) {
            AbstractObjectValue objectValue = (AbstractObjectValue) value;
            BObjectType objectType = objectValue.getType();
            for (AttachedFunction func : objectType.getAttachedFunctions()) {
                if (func.funcName.equals("toString") && func.paramTypes.length == 0 &&
                    func.type.retType.getTag() == TypeTags.STRING_TAG) {
                    return objectValue.call(Scheduler.getStrand(), "toString").toString();
                }
            }
        }

        if (type.getTag() == TypeTags.ERROR_TAG) {
            RefValue errorValue = (RefValue) value;
            return errorValue.stringValue(parent);
        }

        RefValue refValue = (RefValue) value;
        return refValue.stringValue(parent);
    }

    /**
     * Returns the json string value of Ballerina values.
     *
     * @param value The value on which the function is invoked
     * @return Json String value of the value
     */
    public static String getJsonString(Object value) {
        if (value == null) {
            return "null";
        }

        BType type = TypeChecker.getType(value);

        if (type.getTag() < TypeTags.JSON_TAG) {
            return String.valueOf(value);
        }

        if (type.getTag() == TypeTags.MAP_TAG) {
            MapValueImpl mapValue = (MapValueImpl) value;
            return mapValue.getJSONString();
        }

        if (type.getTag() == TypeTags.ARRAY_TAG) {
            ArrayValue arrayValue = (ArrayValue) value;
            return arrayValue.getJSONString();
        }

        RefValue refValue = (RefValue) value;
        return refValue.stringValue(null);
    }

    private StringUtils() {
    }
}
