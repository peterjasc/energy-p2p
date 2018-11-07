pragma solidity ^0.4.20;

contract SmartContract {
    address public recipient;

    event BidAccepted(uint indexed roundId, uint indexed contractId, address indexed bidder, uint quantity, uint price, uint time);

    constructor() public {
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

        emit BidAccepted(_roundId, _cId, bidder, _quantity, msg.value, block.timestamp);
        return true;
    }
}

