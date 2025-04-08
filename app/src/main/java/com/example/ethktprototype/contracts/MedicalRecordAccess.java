package com.example.ethktprototype.contracts;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tuples.generated.Tuple4;
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
public class MedicalRecordAccess extends Contract {
    public static final String BINARY = "608060405260006004553480156200001657600080fd5b50336000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550620000676200006d60201b60201c565b620008b8565b60005b6064811015620002c7576000816040516020016200008f919062000354565b6040516020818303038152906040528051906020012060001c9050600082604051602001620000bf9190620003ce565b6040516020818303038152906040528051906020012060001c9050600080600285620000ec919062000427565b146200012e576040518060400160405280600581526020017f582d52617900000000000000000000000000000000000000000000000000000081525062000165565b6040518060400160405280600381526020017f4d524900000000000000000000000000000000000000000000000000000000008152505b9050600360405180608001604052808573ffffffffffffffffffffffffffffffffffffffff1681526020018473ffffffffffffffffffffffffffffffffffffffff1681526020018381526020016201518087620001c391906200048e565b42620001d09190620004d9565b815250908060018154018082558091505060019003906000526020600020906004020160009091909190915060008201518160000160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060208201518160010160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506040820151816002019081620002a1919062000784565b506060820151816003015550505050508080620002be906200086b565b91505062000070565b50565b600081905092915050565b7f646f63746f720000000000000000000000000000000000000000000000000000600082015250565b60006200030d600683620002ca565b91506200031a82620002d5565b600682019050919050565b6000819050919050565b6000819050919050565b6200034e620003488262000325565b6200032f565b82525050565b60006200036182620002fe565b91506200036f828462000339565b60208201915081905092915050565b7f70617469656e7400000000000000000000000000000000000000000000000000600082015250565b6000620003b6600783620002ca565b9150620003c3826200037e565b600782019050919050565b6000620003db82620003a7565b9150620003e9828462000339565b60208201915081905092915050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601260045260246000fd5b6000620004348262000325565b9150620004418362000325565b925082620004545762000453620003f8565b5b828206905092915050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b60006200049b8262000325565b9150620004a88362000325565b9250828202620004b88162000325565b91508282048414831517620004d257620004d16200045f565b5b5092915050565b6000620004e68262000325565b9150620004f38362000325565b92508282039050818111156200050e576200050d6200045f565b5b92915050565b600081519050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b600060028204905060018216806200059657607f821691505b602082108103620005ac57620005ab6200054e565b5b50919050565b60008190508160005260206000209050919050565b60006020601f8301049050919050565b600082821b905092915050565b600060088302620006167fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff82620005d7565b620006228683620005d7565b95508019841693508086168417925050509392505050565b6000819050919050565b6000620006656200065f620006598462000325565b6200063a565b62000325565b9050919050565b6000819050919050565b620006818362000644565b6200069962000690826200066c565b848454620005e4565b825550505050565b600090565b620006b0620006a1565b620006bd81848462000676565b505050565b5b81811015620006e557620006d9600082620006a6565b600181019050620006c3565b5050565b601f8211156200073457620006fe81620005b2565b6200070984620005c7565b8101602085101562000719578190505b620007316200072885620005c7565b830182620006c2565b50505b505050565b600082821c905092915050565b6000620007596000198460080262000739565b1980831691505092915050565b600062000774838362000746565b9150826002028217905092915050565b6200078f8262000514565b67ffffffffffffffff811115620007ab57620007aa6200051f565b5b620007b782546200057d565b620007c4828285620006e9565b600060209050601f831160018114620007fc5760008415620007e7578287015190505b620007f3858262000766565b86555062000863565b601f1984166200080c86620005b2565b60005b8281101562000836578489015182556001820191506020850194506020810190506200080f565b8683101562000856578489015162000852601f89168262000746565b8355505b6001600288020188555050505b505050505050565b6000620008788262000325565b91507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8203620008ad57620008ac6200045f565b5b600182019050919050565b61226a80620008c86000396000f3fe608060405234801561001057600080fd5b50600436106101005760003560e01c8063b203504e11610097578063de6de72111610066578063de6de7211461029e578063deb59566146102a8578063e2f99c7e146102c6578063fde657b1146102f657610100565b8063b203504e14610218578063b53baf6614610234578063d3cb562b14610250578063db979bf81461028057610100565b806359741ba0116100d357806359741ba01461019f578063720bf0ef146101d25780638da5cb5b146101f05780639b9937101461020e57610100565b8063019971aa1461010557806310ee1b221461012157806313bd20e21461013d57806344d2b4e71461016d575b600080fd5b61011f600480360381019061011a919061160e565b610314565b005b61013b6004803603810190610136919061166a565b610569565b005b6101576004803603810190610152919061160e565b6107bb565b60405161016491906116ce565b60405180910390f35b6101876004803603810190610182919061171f565b6108e7565b604051610196939291906117eb565b60405180910390f35b6101b960048036038101906101b4919061184e565b61093e565b6040516101c994939291906118fa565b60405180910390f35b6101da610a46565b6040516101e79190611946565b60405180910390f35b6101f8610a4b565b6040516102059190611961565b60405180910390f35b610216610a6f565b005b610232600480360381019061022d919061197c565b610b07565b005b61024e6004803603810190610249919061160e565b610d29565b005b61026a600480360381019061026591906119eb565b610eff565b6040516102779190611961565b60405180910390f35b610288610f63565b6040516102959190611bd4565b60405180910390f35b6102a661110a565b005b6102b0611153565b6040516102bd9190611946565b60405180910390f35b6102e060048036038101906102db919061160e565b611159565b6040516102ed9190611bf6565b60405180910390f35b6102fe6111b4565b60405161030b9190611bd4565b60405180910390f35b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16146103a2576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161039990611c5d565b60405180910390fd5b600082826040516020016103b7929190611d01565b6040516020818303038152906040528051906020012090506000600160008381526020019081526020016000209050600060028111156103fa576103f9611774565b5b8160020160009054906101000a900460ff16600281111561041e5761041d611774565b5b1461045e576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161045590611d75565b60405180910390fd5b60018160020160006101000a81548160ff0219169083600281111561048657610485611774565b5b021790555060028460405161049b9190611d95565b9081526020016040518091039020839080600181540180825580915050600190039060005260206000200160009091909190916101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508273ffffffffffffffffffffffffffffffffffffffff168460405161052f9190611d95565b60405180910390207f8f8e871778673452dc0de9e1c7af36f496e6ef106308b847488abf7d492b57c760405160405180910390a350505050565b6000813360405160200161057e929190611d01565b6040516020818303038152906040528051906020012090506000600160008381526020019081526020016000209050600060028111156105c1576105c0611774565b5b8160020160009054906101000a900460ff1660028111156105e5576105e4611774565b5b1480610638575060006002811115610600576105ff611774565b5b600281111561061257610611611774565b5b8160020160009054906101000a900460ff16600281111561063657610635611774565b5b145b610677576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161066e90611df8565b60405180910390fd5b60405180606001604052803373ffffffffffffffffffffffffffffffffffffffff168152602001428152602001600060028111156106b8576106b7611774565b5b8152506001600084815260200190815260200160002060008201518160000160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506020820151816001015560408201518160020160006101000a81548160ff0219169083600281111561074a57610749611774565b5b02179055509050503373ffffffffffffffffffffffffffffffffffffffff16836040516107779190611d95565b60405180910390207f049be16051612b02110715a3302374f97570623d59144463390959db9054939b426040516107ae9190611946565b60405180910390a3505050565b6000806002846040516107ce9190611d95565b908152602001604051809103902080548060200260200160405190810160405280929190818152602001828054801561085c57602002820191906000526020600020905b8160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019060010190808311610812575b5050505050905060005b81518110156108da578373ffffffffffffffffffffffffffffffffffffffff1682828151811061089957610898611e18565b5b602002602001015173ffffffffffffffffffffffffffffffffffffffff16036108c7576001925050506108e1565b80806108d290611e76565b915050610866565b5060009150505b92915050565b60016020528060005260406000206000915090508060000160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16908060010154908060020160009054906101000a900460ff16905083565b6003818154811061094e57600080fd5b90600052602060002090600402016000915090508060000160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16908060010160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16908060020180546109bd90611eed565b80601f01602080910402602001604051908101604052809291908181526020018280546109e990611eed565b8015610a365780601f10610a0b57610100808354040283529160200191610a36565b820191906000526020600020905b815481529060010190602001808311610a1957829003601f168201915b5050505050908060030154905084565b600381565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614610afd576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610af490611c5d565b60405180910390fd5b6000600481905550565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614610b95576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610b8c90611c5d565b60405180910390fd5b600360405180608001604052808573ffffffffffffffffffffffffffffffffffffffff1681526020018473ffffffffffffffffffffffffffffffffffffffff16815260200183815260200142815250908060018154018082558091505060019003906000526020600020906004020160009091909190915060008201518160000160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060208201518160010160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506040820151816002019081610cb091906120ca565b506060820151816003015550508173ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff167fa8c88859ce36fd5f0d1f642ec7fe1799a59641817c2cace9eab675abb5f717858342604051610d1c92919061219c565b60405180910390a3505050565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614610db7576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610dae90611c5d565b60405180910390fd5b60008282604051602001610dcc929190611d01565b604051602081830303815290604052805190602001209050600060016000838152602001908152602001600020905060006002811115610e0f57610e0e611774565b5b8160020160009054906101000a900460ff166002811115610e3357610e32611774565b5b14610e73576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610e6a90611d75565b60405180910390fd5b60028160020160006101000a81548160ff02191690836002811115610e9b57610e9a611774565b5b02179055508273ffffffffffffffffffffffffffffffffffffffff1684604051610ec59190611d95565b60405180910390207fe64c0348a1cbe72872296d322988160fb830d4538412fd7d3d956692eee6881660405160405180910390a350505050565b6002828051602081018201805184825260208301602085012081835280955050505050508181548110610f3157600080fd5b906000526020600020016000915091509054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b60606003805480602002602001604051908101604052809291908181526020016000905b8282101561110157838290600052602060002090600402016040518060800160405290816000820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020016001820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200160028201805461106690611eed565b80601f016020809104026020016040519081016040528092919081815260200182805461109290611eed565b80156110df5780601f106110b4576101008083540402835291602001916110df565b820191906000526020600020905b8154815290600101906020018083116110c257829003601f168201915b5050505050815260200160038201548152505081526020019060010190610f87565b50505050905090565b600060045460038054905061111f91906121cc565b9050600060038210611132576003611134565b815b905080600460008282546111489190612200565b925050819055505050565b60045481565b600080838360405160200161116f929190611d01565b6040516020818303038152906040528051906020012090506001600082815260200190815260200160002060020160009054906101000a900460ff1691505092915050565b606060006004546003805490506111cb91906121cc565b90506000600382106111de5760036111e0565b815b905060008167ffffffffffffffff8111156111fe576111fd611485565b5b60405190808252806020026020018201604052801561123757816020015b611224611402565b81526020019060019003908161121c5790505b50905060005b828110156113f8576003816004546112559190612200565b8154811061126657611265611e18565b5b90600052602060002090600402016040518060800160405290816000820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020016001820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200160028201805461133b90611eed565b80601f016020809104026020016040519081016040528092919081815260200182805461136790611eed565b80156113b45780601f10611389576101008083540402835291602001916113b4565b820191906000526020600020905b81548152906001019060200180831161139757829003601f168201915b505050505081526020016003820154815250508282815181106113da576113d9611e18565b5b602002602001018190525080806113f090611e76565b91505061123d565b5080935050505090565b6040518060800160405280600073ffffffffffffffffffffffffffffffffffffffff168152602001600073ffffffffffffffffffffffffffffffffffffffff16815260200160608152602001600081525090565b6000604051905090565b600080fd5b600080fd5b600080fd5b600080fd5b6000601f19601f8301169050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b6114bd82611474565b810181811067ffffffffffffffff821117156114dc576114db611485565b5b80604052505050565b60006114ef611456565b90506114fb82826114b4565b919050565b600067ffffffffffffffff82111561151b5761151a611485565b5b61152482611474565b9050602081019050919050565b82818337600083830152505050565b600061155361154e84611500565b6114e5565b90508281526020810184848401111561156f5761156e61146f565b5b61157a848285611531565b509392505050565b600082601f8301126115975761159661146a565b5b81356115a7848260208601611540565b91505092915050565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b60006115db826115b0565b9050919050565b6115eb816115d0565b81146115f657600080fd5b50565b600081359050611608816115e2565b92915050565b6000806040838503121561162557611624611460565b5b600083013567ffffffffffffffff81111561164357611642611465565b5b61164f85828601611582565b9250506020611660858286016115f9565b9150509250929050565b6000602082840312156116805761167f611460565b5b600082013567ffffffffffffffff81111561169e5761169d611465565b5b6116aa84828501611582565b91505092915050565b60008115159050919050565b6116c8816116b3565b82525050565b60006020820190506116e360008301846116bf565b92915050565b6000819050919050565b6116fc816116e9565b811461170757600080fd5b50565b600081359050611719816116f3565b92915050565b60006020828403121561173557611734611460565b5b60006117438482850161170a565b91505092915050565b611755816115d0565b82525050565b6000819050919050565b61176e8161175b565b82525050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602160045260246000fd5b600381106117b4576117b3611774565b5b50565b60008190506117c5826117a3565b919050565b60006117d5826117b7565b9050919050565b6117e5816117ca565b82525050565b6000606082019050611800600083018661174c565b61180d6020830185611765565b61181a60408301846117dc565b949350505050565b61182b8161175b565b811461183657600080fd5b50565b60008135905061184881611822565b92915050565b60006020828403121561186457611863611460565b5b600061187284828501611839565b91505092915050565b600081519050919050565b600082825260208201905092915050565b60005b838110156118b557808201518184015260208101905061189a565b60008484015250505050565b60006118cc8261187b565b6118d68185611886565b93506118e6818560208601611897565b6118ef81611474565b840191505092915050565b600060808201905061190f600083018761174c565b61191c602083018661174c565b818103604083015261192e81856118c1565b905061193d6060830184611765565b95945050505050565b600060208201905061195b6000830184611765565b92915050565b6000602082019050611976600083018461174c565b92915050565b60008060006060848603121561199557611994611460565b5b60006119a3868287016115f9565b93505060206119b4868287016115f9565b925050604084013567ffffffffffffffff8111156119d5576119d4611465565b5b6119e186828701611582565b9150509250925092565b60008060408385031215611a0257611a01611460565b5b600083013567ffffffffffffffff811115611a2057611a1f611465565b5b611a2c85828601611582565b9250506020611a3d85828601611839565b9150509250929050565b600081519050919050565b600082825260208201905092915050565b6000819050602082019050919050565b611a7c816115d0565b82525050565b600082825260208201905092915050565b6000611a9e8261187b565b611aa88185611a82565b9350611ab8818560208601611897565b611ac181611474565b840191505092915050565b611ad58161175b565b82525050565b6000608083016000830151611af36000860182611a73565b506020830151611b066020860182611a73565b5060408301518482036040860152611b1e8282611a93565b9150506060830151611b336060860182611acc565b508091505092915050565b6000611b4a8383611adb565b905092915050565b6000602082019050919050565b6000611b6a82611a47565b611b748185611a52565b935083602082028501611b8685611a63565b8060005b85811015611bc25784840389528151611ba38582611b3e565b9450611bae83611b52565b925060208a01995050600181019050611b8a565b50829750879550505050505092915050565b60006020820190508181036000830152611bee8184611b5f565b905092915050565b6000602082019050611c0b60008301846117dc565b92915050565b7f4f6e6c79206f776e65722063616e20706572666f726d20746869730000000000600082015250565b6000611c47601b83611886565b9150611c5282611c11565b602082019050919050565b60006020820190508181036000830152611c7681611c3a565b9050919050565b600081905092915050565b6000611c938261187b565b611c9d8185611c7d565b9350611cad818560208601611897565b80840191505092915050565b60008160601b9050919050565b6000611cd182611cb9565b9050919050565b6000611ce382611cc6565b9050919050565b611cfb611cf6826115d0565b611cd8565b82525050565b6000611d0d8285611c88565b9150611d198284611cea565b6014820191508190509392505050565b7f52657175657374206e6f742070656e64696e6700000000000000000000000000600082015250565b6000611d5f601383611886565b9150611d6a82611d29565b602082019050919050565b60006020820190508181036000830152611d8e81611d52565b9050919050565b6000611da18284611c88565b915081905092915050565b7f416c726561647920726571756573746564000000000000000000000000000000600082015250565b6000611de2601183611886565b9150611ded82611dac565b602082019050919050565b60006020820190508181036000830152611e1181611dd5565b9050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052603260045260246000fd5b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b6000611e818261175b565b91507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8203611eb357611eb2611e47565b5b600182019050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b60006002820490506001821680611f0557607f821691505b602082108103611f1857611f17611ebe565b5b50919050565b60008190508160005260206000209050919050565b60006020601f8301049050919050565b600082821b905092915050565b600060088302611f807fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff82611f43565b611f8a8683611f43565b95508019841693508086168417925050509392505050565b6000819050919050565b6000611fc7611fc2611fbd8461175b565b611fa2565b61175b565b9050919050565b6000819050919050565b611fe183611fac565b611ff5611fed82611fce565b848454611f50565b825550505050565b600090565b61200a611ffd565b612015818484611fd8565b505050565b5b818110156120395761202e600082612002565b60018101905061201b565b5050565b601f82111561207e5761204f81611f1e565b61205884611f33565b81016020851015612067578190505b61207b61207385611f33565b83018261201a565b50505b505050565b600082821c905092915050565b60006120a160001984600802612083565b1980831691505092915050565b60006120ba8383612090565b9150826002028217905092915050565b6120d38261187b565b67ffffffffffffffff8111156120ec576120eb611485565b5b6120f68254611eed565b61210182828561203d565b600060209050601f8311600181146121345760008415612122578287015190505b61212c85826120ae565b865550612194565b601f19841661214286611f1e565b60005b8281101561216a57848901518255600182019150602085019450602081019050612145565b868310156121875784890151612183601f891682612090565b8355505b6001600288020188555050505b505050505050565b600060408201905081810360008301526121b681856118c1565b90506121c56020830184611765565b9392505050565b60006121d78261175b565b91506121e28361175b565b92508282039050818111156121fa576121f9611e47565b5b92915050565b600061220b8261175b565b91506122168361175b565b925082820190508082111561222e5761222d611e47565b5b9291505056fea2646970667358221220cad8b2829caa2398960fd40b17ad25624afb60e3390e499dc22db30434b6ca9b64736f6c63430008110033";

