package teavm;

import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

public class GetCanonicalNamePlugin implements TeaVMPlugin {
    public GetCanonicalNamePlugin() {
        System.out.println("ctor");
    }

    @Override
    public void install(TeaVMHost host) {
        System.out.println("plugin starting");
        host.add(new ClassGetCanonicalNamePatch());
    }
}
