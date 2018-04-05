var SmartContract = artifacts.require("./contracts/SmartContract.sol");

module.exports = function(deployer) {
  deployer.deploy(SmartContract);

};
