pragma solidity ^0.4.20;

contract SmartContract {
    address public recipient;

    event BidAccepted(uint indexed contractId, address indexed bidder, uint quantity, uint price, uint indexed time);

    function SmartContract() public {
    }

    function addContract(uint _cId, address buyer, uint _quantity, uint _unitPrice)
    payable public returns (bool success) {

        address myAddress = this;

        uint amount = _quantity*_unitPrice;
        if (myAddress.balance >= amount) {
            buyer.transfer(amount);
        } else {
            return false;
        }

        emit BidAccepted(_cId, buyer, _quantity, _unitPrice, block.timestamp);
        return true;
    }
}
