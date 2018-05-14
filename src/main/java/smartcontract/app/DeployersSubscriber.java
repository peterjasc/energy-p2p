package smartcontract.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;
import smartcontract.app.generated.SmartContract;

public class DeployersSubscriber extends Subscriber<SmartContract.BidAcceptedEventResponse> {
    private static final Logger log = LoggerFactory.getLogger(DeployersSubscriber.class);
    @Override
    public void onCompleted() {
        log.info("onCompleted");
    }

    @Override
    public void onError(Throwable e) {
       e.printStackTrace();
    }

    @Override
    public void onNext(SmartContract.BidAcceptedEventResponse o) {

        log.info("\n");
        log.info("roundId " + o.roundId);
        log.info("contractId " + o.contractId);
        log.info("bidder " + o.bidder);
        log.info("quantity " + o.quantity);
        log.info("price " + o.price);
        log.info("time " + o.time);
        log.info("log " + o.log);


    }
}
