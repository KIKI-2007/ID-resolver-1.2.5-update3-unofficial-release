package sharose.mods.idresolver;

import java.util.Map;

public class IDResolverCorePlugin {

    public IDResolverCorePlugin() {
    }

    public void onPreLoad(String profile) {
        IDResolver.scan();
    }

    public void injectData(Map<String, Object> data) {
        IDResolver.scan();
    }
}