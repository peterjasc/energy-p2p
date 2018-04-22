pragma solidity ^0.4.20;

contract SmartContract {
    address public recipient;

    event BidAccepted(uint indexed contractId, address indexed bidder, uint quantity, uint price, uint indexed time);

    function SmartContract() public {
    }

    // msg.value == price
    function addContract(uint _cId, address buyer, uint _quantity)
    payable public returns (bool success) {

        address myAddress = this;

        if (myAddress.balance >= msg.value) {
            buyer.transfer(msg.value);
        } else {
            return false;
        }

        emit BidAccepted(_cId, buyer, _quantity, msg.value, block.timestamp);
        return true;
    }
}
