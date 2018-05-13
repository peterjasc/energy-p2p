pragma solidity ^0.4.20;

contract SmartContract {
    address public recipient;

    event BidAccepted(uint indexed roundId, uint contractId,
        address indexed bidder, address indexed buyer, uint quantity, uint price, uint time);

    function SmartContract() public {
    }

    // msg.value == price
    function addContract(uint _roundId, uint _cId, address bidder, uint _quantity)
    payable public returns (bool success) {

        address myAddress = this;

        if (myAddress.balance >= msg.value) {
            bidder.transfer(msg.value);
        } else {
            return false;
        }

        emit BidAccepted(_roundId, _cId, bidder, myAddress, _quantity, msg.value, block.timestamp);
        return true;
    }
}
