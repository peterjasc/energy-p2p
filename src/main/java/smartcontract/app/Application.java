package smartcontract.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.tx.ManagedTransaction;
import smartcontract.app.generated.SmartContract;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws Exception {
        new Application().run();
    }

    private void run() throws Exception {
        Web3j web3j = Web3j.build(new HttpService(
                "http://localhost:8110"));
        log.info("Connected to Ethereum client version: "
                + web3j.web3ClientVersion().send().getWeb3ClientVersion());

        Credentials credentials =
                WalletUtils.loadCredentials(
                        "password",
                        "/home/peter/Documents/energy-p2p/private-testnet/keystore/UTC--2018-04-04T09-17-25.118212336Z--9b538e4a5eba8ac0f83d6025cbbabdbd13a32bfe");
        log.info("Credentials loaded");

//        log.info("Deploying smart contract");
//        SmartContract contract = SmartContract.deploy(
//                web3j, credentials,
//                ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT).send();


        SmartContract contract = SmartContract.load(
                "0x6ee6a152166d8916e861bae600c02e3a17208ffe", web3j, credentials, ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT);
        log.info("Contract is valid: "+ contract.isValid());
        String contractAddress = contract.getContractAddress();
        log.info("Smart contract deployed to address " + contractAddress);

//        log.info("Value stored in remote smart contract: " + contract.greet().send());

        // Lets modify the value in our smart contract
//        TransactionReceipt transactionReceipt = contract.newGreeting("Well hello again").send();

//        log.info("New value stored in remote smart contract: " + contract.greet().send());

        // Events enable us to log specific events happening during the execution of our smart
        // contract to the blockchain. Index events cannot be logged in their entirety.
        // For Strings and arrays, the hash of values is provided, not the original value.
        // For further information, refer to https://docs.web3j.io/filters.html#filters-and-events
//        for (Greeter.ModifiedEventResponse event : contract.getModifiedEvents(transactionReceipt)) {
//            log.info("Modify event fired, previous value: " + event.oldGreeting
//                    + ", new value: " + event.newGreeting);
//            log.info("Indexed event previous value: " + Numeric.toHexString(event.oldGreetingIdx)
//                    + ", new value: " + Numeric.toHexString(event.newGreetingIdx));
//        }
    }
}
