package com.example.ethktprototype.contracts;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Array;
import org.web3j.abi.datatypes.DynamicArray;
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
 * <a href="https://github.com/hyperledger-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.6.3.
 */
@SuppressWarnings("rawtypes")
public class RecordAccessContract extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b50610a80806100206000396000f3fe608060405234801561001057600080fd5b506004361061002b5760003560e01c8063d517521814610030575b600080fd5b61004a600480360381019061004591906104b8565b61004c565b005b600060405180606001604052803373ffffffffffffffffffffffffffffffffffffffff1681526020018481526020014281525090508060008360405161009291906105a1565b908152602001604051809103902060008201518160000160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550602082015181600101908051906020019061010392919061019e565b50604082015181600201559050506001808360405161012291906105a1565b908152602001604051809103902060006101000a81548160ff0219169083151502179055503373ffffffffffffffffffffffffffffffffffffffff167fd7c62831bdbdb05061c95ce5304350737b1127dde22936478d38aa562226da2e83854260405161019193929190610727565b60405180910390a2505050565b8280548282559060005260206000209081019282156101e6579160200282015b828111156101e55782518290816101d59190610978565b50916020019190600101906101be565b5b5090506101f391906101f7565b5090565b5b80821115610217576000818161020e919061021b565b506001016101f8565b5090565b5080546102279061079b565b6000825580601f106102395750610258565b601f016020900490600052602060002090810190610257919061025b565b5b50565b5b8082111561027457600081600090555060010161025c565b5090565b6000604051905090565b600080fd5b600080fd5b600080fd5b6000601f19601f8301169050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b6102da82610291565b810181811067ffffffffffffffff821117156102f9576102f86102a2565b5b80604052505050565b600061030c610278565b905061031882826102d1565b919050565b600067ffffffffffffffff821115610338576103376102a2565b5b602082029050602081019050919050565b600080fd5b600080fd5b600067ffffffffffffffff82111561036e5761036d6102a2565b5b61037782610291565b9050602081019050919050565b82818337600083830152505050565b60006103a66103a184610353565b610302565b9050828152602081018484840111156103c2576103c161034e565b5b6103cd848285610384565b509392505050565b600082601f8301126103ea576103e961028c565b5b81356103fa848260208601610393565b91505092915050565b60006104166104118461031d565b610302565b9050808382526020820190506020840283018581111561043957610438610349565b5b835b8181101561048057803567ffffffffffffffff81111561045e5761045d61028c565b5b80860161046b89826103d5565b8552602085019450505060208101905061043b565b5050509392505050565b600082601f83011261049f5761049e61028c565b5b81356104af848260208601610403565b91505092915050565b600080604083850312156104cf576104ce610282565b5b600083013567ffffffffffffffff8111156104ed576104ec610287565b5b6104f98582860161048a565b925050602083013567ffffffffffffffff81111561051a57610519610287565b5b610526858286016103d5565b9150509250929050565b600081519050919050565b600081905092915050565b60005b83811015610564578082015181840152602081019050610549565b60008484015250505050565b600061057b82610530565b610585818561053b565b9350610595818560208601610546565b80840191505092915050565b60006105ad8284610570565b915081905092915050565b600082825260208201905092915050565b60006105d482610530565b6105de81856105b8565b93506105ee818560208601610546565b6105f781610291565b840191505092915050565b600081519050919050565b600082825260208201905092915050565b6000819050602082019050919050565b600082825260208201905092915050565b600061064a82610530565b610654818561062e565b9350610664818560208601610546565b61066d81610291565b840191505092915050565b6000610684838361063f565b905092915050565b6000602082019050919050565b60006106a482610602565b6106ae818561060d565b9350836020820285016106c08561061e565b8060005b858110156106fc57848403895281516106dd8582610678565b94506106e88361068c565b925060208a019950506001810190506106c4565b50829750879550505050505092915050565b6000819050919050565b6107218161070e565b82525050565b6000606082019050818103600083015261074181866105c9565b905081810360208301526107558185610699565b90506107646040830184610718565b949350505050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b600060028204905060018216806107b357607f821691505b6020821081036107c6576107c561076c565b5b50919050565b60008190508160005260206000209050919050565b60006020601f8301049050919050565b600082821b905092915050565b60006008830261082e7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff826107f1565b61083886836107f1565b95508019841693508086168417925050509392505050565b6000819050919050565b600061087561087061086b8461070e565b610850565b61070e565b9050919050565b6000819050919050565b61088f8361085a565b6108a361089b8261087c565b8484546107fe565b825550505050565b600090565b6108b86108ab565b6108c3818484610886565b505050565b5b818110156108e7576108dc6000826108b0565b6001810190506108c9565b5050565b601f82111561092c576108fd816107cc565b610906846107e1565b81016020851015610915578190505b610929610921856107e1565b8301826108c8565b50505b505050565b600082821c905092915050565b600061094f60001984600802610931565b1980831691505092915050565b6000610968838361093e565b9150826002028217905092915050565b61098182610530565b67ffffffffffffffff81111561099a576109996102a2565b5b6109a4825461079b565b6109af8282856108eb565b600060209050601f8311600181146109e257600084156109d0578287015190505b6109da858261095c565b865550610a42565b601f1984166109f0866107cc565b60005b82811015610a18578489015182556001820191506020850194506020810190506109f3565b86831015610a355784890151610a31601f89168261093e565b8355505b6001600288020188555050505b50505050505056fea26469706673582212204dcbc7f605b3dfc954b1fc816f078e88ee1a60586ad5982d2b654cf8f210067e64736f6c63430008110033";

    private static String librariesLinkedBinary;

    public static final String FUNC_LOGACCESS = "logAccess";

    public static final Event RECORDACCESSED_EVENT = new Event("RecordAccessed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Address>(true) {}, new TypeReference<DynamicArray<Utf8String>>() {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected RecordAccessContract(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected RecordAccessContract(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected RecordAccessContract(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected RecordAccessContract(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<RecordAccessedEventResponse> getRecordAccessedEvents(
            TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RECORDACCESSED_EVENT, transactionReceipt);
        ArrayList<RecordAccessedEventResponse> responses = new ArrayList<RecordAccessedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            RecordAccessedEventResponse typedResponse = new RecordAccessedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.requester = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.accessId = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.recordIds = (List<String>) ((Array) eventValues.getNonIndexedValues().get(1)).getNativeValueCopy();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RecordAccessedEventResponse getRecordAccessedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RECORDACCESSED_EVENT, log);
        RecordAccessedEventResponse typedResponse = new RecordAccessedEventResponse();
        typedResponse.log = log;
        typedResponse.requester = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.accessId = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.recordIds = (List<String>) ((Array) eventValues.getNonIndexedValues().get(1)).getNativeValueCopy();
        typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<RecordAccessedEventResponse> recordAccessedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRecordAccessedEventFromLog(log));
    }

    public Flowable<RecordAccessedEventResponse> recordAccessedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RECORDACCESSED_EVENT));
        return recordAccessedEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> logAccess(List<String> recordIds,
            String accessId) {
        final Function function = new Function(
                FUNC_LOGACCESS, 
                Arrays.<Type>asList(new DynamicArray<Utf8String>(
                        Utf8String.class,
                        org.web3j.abi.Utils.typeMap(recordIds, Utf8String.class)),
                new Utf8String(accessId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static RecordAccessContract load(String contractAddress, Web3j web3j,
            Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new RecordAccessContract(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static RecordAccessContract load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new RecordAccessContract(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static RecordAccessContract load(String contractAddress, Web3j web3j,
            Credentials credentials, ContractGasProvider contractGasProvider) {
        return new RecordAccessContract(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static RecordAccessContract load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new RecordAccessContract(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<RecordAccessContract> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return deployRemoteCall(RecordAccessContract.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<RecordAccessContract> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(RecordAccessContract.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), "");
    }

    public static RemoteCall<RecordAccessContract> deploy(Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(RecordAccessContract.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<RecordAccessContract> deploy(Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(RecordAccessContract.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), "");
    }

    private static String getDeploymentBinary() {
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }

    public static class RecordAccessedEventResponse extends BaseEventResponse {
        public String requester;

        public String accessId;

        public List<String> recordIds;

        public BigInteger timestamp;
    }
}
