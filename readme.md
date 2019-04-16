
### Testing contract deployment

Running the node:

`geth --datadir ~/Documents/energy-p2p/private-testnet/ --ipcpath geth.ipc --nodiscover --networkid 567345 --rpc --rpcaddr "localhost" --rpcport 8110 --rpcapi "db,eth,net,web3" --maxpeers 0 --verbosity 3 --pprof --pprofport 6110 console`


Compiling code:

`solc src/main/java/smartcontract/contracts/SmartContract.sol --bin --abi --optimize -o src/main/java/smartcontract/contracts/`

Generating Java classes from .abi and .bin

`/home/peter/Documents/web3j-3.3.1/bin/web3j solidity generate --javaTypes src/main/java/smartcontract/contracts/SmartContract.bin src/main/java/smartcontract/contracts/SmartContract.abi -o src/main/java/ -p smartcontract.app.generated`


### Interacting with geth


geth --datadir /home/peter/Documents/energy-p2p/private-testnet/ account list

####account

web3.personal.unlockAccount("0x9b538e4a5eba8ac0f83d6025cbbabdbd13a32bfe","password")

eth.sendTransaction({from:'634912c016183f2cf416a3a35afa50de4a4ae726', to:'f70eb6650142417be6d4887acb4d132fb784f8b2', value: web3.toWei(1000000, "wei"), gas:21000});