package trading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;

class ChainConnector {
    private Web3j web3j;
    private Credentials credentials;
    private static final Logger log = LoggerFactory.getLogger(ChainConnector.class);


    public Web3j getWeb3j() {
        return web3j;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public ChainConnector invoke(String password, String walletFilePath) {
        web3j = Web3j.build(new HttpService(
                "http://localhost:8110"));
        try {
            log.info("Connected to Ethereum client version: "
                    + web3j.web3ClientVersion().send().getWeb3ClientVersion());
        } catch (IOException e) {
            e.printStackTrace();
        }

        credentials = null;
        try {
            credentials = WalletUtils.loadCredentials(password, walletFilePath);
        } catch (IOException | CipherException e) {
            e.printStackTrace();
        }
        log.info("Credentials loaded");
        return this;
    }
}
