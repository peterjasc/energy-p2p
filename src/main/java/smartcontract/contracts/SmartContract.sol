pragma solidity ^0.4.20;
contract SmartContract {
    Contract[] public contracts;
    event BidAccepted(uint indexed contractId, uint indexed contractIndex);
    uint noOfContracts;

    struct Contract {
        uint contractId;
        uint quantity;
        uint targetPrice;
        uint targetTime;
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
        BidAccepted(_cId, contractIndex);
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

    function getContractByIndex(uint _index) constant returns (uint[], uint[], uint[], uint[]) {
        
    }

}
