package smartcontract.app;

import rx.Subscriber;
import smartcontract.app.generated.SmartContract;

class BidAcceptedEventSubscriber extends Subscriber<SmartContract.BidAcceptedEventResponse> {
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
        System.out.println();
        System.out.println("contractId " + o.contractId);
        System.out.println("bidder " + o.bidder);
        System.out.println("price " + o.price);
        System.out.println("quantity " + o.quantity);
        System.out.println("time " + o.time);
        System.out.println("log " + o.log);


    }
}
