package trading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.tx.ManagedTransaction;
import rx.Subscriber;
import smartcontract.app.generated.SmartContract;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class ContractLoader {
    public static final String CONTRACT_ADDRESS = "0x20CeC4A892b9C2692eb0ED4ed13AE8c9Da411653";
    private Web3j web3j;
    private Credentials credentials;
    private static final Logger log = LoggerFactory.getLogger(ContractLoader.class);

    public Web3j getWeb3j() {
        return web3j;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public ContractLoader(String password, String walletFilePath) {
        web3j = Web3j.build(new HttpService(
                "http://localhost:8110"));
        try {
            log.info("Connected to Ethereum client version: "
                    + web3j.web3ClientVersion().send().getWeb3ClientVersion());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            credentials = WalletUtils.loadCredentials(password, walletFilePath);
            log.info("Credentials loaded");
        } catch (IOException | CipherException e) {
            e.printStackTrace();
        }
    }

    public SmartContract deployContract() {
        SmartContract contract = null;
        RemoteCall<SmartContract> contractRemoteCall = SmartContract.deploy(web3j, credentials, ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT);
        try {
            contract = contractRemoteCall.send();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            log.info("Contract is valid: " + Objects.requireNonNull(contract).isValid());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contract;
    }

    public SmartContract loadContractWithSubscriberFromEarliestToLatest(Subscriber<SmartContract.BidAcceptedEventResponse> subscriber) {
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

    public SmartContract loadContract() {
        SmartContract contract = SmartContract.load(
                CONTRACT_ADDRESS, web3j, credentials, ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT);
        try {
            log.info("Contract is valid: " + contract.isValid());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contract;
    }

    public Set<SmartContract.BidAcceptedEventResponse> getLogsForRoundId(BigInteger roundId, SmartContract smartContract) {
        HashSet<SmartContract.BidAcceptedEventResponse> logsForAllBids = new HashSet<>();
        List<EthLog.LogResult> logResults = getLogsForBidEvents();

        for (EthLog.LogResult logResult : logResults) {
            EthLog.LogObject logObject
                    = (EthLog.LogObject) logResult.get();

            Optional<TransactionReceipt> transactionReceipt = Optional.empty();
            try {
                transactionReceipt = web3j.ethGetTransactionReceipt(logObject.getTransactionHash()).send().getTransactionReceipt();
            } catch (IOException e) {
                e.printStackTrace();
            }

            List<SmartContract.BidAcceptedEventResponse> bidAcceptedEventResponses
                    = smartContract.getBidAcceptedEvents(transactionReceipt.orElse(new TransactionReceipt()));

            logsForAllBids.addAll(bidAcceptedEventResponses);
        }


        return logsForAllBids.stream()
                .filter(x -> x.roundId.compareTo(roundId) == 0)
                .collect(Collectors.toSet());
    }

    BigInteger findRoundIdFromLastBidEvent(SmartContract smartContract) {
        List<EthLog.LogResult> logResults = getLogsForBidEvents();

        if (logResults.isEmpty()) {
            return BigInteger.ONE;
        }

        EthLog.LogResult lastResult = logResults.get(logResults.size() - 1);

        EthLog.LogObject logObject
                = (EthLog.LogObject) lastResult.get();

        Optional<TransactionReceipt> transactionReceipt = Optional.empty();
        try {
            transactionReceipt = web3j.ethGetTransactionReceipt(logObject.getTransactionHash()).send().getTransactionReceipt();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<SmartContract.BidAcceptedEventResponse> bidAcceptedEventResponses
                = smartContract.getBidAcceptedEvents(transactionReceipt.orElse(new TransactionReceipt()));

        SmartContract.BidAcceptedEventResponse lastBidAcceptedEventResponse
                = bidAcceptedEventResponses.get(bidAcceptedEventResponses.size() - 1);

        return lastBidAcceptedEventResponse.roundId;
    }


    private List<EthLog.LogResult> getLogsForBidEvents() {
        List<EthLog.LogResult> logResults = new ArrayList<>();
        final Event event = new Event("BidAccepted",
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Address>() {
                }),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Uint256>() {
                }));
        EthFilter filter = new EthFilter(
                DefaultBlockParameterName.fromString(DefaultBlockParameterName.EARLIEST.getValue()),
                DefaultBlockParameterName.fromString(DefaultBlockParameterName.LATEST.getValue()),
                CONTRACT_ADDRESS);
        filter.addSingleTopic(EventEncoder.encode(event));

        try {
            logResults = web3j.ethGetLogs(filter).send().getLogs();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return logResults;
    }


}
