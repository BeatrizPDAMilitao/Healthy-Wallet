# Healthy Wallet🩺💉🏥 🔐

A lightweight, native Android wallet for Ethereum and Polygon networks, focused on simplicity and performance.

A secure digital wallet tailored for the healthcare industry. The wallet will allow patients, medical staff, and authorized stakeholders to manage, view, and share Electronic Health Records with ease.

## 🎯 Project Objectives

- **O1**: The design and development of a wallet for a mobile device to store and share EHRs;
- **O2**: Secure key management and user authentication in order to protect private keys and credentials;
- **O3**: Ensuring data privacy and control, empowering users to maintain full authority over their personal
  information;
- **O4**: Promote interoperability and data sharing across platforms and institutions;
- **O5**: Resilience against attacks, ensuring robust defenses against potential threats to maintain trust and reliability;
- **O6**: Usability and user experience, ensuring that the system is accessible, intuitive, and easy to navigate for all users.

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

## 📚 Documentation
documentation for Simple Android Wallet is in progress at: https://simple-wallet-docs.vercel.app/
