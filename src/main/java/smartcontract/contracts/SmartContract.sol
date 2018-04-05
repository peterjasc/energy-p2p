pragma solidity ^0.4.20;
contract SmartContract {
    Contract[] public contracts;
    event BidAccepted(uint indexed age, uint indexed height);

//Bid[] public bids;
    mapping(uint => Bid) bidMap;
    uint bidTableContractId = 0;
    uint noOfContracts;
    /*struct Date {
        uint day;
        uint month;
        uint year;
    }*/
    struct Contract {
        uint contractId;
        uint quantity;
        uint targetPrice;
        uint targetTime;
    }
    struct Bid {
        uint contractId;
        bytes32 supplier;
        address owner;
        uint price;
        uint bidTime;
    }

    function SmartContract() {
        noOfContracts = 0;
    }

    function addContract(uint _cId , uint _quantity, uint _targetPrice, uint _targetTime) returns (bool success) {
        Contract memory newContract; //creates new struct and memory
        /*Date memory _date;*/
        newContract.contractId = _cId;
        newContract.quantity = _quantity;
        newContract.targetPrice = _targetPrice;
        newContract.targetTime = _targetTime;
        contracts.push(newContract);//add elem to array
        return true;
    }

    function bid(uint _cid, bytes32 _supplier, uint _price, uint _bidTime) returns (bool success) {
        Bid memory newBid;
        newBid.contractId = _cid;
        newBid.supplier = _supplier;
        newBid.price = _price;
        newBid.bidTime = _bidTime;
        newBid.owner = msg.sender;
        bidMap[_cid] = newBid;
        return true;
    }

    function getClosedContracts() constant returns (uint[], uint[], uint[], uint[]) {
        uint length = contracts.length;
        uint[] memory contractId = new uint[](length);
        uint[] memory qty = new uint[](length);
        uint[] memory targetPrice = new uint[](length);
        uint[] memory targetTime = new uint[](length);

        for (uint i = 0; i < contracts.length; i++) {
            Contract memory currentContract;
            currentContract = contracts[i];

              contractId[i] = currentContract.contractId;
              qty[i] = currentContract.quantity;
              targetPrice[i] = currentContract.targetPrice;
              targetTime[i] = currentContract.targetTime;
        }
        return (contractId, qty, targetPrice, targetTime);
    }

    function getBid(uint _cid) constant returns (uint, bytes32, uint, uint){
        var bidMade = bidMap[_cid];
        uint contractId = bidMade.contractId;
        bytes32 supplier = bidMade.supplier;
        uint price = bidMade.price;
        uint timeToComplete = bidMade.bidTime;
        return (contractId, supplier, price, timeToComplete);
    }
}
