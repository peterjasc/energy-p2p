package smartcontract.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;
import smartcontract.app.generated.SmartContract;

public class BiddersSubscriber extends Subscriber<SmartContract.BidAcceptedEventResponse> {
    private static final Logger log = LoggerFactory.getLogger(BiddersSubscriber.class);
    @Override
    public void onCompleted() {
        System.out.println("onCompleted");
    }

    @Override
    public void onError(Throwable e) {
       e.printStackTrace();
    }

    @Override
    public void onNext(SmartContract.BidAcceptedEventResponse o) {
        //todo: find out if the money was received

    }
}
