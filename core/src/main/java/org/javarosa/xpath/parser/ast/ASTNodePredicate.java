/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.xpath.parser.ast;

import java.util.Vector;

import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class ASTNodePredicate extends ASTNode {
    public ASTNode expr;

    public Vector getChildren() {
        Vector v = new Vector();
        v.addElement(expr);
        return v;
    }

    public XPathExpression build() throws XPathSyntaxException {
        return expr.build();
    }
}
