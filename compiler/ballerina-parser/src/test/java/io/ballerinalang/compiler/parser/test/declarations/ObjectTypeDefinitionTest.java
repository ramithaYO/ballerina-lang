/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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
package io.ballerinalang.compiler.parser.test.declarations;

import org.testng.annotations.Test;

/**
 * Test parsing object type definitions.
 */
public class ObjectTypeDefinitionTest extends AbstractDeclarationTest {

    // Valid syntax tests

    // TODO: abstract object | client object test

    @Test
    public void testComplexObjectTypeDef() {
        test("object-type-def/object_type_def_source_01.bal", "object-type-def/object_type_def_assert_01.json");
    }

    @Test
    public void testEmptyObjectTypeDef() {
        test("object-type-def/object_type_def_source_02.bal", "object-type-def/object_type_def_assert_02.json");
    }

    @Test
    public void testObjectTypeDefWithFieldsOnly() {
        test("object-type-def/object_type_def_source_03.bal", "object-type-def/object_type_def_assert_03.json");
    }

    @Test
    public void testObjectTypeDefWithMethodsOnly() {
        test("object-type-def/object_type_def_source_04.bal", "object-type-def/object_type_def_assert_04.json");
    }

    @Test
    public void testAnonymousObjectTypeInVarDef() {
        test("object-type-def/object_type_def_source_11.bal", "object-type-def/object_type_def_assert_11.json");
    }

    @Test
    public void testAnonymousObjectTypeInFuncParam() {
        test("object-type-def/object_type_def_source_12.bal", "object-type-def/object_type_def_assert_12.json");
    }

    // Recovery tests

    @Test
    public void testMissingCloseBrace() {
        test("object-type-def/object_type_def_source_05.bal", "object-type-def/object_type_def_assert_05.json");
    }

    @Test
    public void testMissingOpenBrace() {
        test("object-type-def/object_type_def_source_06.bal", "object-type-def/object_type_def_assert_06.json");
    }

    @Test
    public void testMissingObjectKeyword() {
        test("object-type-def/object_type_def_source_07.bal", "object-type-def/object_type_def_assert_07.json");
    }

    @Test
    public void testObjectMembersWithExtraTokens() {
        test("object-type-def/object_type_def_source_08.bal", "object-type-def/object_type_def_assert_08.json");
    }

    @Test
    public void testObjectFieldWithMissingEqual() {
        test("object-type-def/object_type_def_source_09.bal", "object-type-def/object_type_def_assert_09.json");
    }

    @Test
    public void testNestedObjectRecovery() {
        test("object-type-def/object_type_def_source_10.bal", "object-type-def/object_type_def_assert_10.json");
    }

    @Test
    public void testMissingTypeReference() {
        test("object-type-def/object_type_def_source_13.bal", "object-type-def/object_type_def_assert_13.json");
    }
}
