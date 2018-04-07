pragma solidity ^0.4.20;

contract SmartContract {
    event BidAccepted(uint indexed contractId, uint indexed contractIndex, uint quantity, uint targetPrice, uint targetTime);

    uint noOfContracts;

    function SmartContract() public {
        noOfContracts = 0;
    }

    function addContract(uint _cId, uint _quantity, uint _targetPrice, uint _targetTime) public returns (bool success) {
        noOfContracts = noOfContracts + 1;
        emit BidAccepted(_cId, noOfContracts, _quantity, _targetPrice, _targetTime);
        return true;
    }

}
