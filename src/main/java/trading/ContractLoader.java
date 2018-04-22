package trading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.tx.Contract;
import org.web3j.tx.ManagedTransaction;
import rx.Subscriber;
import smartcontract.app.generated.SmartContract;

import java.io.IOException;

class ContractLoader {
    public static final String CONTRACT_ADDRESS = "0xB08a4Aa7904d50155d10B8cE447Cc4b3fae212A4";
    private Web3j web3j;
    private Credentials credentials;
    private static final Logger log = LoggerFactory.getLogger(ContractLoader.class);

    public ContractLoader(Web3j web3j, Credentials credentials) {
        this.web3j = web3j;
        this.credentials = credentials;
    }

    public SmartContract invoke(Subscriber<SmartContract.BidAcceptedEventResponse> subscriber) {
        SmartContract contract = SmartContract.load(
                CONTRACT_ADDRESS, web3j, credentials, ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT);
        try {
            log.info("Contract is valid: " + contract.isValid());
        } catch (IOException e) {
            e.printStackTrace();
        }

        contract.bidAcceptedEventObservable(
                DefaultBlockParameterName.fromString(DefaultBlockParameterName.EARLIEST.getValue()),
                DefaultBlockParameterName.fromString(DefaultBlockParameterName.LATEST.getValue())).subscribe(subscriber);

        return contract;
    }
}
