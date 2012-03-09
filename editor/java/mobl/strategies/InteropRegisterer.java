package mobl.strategies;

import org.strategoxt.lang.JavaInteropRegisterer;
import org.strategoxt.lang.Strategy;

/**
 * Helper class for {@link java_strategy_0_0}.
 */
public class InteropRegisterer extends JavaInteropRegisterer {

    public InteropRegisterer() {
        super(new Strategy[] { deltree_0_0.instance, uglify_0_0.instance, compress_0_0.instance,
                path_to_dataurl_0_0.instance,buildOnCloud_0_0.instance });
    }
}
