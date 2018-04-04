// Allows us to use ES6 in our migrations and tests.

module.exports = {
    networks: {
        kovan: {
            network_id: 42,
            host: 'localhost',
            port: 8545
        },
        development: {
            from: '0x9b538e4a5eba8ac0f83d6025cbbabdbd13a32bfe',
            host: 'localhost',
            port: 8110,
            network_id: '567345' // Match any network id

        }
    }
}
