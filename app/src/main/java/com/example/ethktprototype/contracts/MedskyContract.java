package com.example.ethktprototype.contracts;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Array;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.5.0.
 */
@SuppressWarnings("rawtypes")
public class MedskyContract extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b50612678806100206000396000f3fe608060405234801561001057600080fd5b50600436106100a95760003560e01c8063678798c311610071578063678798c314610176578063aca31d71146101a6578063c090b4df146101d6578063d5175218146101f2578063d8da4f571461020e578063f21d04491461022a576100a9565b80630577decd146100ae57806319dddfa5146100ca578063540248f0146100e6578063596cd4bd14610116578063625eb17414610146575b600080fd5b6100c860048036038101906100c3919061172a565b61025a565b005b6100e460048036038101906100df91906117d1565b6102e1565b005b61010060048036038101906100fb9190611849565b6104c3565b60405161010d91906118ad565b60405180910390f35b610130600480360381019061012b91906119ae565b6104f8565b60405161013d9190611c2b565b60405180910390f35b610160600480360381019061015b9190611c4d565b6107b8565b60405161016d9190611c2b565b60405180910390f35b610190600480360381019061018b9190611c4d565b610a6d565b60405161019d9190611e6a565b60405180910390f35b6101c060048036038101906101bb9190611849565b610cd7565b6040516101cd91906118ad565b60405180910390f35b6101f060048036038101906101eb91906117d1565b610d0c565b005b61020c600480360381019061020791906119ae565b610e5b565b005b61022860048036038101906102239190611e8c565b610fda565b005b610244600480360381019061023f91906117d1565b6111db565b6040516102519190611f6e565b60405180910390f35b600060048460405161026c9190611fcc565b908152602001604051809103902090508281600001908161028d91906121ef565b508181600101908161029f91906121ef565b507fab256a724af0898f21f537147611d37149228f165cee43896fd82ca782d341b98484846040516102d39392919061230b565b60405180910390a150505050565b6001826040516102f19190611fcc565b908152602001604051809103902060009054906101000a900460ff1661034c576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610343906123a3565b60405180910390fd5b6000600167ffffffffffffffff811115610369576103686115ff565b5b60405190808252806020026020018201604052801561039c57816020015b60608152602001906001900390816103875790505b50905082816000815181106103b4576103b36123c3565b5b60200260200101819052506103c98183610e5b565b6000836040516103d99190611fcc565b9081526020016040518091039020600080820160006101000a81549073ffffffffffffffffffffffffffffffffffffffff021916905560018201600061041f9190611481565b60028201600061042f9190611481565b60038201600090555050600060018460405161044b9190611fcc565b908152602001604051809103902060006101000a81548160ff0219169083151502179055503373ffffffffffffffffffffffffffffffffffffffff167fcf36278c9e208713df56b3b4372a826e000cc47a90e4dbb82b57e842b82d6532846040516104b691906123f2565b60405180910390a2505050565b60006001826040516104d59190611fcc565b908152602001604051809103902060009054906101000a900460ff169050919050565b60606000835167ffffffffffffffff811115610517576105166115ff565b5b60405190808252806020026020018201604052801561055057816020015b61053d6114c1565b8152602001906001900390816105355790505b50905060005b84518110156107a3576001858281518110610574576105736123c3565b5b60200260200101516040516105899190611fcc565b908152602001604051809103902060009054906101000a900460ff16156107905760008582815181106105bf576105be6123c3565b5b60200260200101516040516105d49190611fcc565b90815260200160405180910390206040518060800160405290816000820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200160018201805461065390612012565b80601f016020809104026020016040519081016040528092919081815260200182805461067f90612012565b80156106cc5780601f106106a1576101008083540402835291602001916106cc565b820191906000526020600020905b8154815290600101906020018083116106af57829003601f168201915b505050505081526020016002820180546106e590612012565b80601f016020809104026020016040519081016040528092919081815260200182805461071190612012565b801561075e5780601f106107335761010080835404028352916020019161075e565b820191906000526020600020905b81548152906001019060200180831161074157829003601f168201915b50505050508152602001600382015481525050828281518110610784576107836123c3565b5b60200260200101819052505b808061079b90612443565b915050610556565b506107ae8484610e5b565b8091505092915050565b60606000825167ffffffffffffffff8111156107d7576107d66115ff565b5b60405190808252806020026020018201604052801561081057816020015b6107fd6114c1565b8152602001906001900390816107f55790505b50905060005b8351811015610a63576001848281518110610834576108336123c3565b5b60200260200101516040516108499190611fcc565b908152602001604051809103902060009054906101000a900460ff1615610a5057600084828151811061087f5761087e6123c3565b5b60200260200101516040516108949190611fcc565b90815260200160405180910390206040518060800160405290816000820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200160018201805461091390612012565b80601f016020809104026020016040519081016040528092919081815260200182805461093f90612012565b801561098c5780601f106109615761010080835404028352916020019161098c565b820191906000526020600020905b81548152906001019060200180831161096f57829003601f168201915b505050505081526020016002820180546109a590612012565b80601f01602080910402602001604051908101604052809291908181526020018280546109d190612012565b8015610a1e5780601f106109f357610100808354040283529160200191610a1e565b820191906000526020600020905b815481529060010190602001808311610a0157829003601f168201915b50505050508152602001600382015481525050828281518110610a4457610a436123c3565b5b60200260200101819052505b8080610a5b90612443565b915050610816565b5080915050919050565b60606000825167ffffffffffffffff811115610a8c57610a8b6115ff565b5b604051908082528060200260200182016040528015610ac557816020015b610ab26114ff565b815260200190600190039081610aaa5790505b50905060005b8351811015610ccd576003848281518110610ae957610ae86123c3565b5b6020026020010151604051610afe9190611fcc565b908152602001604051809103902060009054906101000a900460ff1615610cba576002848281518110610b3457610b336123c3565b5b6020026020010151604051610b499190611fcc565b90815260200160405180910390206040518060600160405290816000820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200160018201805480602002602001604051908101604052809291908181526020016000905b82821015610c89578382906000526020600020018054610bfc90612012565b80601f0160208091040260200160405190810160405280929190818152602001828054610c2890612012565b8015610c755780601f10610c4a57610100808354040283529160200191610c75565b820191906000526020600020905b815481529060010190602001808311610c5857829003601f168201915b505050505081526020019060010190610bdd565b505050508152602001600282015481525050828281518110610cae57610cad6123c3565b5b60200260200101819052505b8080610cc590612443565b915050610acb565b5080915050919050565b6000600382604051610ce99190611fcc565b908152602001604051809103902060009054906101000a900460ff169050919050565b60008083604051610d1d9190611fcc565b90815260200160405180910390209050338160000160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555081816001019081610d8191906121ef565b506040518060400160405280600381526020017f78797a0000000000000000000000000000000000000000000000000000000000815250816002019081610dc891906121ef565b5042816003018190555060018084604051610de39190611fcc565b908152602001604051809103902060006101000a81548160ff0219169083151502179055503373ffffffffffffffffffffffffffffffffffffffff167fe006f2fa2489218e63727a3f4a2f48ea3e506b0c0c6c4f03e5577c99b63a3d0c84604051610e4e91906123f2565b60405180910390a2505050565b600381604051610e6b9190611fcc565b908152602001604051809103902060009054906101000a900460ff1615610ec7576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610ebe906124d7565b60405180910390fd5b6000600282604051610ed99190611fcc565b90815260200160405180910390209050338160000160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555082816001019080519060200190610f44929190611536565b504281600201819055506001600383604051610f609190611fcc565b908152602001604051809103902060006101000a81548160ff0219169083151502179055503373ffffffffffffffffffffffffffffffffffffffff167f149348318dc84534a04eeacd31d9ecb5ebc1cc646cb3eb727eaba371410aa0598385604051610fcd92919061257d565b60405180910390a2505050565b805182511461101e576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161101590612600565b60405180910390fd5b60005b8251811015611188576000808483815181106110405761103f6123c3565b5b60200260200101516040516110559190611fcc565b90815260200160405180910390209050338160000160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508282815181106110bb576110ba6123c3565b5b60200260200101518160010190816110d391906121ef565b506040518060400160405280600381526020017f78797a000000000000000000000000000000000000000000000000000000000081525081600201908161111a91906121ef565b5042816003018190555060018085848151811061113a576111396123c3565b5b602002602001015160405161114f9190611fcc565b908152602001604051809103902060006101000a81548160ff02191690831515021790555050808061118090612443565b915050611021565b503373ffffffffffffffffffffffffffffffffffffffff167f51f7176ef32086123e7cc8db274f6a77714a6c7769e1b3191183c4aa0769f2f8836040516111cf9190612620565b60405180910390a25050565b6111e36114c1565b6001836040516111f39190611fcc565b908152602001604051809103902060009054906101000a900460ff1661124e576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401611245906123a3565b60405180910390fd5b6000600167ffffffffffffffff81111561126b5761126a6115ff565b5b60405190808252806020026020018201604052801561129e57816020015b60608152602001906001900390816112895790505b50905083816000815181106112b6576112b56123c3565b5b60200260200101819052506112cb8184610e5b565b6000846040516112db9190611fcc565b90815260200160405180910390206040518060800160405290816000820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200160018201805461135a90612012565b80601f016020809104026020016040519081016040528092919081815260200182805461138690612012565b80156113d35780601f106113a8576101008083540402835291602001916113d3565b820191906000526020600020905b8154815290600101906020018083116113b657829003601f168201915b505050505081526020016002820180546113ec90612012565b80601f016020809104026020016040519081016040528092919081815260200182805461141890612012565b80156114655780601f1061143a57610100808354040283529160200191611465565b820191906000526020600020905b81548152906001019060200180831161144857829003601f168201915b5050505050815260200160038201548152505091505092915050565b50805461148d90612012565b6000825580601f1061149f57506114be565b601f0160209004906000526020600020908101906114bd919061158f565b5b50565b6040518060800160405280600073ffffffffffffffffffffffffffffffffffffffff1681526020016060815260200160608152602001600081525090565b6040518060600160405280600073ffffffffffffffffffffffffffffffffffffffff16815260200160608152602001600081525090565b82805482825590600052602060002090810192821561157e579160200282015b8281111561157d57825182908161156d91906121ef565b5091602001919060010190611556565b5b50905061158b91906115ac565b5090565b5b808211156115a8576000816000905550600101611590565b5090565b5b808211156115cc57600081816115c39190611481565b506001016115ad565b5090565b6000604051905090565b600080fd5b600080fd5b600080fd5b600080fd5b6000601f19601f8301169050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b611637826115ee565b810181811067ffffffffffffffff82111715611656576116556115ff565b5b80604052505050565b60006116696115d0565b9050611675828261162e565b919050565b600067ffffffffffffffff821115611695576116946115ff565b5b61169e826115ee565b9050602081019050919050565b82818337600083830152505050565b60006116cd6116c88461167a565b61165f565b9050828152602081018484840111156116e9576116e86115e9565b5b6116f48482856116ab565b509392505050565b600082601f830112611711576117106115e4565b5b81356117218482602086016116ba565b91505092915050565b600080600060608486031215611743576117426115da565b5b600084013567ffffffffffffffff811115611761576117606115df565b5b61176d868287016116fc565b935050602084013567ffffffffffffffff81111561178e5761178d6115df565b5b61179a868287016116fc565b925050604084013567ffffffffffffffff8111156117bb576117ba6115df565b5b6117c7868287016116fc565b9150509250925092565b600080604083850312156117e8576117e76115da565b5b600083013567ffffffffffffffff811115611806576118056115df565b5b611812858286016116fc565b925050602083013567ffffffffffffffff811115611833576118326115df565b5b61183f858286016116fc565b9150509250929050565b60006020828403121561185f5761185e6115da565b5b600082013567ffffffffffffffff81111561187d5761187c6115df565b5b611889848285016116fc565b91505092915050565b60008115159050919050565b6118a781611892565b82525050565b60006020820190506118c2600083018461189e565b92915050565b600067ffffffffffffffff8211156118e3576118e26115ff565b5b602082029050602081019050919050565b600080fd5b600061190c611907846118c8565b61165f565b9050808382526020820190506020840283018581111561192f5761192e6118f4565b5b835b8181101561197657803567ffffffffffffffff811115611954576119536115e4565b5b80860161196189826116fc565b85526020850194505050602081019050611931565b5050509392505050565b600082601f830112611995576119946115e4565b5b81356119a58482602086016118f9565b91505092915050565b600080604083850312156119c5576119c46115da565b5b600083013567ffffffffffffffff8111156119e3576119e26115df565b5b6119ef85828601611980565b925050602083013567ffffffffffffffff811115611a1057611a0f6115df565b5b611a1c858286016116fc565b9150509250929050565b600081519050919050565b600082825260208201905092915050565b6000819050602082019050919050565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b6000611a7d82611a52565b9050919050565b611a8d81611a72565b82525050565b600081519050919050565b600082825260208201905092915050565b60005b83811015611acd578082015181840152602081019050611ab2565b60008484015250505050565b6000611ae482611a93565b611aee8185611a9e565b9350611afe818560208601611aaf565b611b07816115ee565b840191505092915050565b6000819050919050565b611b2581611b12565b82525050565b6000608083016000830151611b436000860182611a84565b5060208301518482036020860152611b5b8282611ad9565b91505060408301518482036040860152611b758282611ad9565b9150506060830151611b8a6060860182611b1c565b508091505092915050565b6000611ba18383611b2b565b905092915050565b6000602082019050919050565b6000611bc182611a26565b611bcb8185611a31565b935083602082028501611bdd85611a42565b8060005b85811015611c195784840389528151611bfa8582611b95565b9450611c0583611ba9565b925060208a01995050600181019050611be1565b50829750879550505050505092915050565b60006020820190508181036000830152611c458184611bb6565b905092915050565b600060208284031215611c6357611c626115da565b5b600082013567ffffffffffffffff811115611c8157611c806115df565b5b611c8d84828501611980565b91505092915050565b600081519050919050565b600082825260208201905092915050565b6000819050602082019050919050565b600081519050919050565b600082825260208201905092915050565b6000819050602082019050919050565b6000611cfa8383611ad9565b905092915050565b6000602082019050919050565b6000611d1a82611cc2565b611d248185611ccd565b935083602082028501611d3685611cde565b8060005b85811015611d725784840389528151611d538582611cee565b9450611d5e83611d02565b925060208a01995050600181019050611d3a565b50829750879550505050505092915050565b6000606083016000830151611d9c6000860182611a84565b5060208301518482036020860152611db48282611d0f565b9150506040830151611dc96040860182611b1c565b508091505092915050565b6000611de08383611d84565b905092915050565b6000602082019050919050565b6000611e0082611c96565b611e0a8185611ca1565b935083602082028501611e1c85611cb2565b8060005b85811015611e585784840389528151611e398582611dd4565b9450611e4483611de8565b925060208a01995050600181019050611e20565b50829750879550505050505092915050565b60006020820190508181036000830152611e848184611df5565b905092915050565b60008060408385031215611ea357611ea26115da565b5b600083013567ffffffffffffffff811115611ec157611ec06115df565b5b611ecd85828601611980565b925050602083013567ffffffffffffffff811115611eee57611eed6115df565b5b611efa85828601611980565b9150509250929050565b6000608083016000830151611f1c6000860182611a84565b5060208301518482036020860152611f348282611ad9565b91505060408301518482036040860152611f4e8282611ad9565b9150506060830151611f636060860182611b1c565b508091505092915050565b60006020820190508181036000830152611f888184611f04565b905092915050565b600081905092915050565b6000611fa682611a93565b611fb08185611f90565b9350611fc0818560208601611aaf565b80840191505092915050565b6000611fd88284611f9b565b915081905092915050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b6000600282049050600182168061202a57607f821691505b60208210810361203d5761203c611fe3565b5b50919050565b60008190508160005260206000209050919050565b60006020601f8301049050919050565b600082821b905092915050565b6000600883026120a57fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff82612068565b6120af8683612068565b95508019841693508086168417925050509392505050565b6000819050919050565b60006120ec6120e76120e284611b12565b6120c7565b611b12565b9050919050565b6000819050919050565b612106836120d1565b61211a612112826120f3565b848454612075565b825550505050565b600090565b61212f612122565b61213a8184846120fd565b505050565b5b8181101561215e57612153600082612127565b600181019050612140565b5050565b601f8211156121a35761217481612043565b61217d84612058565b8101602085101561218c578190505b6121a061219885612058565b83018261213f565b50505b505050565b600082821c905092915050565b60006121c6600019846008026121a8565b1980831691505092915050565b60006121df83836121b5565b9150826002028217905092915050565b6121f882611a93565b67ffffffffffffffff811115612211576122106115ff565b5b61221b8254612012565b612226828285612162565b600060209050601f8311600181146122595760008415612247578287015190505b61225185826121d3565b8655506122b9565b601f19841661226786612043565b60005b8281101561228f5784890151825560018201915060208501945060208101905061226a565b868310156122ac57848901516122a8601f8916826121b5565b8355505b6001600288020188555050505b505050505050565b600082825260208201905092915050565b60006122dd82611a93565b6122e781856122c1565b93506122f7818560208601611aaf565b612300816115ee565b840191505092915050565b6000606082019050818103600083015261232581866122d2565b9050818103602083015261233981856122d2565b9050818103604083015261234d81846122d2565b9050949350505050565b7f5265636f726420646f6573206e6f742065786973740000000000000000000000600082015250565b600061238d6015836122c1565b915061239882612357565b602082019050919050565b600060208201905081810360008301526123bc81612380565b9050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052603260045260246000fd5b6000602082019050818103600083015261240c81846122d2565b905092915050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b600061244e82611b12565b91507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff82036124805761247f612414565b5b600182019050919050565b7f41636365737320616c7265616479206578697374730000000000000000000000600082015250565b60006124c16015836122c1565b91506124cc8261248b565b602082019050919050565b600060208201905081810360008301526124f0816124b4565b9050919050565b600082825260208201905092915050565b600061251382611cc2565b61251d81856124f7565b93508360208202850161252f85611cde565b8060005b8581101561256b578484038952815161254c8582611cee565b945061255783611d02565b925060208a01995050600181019050612533565b50829750879550505050505092915050565b6000604082019050818103600083015261259781856122d2565b905081810360208301526125ab8184612508565b90509392505050565b7f4d69736d617463686564206172726179206c656e677468730000000000000000600082015250565b60006125ea6018836122c1565b91506125f5826125b4565b602082019050919050565b60006020820190508181036000830152612619816125dd565b9050919050565b6000602082019050818103600083015261263a8184612508565b90509291505056fea2646970667358221220fdc8360985beeb8c197e91ef9a810c964fe1ab8b61a5dafc1e30571e89cbc30264736f6c63430008110033";

    public static final String FUNC_ACCESSEXISTS = "accessExists";

    public static final String FUNC_CREATERECORD = "createRecord";

    public static final String FUNC_CREATERECORDS = "createRecords";

    public static final String FUNC_DELETERECORD = "deleteRecord";

    public static final String FUNC_LOGACCESS = "logAccess";

    public static final String FUNC_LOGBADACTION = "logBadAction";

    public static final String FUNC_READACCESSES = "readAccesses";

    public static final String FUNC_READRECORDTX = "readRecordTx";

    public static final String FUNC_READRECORDS = "readRecords";

    public static final String FUNC_READRECORDSTX = "readRecordsTx";

    public static final String FUNC_RECORDEXISTS = "recordExists";

    public static final Event ACCESSLOGGED_EVENT = new Event("AccessLogged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Address>(true) {}, new TypeReference<DynamicArray<Utf8String>>() {}));
    ;

    public static final Event BADACTIONLOGGED_EVENT = new Event("BadActionLogged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event RECORDCREATED_EVENT = new Event("RecordCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event RECORDDELETED_EVENT = new Event("RecordDeleted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event RECORDSCREATED_EVENT = new Event("RecordsCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Utf8String>>() {}, new TypeReference<Address>(true) {}));
    ;

    @Deprecated
    protected MedskyContract(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected MedskyContract(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected MedskyContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected MedskyContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<AccessLoggedEventResponse> getAccessLoggedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ACCESSLOGGED_EVENT, transactionReceipt);
        ArrayList<AccessLoggedEventResponse> responses = new ArrayList<AccessLoggedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            AccessLoggedEventResponse typedResponse = new AccessLoggedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.requestor = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.accessId = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.recordIds = (List<String>) ((Array) eventValues.getNonIndexedValues().get(1)).getNativeValueCopy();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AccessLoggedEventResponse getAccessLoggedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ACCESSLOGGED_EVENT, log);
        AccessLoggedEventResponse typedResponse = new AccessLoggedEventResponse();
        typedResponse.log = log;
        typedResponse.requestor = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.accessId = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.recordIds = (List<String>) ((Array) eventValues.getNonIndexedValues().get(1)).getNativeValueCopy();
        return typedResponse;
    }

    public Flowable<AccessLoggedEventResponse> accessLoggedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAccessLoggedEventFromLog(log));
    }

    public Flowable<AccessLoggedEventResponse> accessLoggedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ACCESSLOGGED_EVENT));
        return accessLoggedEventFlowable(filter);
    }

    public static List<BadActionLoggedEventResponse> getBadActionLoggedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(BADACTIONLOGGED_EVENT, transactionReceipt);
        ArrayList<BadActionLoggedEventResponse> responses = new ArrayList<BadActionLoggedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            BadActionLoggedEventResponse typedResponse = new BadActionLoggedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.badActionId = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.identity = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.reason = (String) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static BadActionLoggedEventResponse getBadActionLoggedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(BADACTIONLOGGED_EVENT, log);
        BadActionLoggedEventResponse typedResponse = new BadActionLoggedEventResponse();
        typedResponse.log = log;
        typedResponse.badActionId = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.identity = (String) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.reason = (String) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<BadActionLoggedEventResponse> badActionLoggedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getBadActionLoggedEventFromLog(log));
    }

    public Flowable<BadActionLoggedEventResponse> badActionLoggedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BADACTIONLOGGED_EVENT));
        return badActionLoggedEventFlowable(filter);
    }

    public static List<RecordCreatedEventResponse> getRecordCreatedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RECORDCREATED_EVENT, transactionReceipt);
        ArrayList<RecordCreatedEventResponse> responses = new ArrayList<RecordCreatedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            RecordCreatedEventResponse typedResponse = new RecordCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.requestor = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.recordId = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RecordCreatedEventResponse getRecordCreatedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RECORDCREATED_EVENT, log);
        RecordCreatedEventResponse typedResponse = new RecordCreatedEventResponse();
        typedResponse.log = log;
        typedResponse.requestor = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.recordId = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RecordCreatedEventResponse> recordCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRecordCreatedEventFromLog(log));
    }

    public Flowable<RecordCreatedEventResponse> recordCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RECORDCREATED_EVENT));
        return recordCreatedEventFlowable(filter);
    }

    public static List<RecordDeletedEventResponse> getRecordDeletedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RECORDDELETED_EVENT, transactionReceipt);
        ArrayList<RecordDeletedEventResponse> responses = new ArrayList<RecordDeletedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            RecordDeletedEventResponse typedResponse = new RecordDeletedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.requestor = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.recordId = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RecordDeletedEventResponse getRecordDeletedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RECORDDELETED_EVENT, log);
        RecordDeletedEventResponse typedResponse = new RecordDeletedEventResponse();
        typedResponse.log = log;
        typedResponse.requestor = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.recordId = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RecordDeletedEventResponse> recordDeletedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRecordDeletedEventFromLog(log));
    }

    public Flowable<RecordDeletedEventResponse> recordDeletedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RECORDDELETED_EVENT));
        return recordDeletedEventFlowable(filter);
    }

    public static List<RecordsCreatedEventResponse> getRecordsCreatedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RECORDSCREATED_EVENT, transactionReceipt);
        ArrayList<RecordsCreatedEventResponse> responses = new ArrayList<RecordsCreatedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            RecordsCreatedEventResponse typedResponse = new RecordsCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.requestor = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.recordIds = (List<String>) ((Array) eventValues.getNonIndexedValues().get(0)).getNativeValueCopy();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RecordsCreatedEventResponse getRecordsCreatedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RECORDSCREATED_EVENT, log);
        RecordsCreatedEventResponse typedResponse = new RecordsCreatedEventResponse();
        typedResponse.log = log;
        typedResponse.requestor = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.recordIds = (List<String>) ((Array) eventValues.getNonIndexedValues().get(0)).getNativeValueCopy();
        return typedResponse;
    }

    public Flowable<RecordsCreatedEventResponse> recordsCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRecordsCreatedEventFromLog(log));
    }

    public Flowable<RecordsCreatedEventResponse> recordsCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RECORDSCREATED_EVENT));
        return recordsCreatedEventFlowable(filter);
    }

    public RemoteFunctionCall<Boolean> accessExists(String accessId) {
        final Function function = new Function(FUNC_ACCESSEXISTS, 
                Arrays.<Type>asList(new Utf8String(accessId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> createRecord(String recordId, String hash) {
        final Function function = new Function(
                FUNC_CREATERECORD, 
                Arrays.<Type>asList(new Utf8String(recordId),
                new Utf8String(hash)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> createRecords(List<String> recordIds, List<String> hashes) {
        final Function function = new Function(
                FUNC_CREATERECORDS, 
                Arrays.<Type>asList(new DynamicArray<Utf8String>(
                        Utf8String.class,
                        org.web3j.abi.Utils.typeMap(recordIds, Utf8String.class)),
                new DynamicArray<Utf8String>(
                        Utf8String.class,
                        org.web3j.abi.Utils.typeMap(hashes, Utf8String.class))),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> deleteRecord(String recordId, String actionId) {
        final Function function = new Function(
                FUNC_DELETERECORD, 
                Arrays.<Type>asList(new Utf8String(recordId),
                new Utf8String(actionId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> logAccess(List<String> recordIds, String accessId) {
        final Function function = new Function(
                FUNC_LOGACCESS, 
                Arrays.<Type>asList(new DynamicArray<Utf8String>(
                        Utf8String.class,
                        org.web3j.abi.Utils.typeMap(recordIds, Utf8String.class)),
                new Utf8String(accessId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> logBadAction(String badActionId, String identity, String reason) {
        final Function function = new Function(
                FUNC_LOGBADACTION, 
                Arrays.<Type>asList(new Utf8String(badActionId),
                new Utf8String(identity),
                new Utf8String(reason)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<List> readAccesses(List<String> accessIds) {
        final Function function = new Function(FUNC_READACCESSES, 
                Arrays.<Type>asList(new DynamicArray<Utf8String>(
                        Utf8String.class,
                        org.web3j.abi.Utils.typeMap(accessIds, Utf8String.class))),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Access>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> readRecordTx(String recordId, String accessId) {
        final Function function = new Function(
                FUNC_READRECORDTX, 
                Arrays.<Type>asList(new Utf8String(recordId),
                new Utf8String(accessId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<List> readRecords(List<String> recordIds) {
        final Function function = new Function(FUNC_READRECORDS, 
                Arrays.<Type>asList(new DynamicArray<Utf8String>(
                        Utf8String.class,
                        org.web3j.abi.Utils.typeMap(recordIds, Utf8String.class))),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Record>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> readRecordsTx(List<String> recordIds, String accessId) {
        final Function function = new Function(
                FUNC_READRECORDSTX, 
                Arrays.<Type>asList(new DynamicArray<Utf8String>(
                        Utf8String.class,
                        org.web3j.abi.Utils.typeMap(recordIds, Utf8String.class)),
                new Utf8String(accessId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> recordExists(String recordId) {
        final Function function = new Function(FUNC_RECORDEXISTS, 
                Arrays.<Type>asList(new Utf8String(recordId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    @Deprecated
    public static MedskyContract load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new MedskyContract(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static MedskyContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new MedskyContract(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static MedskyContract load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new MedskyContract(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static MedskyContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new MedskyContract(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<MedskyContract> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(MedskyContract.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<MedskyContract> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(MedskyContract.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<MedskyContract> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(MedskyContract.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<MedskyContract> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(MedskyContract.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class Access extends DynamicStruct {
        public String requestor;

        public List<String> recordIDs;

        public BigInteger timestamp;

        public Access(String requestor, List<String> recordIDs, BigInteger timestamp) {
            super(new Address(160, requestor),
                    new DynamicArray<Utf8String>(
                            Utf8String.class,
                            org.web3j.abi.Utils.typeMap(recordIDs, Utf8String.class)),
                    new Uint256(timestamp));
            this.requestor = requestor;
            this.recordIDs = recordIDs;
            this.timestamp = timestamp;
        }

        public Access(Address requestor, DynamicArray<Utf8String> recordIDs, Uint256 timestamp) {
            super(requestor, recordIDs, timestamp);
            this.requestor = requestor.getValue();
            this.recordIDs = recordIDs.getValue().stream().map(v -> v.getValue()).collect(Collectors.toList());
            this.timestamp = timestamp.getValue();
        }
    }

    public static class Record extends DynamicStruct {
        public String requestor;

        public String hash;

        public String version;

        public BigInteger lastUpdated;

        public Record(String requestor, String hash, String version, BigInteger lastUpdated) {
            super(new Address(160, requestor),
                    new Utf8String(hash),
                    new Utf8String(version),
                    new Uint256(lastUpdated));
            this.requestor = requestor;
            this.hash = hash;
            this.version = version;
            this.lastUpdated = lastUpdated;
        }

        public Record(Address requestor, Utf8String hash, Utf8String version, Uint256 lastUpdated) {
            super(requestor, hash, version, lastUpdated);
            this.requestor = requestor.getValue();
            this.hash = hash.getValue();
            this.version = version.getValue();
            this.lastUpdated = lastUpdated.getValue();
        }
    }

    public static class AccessLoggedEventResponse extends BaseEventResponse {
        public String requestor;

        public String accessId;

        public List<String> recordIds;
    }

    public static class BadActionLoggedEventResponse extends BaseEventResponse {
        public String badActionId;

        public String identity;

        public String reason;
    }

    public static class RecordCreatedEventResponse extends BaseEventResponse {
        public String requestor;

        public String recordId;
    }

    public static class RecordDeletedEventResponse extends BaseEventResponse {
        public String requestor;

        public String recordId;
    }

    public static class RecordsCreatedEventResponse extends BaseEventResponse {
        public String requestor;

        public List<String> recordIds;
    }
}
