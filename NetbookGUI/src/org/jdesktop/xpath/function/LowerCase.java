/*
 * EndsWith.java
 *
 * Created on September 27, 2006, 10:09 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.xpath.function;

import java.util.List;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

/**
 *
 * @author richardallenbair
 */
public class LowerCase extends AbstractFunction {
    
    /** Creates a new instance of EndsWith */
    public LowerCase() {
        super("lower-case", 1);
    }

    public Object evaluate(List args) throws XPathFunctionException {
        return getStringParam(args.get(0)).toLowerCase();
    }
}
