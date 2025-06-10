package com.example.ethktprototype

//import com.example.ethktprototype.Web3jService.env
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

const val INFURA_API_KEY = "1e34aa8ed01747bfba701b541f69ea6f"

object Web3jService {

    //val env = EnvVars()

    fun build(selectedNetwork: Network): Web3j {
        return Web3j.build(HttpService(selectedNetwork.url))
    }

}

enum class Network(
    val displayName: String,
    val url: String,
    val chainId: Long,
    val covalentChainName: String
) {
    SEPOLIA(
        "Sepolia",
        "https://sepolia.infura.io/v3/$INFURA_API_KEY",
        11155111,
        covalentChainName = "eth-sepolia"
    ),
    QUORUM(
        "Quorum",
        "http://192.168.1.76:8545", //192.168.1.76    172.26.146.19
        1337, // TODO: confirm this chain ID
        covalentChainName = "quorum"
    ),
    ARBITRUM_SEPOLIA_TESTNET(
        "Arbitrum Sepolia Testnet",
        "https://arbitrum-sepolia.infura.io/v3/$INFURA_API_KEY",
        421614,
        covalentChainName = "arbitrum-sepolia"
    ),
    POLYGON_MAINNET(
        "Polygon",
        "https://polygon-mainnet.infura.io/v3/$INFURA_API_KEY",
        137,
        covalentChainName = "matic-mainnet"
    ),
    MUMBAI_TESTNET(
        "Mumbai Testnet",
        "https://polygon-mumbai.infura.io/v3/$INFURA_API_KEY",
        80001,
        covalentChainName = "matic-mumbai"
    ),
    ETH_MAINNET(
    "Ethereum",
    "https://mainnet.infura.io/v3/$INFURA_API_KEY",
    1,
    covalentChainName = "eth-mainnet"
    )
}