    public static final String FUNC_SYNC_BATCH_SIZE = "SYNC_BATCH_SIZE";

    public static final String FUNC_ACCESSLOGS = "accessLogs";

    public static final String FUNC_ACCESSREQUESTS = "accessRequests";

    public static final String FUNC_APPROVEACCESS = "approveAccess";

    public static final String FUNC_DENYACCESS = "denyAccess";

    public static final String FUNC_GETACCESSLOGS = "getAccessLogs";

    public static final String FUNC_GETNEXTACCESSLOGS = "getNextAccessLogs";

    public static final String FUNC_GETREQUESTSTATUS = "getRequestStatus";

    public static final String FUNC_HASACCESS = "hasAccess";

    public static final String FUNC_LOGACCESS = "logAccess";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_PREVIEWNEXTACCESSLOGS = "previewNextAccessLogs";

    public static final String FUNC_RECORDACCESSLIST = "recordAccessList";

    public static final String FUNC_REQUESTACCESS = "requestAccess";

    public static final String FUNC_RESETSYNCPOINTER = "resetSyncPointer";

    public static final String FUNC_SYNCPOINTER = "syncPointer";

    public static final Event ACCESSAPPROVED_EVENT = new Event("AccessApproved", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event ACCESSDENIED_EVENT = new Event("AccessDenied", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event ACCESSREQUESTED_EVENT = new Event("AccessRequested", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event RECORDACCESSED_EVENT = new Event("RecordAccessed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected MedicalRecordAccess(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected MedicalRecordAccess(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected MedicalRecordAccess(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected MedicalRecordAccess(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<AccessApprovedEventResponse> getAccessApprovedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ACCESSAPPROVED_EVENT, transactionReceipt);
        ArrayList<AccessApprovedEventResponse> responses = new ArrayList<AccessApprovedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            AccessApprovedEventResponse typedResponse = new AccessApprovedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.recordId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.requester = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AccessApprovedEventResponse getAccessApprovedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ACCESSAPPROVED_EVENT, log);
        AccessApprovedEventResponse typedResponse = new AccessApprovedEventResponse();
        typedResponse.log = log;
        typedResponse.recordId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.requester = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<AccessApprovedEventResponse> accessApprovedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAccessApprovedEventFromLog(log));
    }

    public Flowable<AccessApprovedEventResponse> accessApprovedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ACCESSAPPROVED_EVENT));
        return accessApprovedEventFlowable(filter);
    }

    public static List<AccessDeniedEventResponse> getAccessDeniedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ACCESSDENIED_EVENT, transactionReceipt);
        ArrayList<AccessDeniedEventResponse> responses = new ArrayList<AccessDeniedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            AccessDeniedEventResponse typedResponse = new AccessDeniedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.recordId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.requester = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AccessDeniedEventResponse getAccessDeniedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ACCESSDENIED_EVENT, log);
        AccessDeniedEventResponse typedResponse = new AccessDeniedEventResponse();
        typedResponse.log = log;
        typedResponse.recordId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.requester = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<AccessDeniedEventResponse> accessDeniedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAccessDeniedEventFromLog(log));
    }

    public Flowable<AccessDeniedEventResponse> accessDeniedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ACCESSDENIED_EVENT));
        return accessDeniedEventFlowable(filter);
    }

    public static List<AccessRequestedEventResponse> getAccessRequestedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ACCESSREQUESTED_EVENT, transactionReceipt);
        ArrayList<AccessRequestedEventResponse> responses = new ArrayList<AccessRequestedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            AccessRequestedEventResponse typedResponse = new AccessRequestedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.recordId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.requester = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AccessRequestedEventResponse getAccessRequestedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ACCESSREQUESTED_EVENT, log);
        AccessRequestedEventResponse typedResponse = new AccessRequestedEventResponse();
        typedResponse.log = log;
        typedResponse.recordId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.requester = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<AccessRequestedEventResponse> accessRequestedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAccessRequestedEventFromLog(log));
    }

    public Flowable<AccessRequestedEventResponse> accessRequestedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ACCESSREQUESTED_EVENT));
        return accessRequestedEventFlowable(filter);
    }

    public static List<RecordAccessedEventResponse> getRecordAccessedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RECORDACCESSED_EVENT, transactionReceipt);
        ArrayList<RecordAccessedEventResponse> responses = new ArrayList<RecordAccessedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            RecordAccessedEventResponse typedResponse = new RecordAccessedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.doctor = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.patient = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.recordType = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RecordAccessedEventResponse getRecordAccessedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RECORDACCESSED_EVENT, log);
        RecordAccessedEventResponse typedResponse = new RecordAccessedEventResponse();
        typedResponse.log = log;
        typedResponse.doctor = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.patient = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.recordType = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<RecordAccessedEventResponse> recordAccessedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRecordAccessedEventFromLog(log));
    }

    public Flowable<RecordAccessedEventResponse> recordAccessedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RECORDACCESSED_EVENT));
        return recordAccessedEventFlowable(filter);
    }

    public RemoteFunctionCall<BigInteger> SYNC_BATCH_SIZE() {
        final Function function = new Function(FUNC_SYNC_BATCH_SIZE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple4<String, String, String, BigInteger>> accessLogs(BigInteger param0) {
        final Function function = new Function(FUNC_ACCESSLOGS, 
                Arrays.<Type>asList(new Uint256(param0)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple4<String, String, String, BigInteger>>(function,
                new Callable<Tuple4<String, String, String, BigInteger>>() {
                    @Override
                    public Tuple4<String, String, String, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<String, String, String, BigInteger>(
                                (String) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (String) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue());
                    }
                });
    }

    public RemoteFunctionCall<Tuple3<String, BigInteger, BigInteger>> accessRequests(byte[] param0) {
        final Function function = new Function(FUNC_ACCESSREQUESTS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint8>() {}));
        return new RemoteFunctionCall<Tuple3<String, BigInteger, BigInteger>>(function,
                new Callable<Tuple3<String, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple3<String, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<String, BigInteger, BigInteger>(
                                (String) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> approveAccess(String recordId, String requester) {
        final Function function = new Function(
                FUNC_APPROVEACCESS, 
                Arrays.<Type>asList(new Utf8String(recordId),
                new Address(160, requester)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> denyAccess(String recordId, String requester) {
        final Function function = new Function(
                FUNC_DENYACCESS, 
                Arrays.<Type>asList(new Utf8String(recordId),
                new Address(160, requester)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<List> getAccessLogs() {
        final Function function = new Function(FUNC_GETACCESSLOGS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<MedicalAccessLog>>() {}));
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

    public RemoteFunctionCall<TransactionReceipt> getNextAccessLogs() {
        final Function function = new Function(
                FUNC_GETNEXTACCESSLOGS, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> getRequestStatus(String recordId, String requester) {
        final Function function = new Function(FUNC_GETREQUESTSTATUS, 
                Arrays.<Type>asList(new Utf8String(recordId),
                new Address(160, requester)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Boolean> hasAccess(String recordId, String user) {
        final Function function = new Function(FUNC_HASACCESS, 
                Arrays.<Type>asList(new Utf8String(recordId),
                new Address(160, user)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> logAccess(String doctor, String patient, String recordType) {
        final Function function = new Function(
                FUNC_LOGACCESS, 
                Arrays.<Type>asList(new Address(160, doctor),
                new Address(160, patient),
                new Utf8String(recordType)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<List> previewNextAccessLogs() {
        final Function function = new Function(FUNC_PREVIEWNEXTACCESSLOGS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<MedicalAccessLog>>() {}));
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

    public RemoteFunctionCall<String> recordAccessList(String param0, BigInteger param1) {
        final Function function = new Function(FUNC_RECORDACCESSLIST, 
                Arrays.<Type>asList(new Utf8String(param0),
                new Uint256(param1)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> requestAccess(String recordId) {
        final Function function = new Function(
                FUNC_REQUESTACCESS, 
                Arrays.<Type>asList(new Utf8String(recordId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> resetSyncPointer() {
        final Function function = new Function(
                FUNC_RESETSYNCPOINTER, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> syncPointer() {
        final Function function = new Function(FUNC_SYNCPOINTER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    @Deprecated
    public static MedicalRecordAccess load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new MedicalRecordAccess(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static MedicalRecordAccess load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new MedicalRecordAccess(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static MedicalRecordAccess load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new MedicalRecordAccess(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static MedicalRecordAccess load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new MedicalRecordAccess(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<MedicalRecordAccess> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(MedicalRecordAccess.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<MedicalRecordAccess> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(MedicalRecordAccess.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<MedicalRecordAccess> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(MedicalRecordAccess.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<MedicalRecordAccess> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(MedicalRecordAccess.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class MedicalAccessLog extends DynamicStruct {
        public String doctor;

        public String patient;

        public String recordType;

        public BigInteger timestamp;

        public MedicalAccessLog(String doctor, String patient, String recordType, BigInteger timestamp) {
            super(new Address(160, doctor),
                    new Address(160, patient),
                    new Utf8String(recordType),
                    new Uint256(timestamp));
            this.doctor = doctor;
            this.patient = patient;
            this.recordType = recordType;
            this.timestamp = timestamp;
        }

        public MedicalAccessLog(Address doctor, Address patient, Utf8String recordType, Uint256 timestamp) {
            super(doctor, patient, recordType, timestamp);
            this.doctor = doctor.getValue();
            this.patient = patient.getValue();
            this.recordType = recordType.getValue();
            this.timestamp = timestamp.getValue();
        }
    }

    public static class AccessApprovedEventResponse extends BaseEventResponse {
        public byte[] recordId;

        public String requester;
    }

    public static class AccessDeniedEventResponse extends BaseEventResponse {
        public byte[] recordId;

        public String requester;
    }

    public static class AccessRequestedEventResponse extends BaseEventResponse {
        public byte[] recordId;

        public String requester;

        public BigInteger timestamp;
    }

    public static class RecordAccessedEventResponse extends BaseEventResponse {
        public String doctor;

        public String patient;

        public String recordType;

        public BigInteger timestamp;
    }
}
