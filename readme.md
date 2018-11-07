
### Testing contract deployment

Running the node:

`geth --datadir ~/Documents/energy-p2p/private-testnet/ --ipcpath geth.ipc --nodiscover --networkid 567345 --rpc --rpcaddr "localhost" --rpcport 8110 --rpcapi "db,eth,net,web3" --rpccorsdomain="localhost" --maxpeers 0 --verbosity 3 --pprof --pprofport 6110 console`


Compiling code:

`solc src/main/java/smartcontract/contracts/SmartContract.sol --bin --abi --optimize -o src/main/java/smartcontract/contracts/`

Generating Java classes from .abi and .bin

`/home/peter/Documents/web3j-3.3.1/bin/web3j solidity generate --javaTypes src/main/java/smartcontract/contracts/SmartContract.bin src/main/java/smartcontract/contracts/SmartContract.abi -o src/main/java/ -p smartcontract.app.generated`


