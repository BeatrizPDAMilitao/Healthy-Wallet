package com.example.ethktprototype

//import com.example.ethktprototype.Web3jService.env
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.quorum.Quorum

const val INFURA_API_KEY = "MY_INFURA_API_KEY"

object Web3jService {

    //val env = EnvVars()

    fun build(selectedNetwork: Network): Quorum {
        return Quorum.build(HttpService(selectedNetwork.url))
    }

}

enum class Network(
    val displayName: String,
    val url: String,
    val chainId: Long,
    val covalentChainName: String
) {
    QUORUM(
        "Quorum",
        "http://192.168.1.2:22000", //192.168.1.76    172.26.146.19
        1337,
        covalentChainName = "quorum"
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


