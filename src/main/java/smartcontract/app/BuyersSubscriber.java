package smartcontract.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;
import smartcontract.app.generated.SmartContract;

public class BuyersSubscriber extends Subscriber<SmartContract.BidAcceptedEventResponse> {
    private static final Logger log = LoggerFactory.getLogger(BuyersSubscriber.class);
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
        log.info("\ncontractId " + o.contractId);
        log.info("bidder " + o.bidder);
        log.info("price " + o.price);
        log.info("quantity " + o.quantity);
        log.info("time " + o.time);
        log.info("log " + o.log);


    }
}
