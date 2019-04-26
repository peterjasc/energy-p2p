package smartcontract.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import rx.Subscriber;
import smartcontract.app.generated.SmartContract;
import trading.ContractLoader;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class ContractDeployer {

    private static final Logger log = LoggerFactory.getLogger(ContractDeployer.class);

    public static void main(String[] args) throws Exception {
        new ContractDeployer().run();
    }

    private void run() throws Exception {
        ContractLoader contractLoader = new ContractLoader("password",
                "/home/peter/Documents/energy-p2p/private-testnet/keystore/UTC--2018-12-31T14-04-41.979553267Z--34c2c13ecaf560f284adb20a002c01e31a84646a");
//      DEPLOY
//        log.info("Deploying smart contract (remember to start mining!!!)");
//        SmartContract contract = contractLoader.deployContract();
//        System.exit(0);

        SmartContract contract = contractLoader.loadContract();
        log.info("Contract is valid: " + contract.isValid());

//      TEST SENDINGContractLoader
//     event BidAccepted(uint indexed roundId, uint indexed contractId, address indexed bidder, uint quantity, uint price, uint time);

//        log.info("Value stored in remote smart contract: " + contract.addContract(
//                new BigInteger("3", 10),
//                new BigInteger("1", 10),
//                "0x521892450a22dc762198f6ce597cfc6d85f673a3",
//                new BigInteger("10", 10),
//                new BigInteger("10", 10)
//        ).send());

// Get all the contracts
        Subscriber<SmartContract.BidAcceptedEventResponse> subscriber = new DeployersSubscriber();

        contract.bidAcceptedEventObservable(
                DefaultBlockParameterName.fromString(DefaultBlockParameterName.EARLIEST.getValue()),
                DefaultBlockParameterName.fromString(DefaultBlockParameterName.LATEST.getValue())).subscribe(subscriber);

// Get all the contracts from a specific round

//        SmartContract smartContract = contractLoader.loadContract();
//        System.out.println("roundId,bidder,quantity");
//        List<SmartContract.BidAcceptedEventResponse> list
//                = contractLoader.getLogsForRoundIdRange(BigInteger.valueOf(10), BigInteger.valueOf(19), smartContract);
//        for (SmartContract.BidAcceptedEventResponse o : list) {
//        System.out.println(o.roundId + "," + o.bidder + "," + o.quantity);

//        log.info("\n");
//        log.info("roundId " + o.roundId);
//        log.info("contractId " + o.contractId);
//        log.info("bidder " + o.bidder);
//        log.info("quantity " + o.quantity);
//        log.info("price " + o.price);
//        log.info("time " + o.time);
//        log.info("log " + o.log);
//        }

    }

}
