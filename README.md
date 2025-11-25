# Healthy Wallet🩺💉🏥 🔐

A secure digital wallet tailored for the healthcare industry. The wallet will allow patients, medical staff, and authorized stakeholders to manage, view, and share Electronic Health Records with ease.

## 🎯 Project Contributions

- **C1**: A blockchain-enabled EHRs wallet application that empowers users to securely share their EHRs
 through decentralized technology, providing enhanced control over personal medical data;
- **C2**: A novel hybrid architecture for decentralized EHRs storage that strategically combines blockchain
 technology with public cloud infrastructure to achieve both security and scalability in health data
 management;
- **C3**: A seamless integration between the MedPlum medical platform and the Healthy Wallet applica
tion, demonstrating practical interoperability between existing healthcare systems and blockchain
based personal health record management solutions;
- **C4**: Integration with a ZKP server capable of performing range proofs on EHRs. This allows patients
 to prove that specific medical values fall within a valid range without revealing the actual data.

## 📹 Demo Video

[FullDEMO.mp4](FullDEMO.mp4)

## Setup

To be able to run the project you need to:
- Run the ZKP server
- Have a MedPlum account so you can login via OAuth2 (https://app.medplum.com/)

To run the ZKP server:
### Run as Admin in Windows PowerShell

To get the ipV4 Address
```shell
ipconfig
```
In the App change the (CallZkpApi.kt) ZKP server ip to what you retrieved from the ipconfig command. Also add that ip address to the network_security_config.xml file.

```shell
netsh interface portproxy add v4tov4 listenport=3000 listenaddress=0.0.0.0 connectport=3000 connectaddress=172.26.146.19
```
```shell
New-NetFirewallRule -DisplayName "Allow ZKP Port 3000" -Direction Inbound -LocalPort 3000 -Protocol TCP -Action Allow
```

### To run the server
```shell
$ cargo run -r
```

On the server side change the url returned in `src/main.rs` to the correct address (The one retrieved from the first command - 192.168.1.something).

## 🛠️ Tech Stack

- Kotlin
- Jetpack Compose
- Web3j
