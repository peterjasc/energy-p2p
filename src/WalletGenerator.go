package main

import (
	"context"
	"crypto/ecdsa"
	"fmt"
	"log"
	"math/big"

	"github.com/ethereum/go-ethereum/accounts/keystore"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/crypto"
	"github.com/ethereum/go-ethereum/ethclient"
)

func main() {
	ks := keystore.NewKeyStore("../private-testnet/keystore", keystore.StandardScryptN, keystore.StandardScryptP)
	password := "password"

	client, err := ethclient.Dial("http://localhost:8110")
	if err != nil {
		log.Fatal(err)
	}

	// coinbase keyfile in keystore
	key := "{\"address\":\"34c2c13ecaf560f284adb20a002c01e31a84646a\",\"crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"446dcdb4b3cc94432b8f1c87226570b2cea9aa4e96cd2cdbb6c7d870ba322ef5\",\"cipherparams\":{\"iv\":\"c684bebd41a8276bbc78c1976ffe42df\"},\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":262144,\"p\":1,\"r\":8,\"salt\":\"9eb499b85487b9c6070406f156339898bd5f10b9db4c67625dd9ac5cc4a4bf67\"},\"mac\":\"a3873b895497f7bfa8c5bbb22f79f259d1d97a72f89894c17819e1a3526c05be\"},\"id\":\"5d661c78-3de6-4b7e-938c-3a3098a2969c\",\"version\":3}"
	unlockedKey, err := keystore.DecryptKey([]byte(key), password)
	fmt.Printf("%+v\n", unlockedKey.PrivateKey)
	if err != nil {
		log.Fatal(err)
	}

	publicKey := unlockedKey.PrivateKey.Public()
	publicKeyECDSA, ok := publicKey.(*ecdsa.PublicKey)
	if !ok {
		log.Fatal("error casting public key to ECDSA")
	}

	fromAddress := crypto.PubkeyToAddress(*publicKeyECDSA)

	// create new addresses and send ether to them from coinbase
	for i := 0; i < 200; i++ {
		account, err := ks.NewAccount(password)
		if err != nil {
			log.Fatal(err)
		}

		fmt.Println(account.Address.Hex())

		nonce, err := client.PendingNonceAt(context.Background(), fromAddress)
		if err != nil {
			log.Fatal(err)
		}

		value := new(big.Int)
		value.SetString("100000000000000000", 10) // in wei (0.1 eth)
		gasLimit := uint64(21000)                 // in units
		gasPrice, err := client.SuggestGasPrice(context.Background())
		if err != nil {
			log.Fatal(err)
		}

		// fmt.Printf("gL %v, gP %v, v %v \n", gasLimit, gasPrice, value)

		toAddress := common.HexToAddress(account.Address.Hex())
		var data []byte
		// fmt.Printf("nonce %+v, toAddress %+v \n", nonce, toAddress)
		tx := types.NewTransaction(nonce, toAddress, value, gasLimit, gasPrice, data)
		signedTx, err := types.SignTx(tx, types.HomesteadSigner{}, unlockedKey.PrivateKey)
		if err != nil {
			log.Fatal(err)
		}

		// fmt.Printf("\n\n tx %+v \n\n sTx %+v \n", tx, signedTx)
		err = client.SendTransaction(context.Background(), signedTx)
		if err != nil {
			log.Fatal(err)
		}

		fmt.Printf("tx sent: %s", signedTx.Hash().Hex())
	}
}
