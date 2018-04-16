package smartcontract.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.tx.ManagedTransaction;
import rx.Subscriber;
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

//        log.info("Deploying smart contract (remember to start mining!!!)");
//        SmartContract contractS = SmartContract.deploy(
//                web3j, credentials,
//                ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT).send();
//        System.exit(0);

        SmartContract contract = SmartContract.load(
                "0x5F46B91769556Bff249915ba536892285D0C1e53", web3j, credentials, ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT);
        log.info("Contract is valid: " + contract.isValid());
        String contractAddress = contract.getContractAddress();
        log.info("Smart contract deployed to address " + contractAddress);

        //uint _cId, uint _quantity, uint _targetPrice, uint _targetTime)

//        log.info("Value stored in remote smart contract: " + contract.addContract(
//                new BigInteger("1", 10),
//                new BigInteger("10", 10),
//                new BigInteger("10", 10),
//                new BigInteger("1000", 10)
//        ).send());

        Subscriber<SmartContract.BidAcceptedEventResponse> subscriber = new BidAcceptedEventSubscriber();

        contract.bidAcceptedEventObservable(
                DefaultBlockParameterName.fromString(DefaultBlockParameterName.EARLIEST.getValue()),
                DefaultBlockParameterName.fromString(DefaultBlockParameterName.LATEST.getValue())).subscribe(subscriber);


    }

}
