import org.bitcoinj.core.Address;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;

import java.io.File;

public class P2P {

    public static void main (String[] args) {

        NetworkParameters params;
        params = RegTestParams.get();

        String filePrefix;
        filePrefix = "forwarding-service-regtest";

        Address forwardingAddress = new Address(params, "n2iwo2wQndBYrf7CGja6a4pP6sjF4yF6qx");

        WalletAppKit kit = new WalletAppKit(params, new File("."), filePrefix) {
            @Override
            protected void onSetupCompleted() {
                // This is called in a background thread after startAndWait is called, as setting up various objects
                // can do disk and network IO that may cause UI jank/stuttering in wallet apps if it were to be done
                // on the main thread.
                if (wallet().getKeyChainGroupSize() < 1)
                    wallet().importKey(new ECKey());
            }
        };

        if (params == RegTestParams.get()) {
            // Regression test mode is designed for testing and development only, so there's no public network for it.
            // If you pick this mode, you're expected to be running a local "bitcoind -regtest" instance.
            kit.connectToLocalHost();
        }

// Download the block chain and wait until it's done.
        kit.startAsync();
        kit.awaitRunning();

    }
}
