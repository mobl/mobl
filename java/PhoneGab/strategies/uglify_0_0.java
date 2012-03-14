package PhoneGab.strategies;
import java.io.FileReader;

import org.mozilla.javascript.Scriptable;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;


import PhoneGab.strategies.uglify_0_0;

/**
 * Example Java strategy implementation.
 *
 * This strategy can be used by editor services and can be called in Stratego
 * modules by declaring it as an external strategy as follows:
 *
 * <code>
        *  external java-strategy(|)
        * </code>
 *
 * @see InteropRegisterer This class registers java_strategy_0_0 for use.
 */
public class uglify_0_0 extends Strategy {

    public static uglify_0_0 instance = new uglify_0_0();

    @Override
    public IStrategoTerm invoke(Context context, IStrategoTerm current) {
        String stdlibPath = getStringFromTerm(current.getSubterm(0));
        String code = getStringFromTerm(current.getSubterm(1));
        ITermFactory factory = context.getFactory();
        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
        //cx.setOptimizationLevel(9);
        Scriptable scope = cx.initStandardObjects();
        try {
            scope.put("code", scope, code);
            FileReader reader = new FileReader(stdlibPath + "/js-minify/parse-js.js");
            cx.evaluateReader(scope, reader, "parse-js.js", 1, null);
            reader = new FileReader(stdlibPath + "/js-minify/process.js");
            cx.evaluateReader(scope, reader, "process.js", 1, null);
            cx.evaluateString(scope, "var ast = jsp.parse(code);", "<cmd>", 1, null);
            cx.evaluateString(scope, "ast = exports.ast_mangle(ast);", "<cmd>", 1, null);
            cx.evaluateString(scope, "ast = exports.ast_squeeze(ast);", "<cmd>", 1, null);
            cx.evaluateString(scope, "var final_code = exports.gen_code(ast);", "<cmd>", 1, null);
            return factory.makeString(scope.get("final_code", scope).toString());
        } catch(Exception e) {
            context.getIOAgent().printError(e.toString());
            e.printStackTrace();
            return null;
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    public static String getStringFromTerm(IStrategoTerm current) {
        Class<?> cls = current.getClass();
        try {
            return (String) cls.getMethod("stringValue").invoke(current);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}