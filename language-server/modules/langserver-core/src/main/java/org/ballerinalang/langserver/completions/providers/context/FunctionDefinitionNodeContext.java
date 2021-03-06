/*
 * Copyright (c) 2020, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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
package org.ballerinalang.langserver.completions.providers.context;

import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.TextRange;
import io.ballerinalang.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerinalang.compiler.syntax.tree.FunctionSignatureNode;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.LSContext;
import org.ballerinalang.langserver.commons.completion.CompletionKeys;
import org.ballerinalang.langserver.commons.completion.LSCompletionException;
import org.ballerinalang.langserver.commons.completion.LSCompletionItem;
import org.ballerinalang.langserver.compiler.DocumentServiceKeys;
import org.ballerinalang.langserver.completions.SnippetCompletionItem;
import org.ballerinalang.langserver.completions.providers.AbstractCompletionProvider;
import org.ballerinalang.langserver.completions.util.CompletionUtil;
import org.ballerinalang.langserver.completions.util.Snippet;
import org.eclipse.lsp4j.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Completion provider for {@link FunctionDefinitionNode} context.
 *
 * @since 2.0.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.completion.spi.CompletionProvider")
public class FunctionDefinitionNodeContext extends AbstractCompletionProvider<FunctionDefinitionNode> {
    public FunctionDefinitionNodeContext() {
        super(FunctionDefinitionNode.class);
    }

    @Override
    public List<LSCompletionItem> getCompletions(LSContext context, FunctionDefinitionNode node)
            throws LSCompletionException {
        List<LSCompletionItem> completionItems = new ArrayList<>();

        if (canCheckWithinFunctionSignature(context, node)) {
            return CompletionUtil.route(context, node.functionSignature());
        }
        if (node.functionKeyword().isMissing()) {
            /*
            This is to cover the following within the service body node
            (1) resource <cursor>
            Suggest the function keyword and the function snippet
             */
            completionItems.add(new SnippetCompletionItem(context, Snippet.KW_FUNCTION.get()));
            completionItems.add(new SnippetCompletionItem(context, Snippet.DEF_FUNCTION.get()));
        }
        return completionItems;
    }

    private boolean canCheckWithinFunctionSignature(LSContext context, FunctionDefinitionNode node) {
        FunctionSignatureNode functionSignatureNode = node.functionSignature();
        if (functionSignatureNode.isMissing()) {
            return false;
        }
        LinePosition signatureEndLine = functionSignatureNode.lineRange().endLine();
        Position cursor = context.get(DocumentServiceKeys.POSITION_KEY).getPosition();

        return (signatureEndLine.line() == cursor.getLine() && signatureEndLine.offset() < cursor.getCharacter())
                || signatureEndLine.line() < cursor.getLine();
    }

    @Override
    public boolean onPreValidation(LSContext context, FunctionDefinitionNode node) {
        Integer textPosition = context.get(CompletionKeys.TEXT_POSITION_IN_TREE);
        TextRange functionKW = node.functionKeyword().textRange();
        return functionKW.endOffset() <= textPosition;
    }
}
