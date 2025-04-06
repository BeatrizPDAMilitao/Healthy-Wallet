# 🔍 Step 1: Get WSL2 IP (inside Ubuntu)
```shell
ip a
```
Look for an IP under eth0, for example:

```shell
inet 172.26.146.19/20
```
This is your WSL2 instance's internal IP used for forwarding.

# 🧠 Step 2: Forward Ports from Windows Host to WSL2 (Run in PowerShell as Administrator)
Replace 172.26.146.19 with your actual WSL2 IP from step 1.

## 📡 Forward Geth JSON-RPC (22000)
```shell
netsh interface portproxy add v4tov4 listenport=22000 listenaddress=0.0.0.0 connectport=22000 connectaddress=172.26.146.19
netsh advfirewall firewall add rule name="Geth RPC 22000" dir=in action=allow protocol=TCP localport=22000
```

# 🔒 Forward Tessera Enclave Port (9085)
```shell
netsh interface portproxy add v4tov4 listenport=9085 listenaddress=0.0.0.0 connectport=9085 connectaddress=172.26.146.19
netsh advfirewall firewall add rule name="Tessera Enclave 9085" dir=in action=allow protocol=TCP localport=9085
```

These commands expose both services to your LAN — so your Android device can access them via the Windows host IP (e.g., 192.168.1.2).

# 🔁 (Optional) To Remove Forwarding Rules Later
```shell
netsh interface portproxy delete v4tov4 listenport=22000 listenaddress=0.0.0.0
netsh interface portproxy delete v4tov4 listenport=9085 listenaddress=0.0.0.0
```

# ✅ Final Testing
From WSL2:

```shell
curl http://localhost:22000
curl http://localhost:9085/upcheck
```

From Android browser or app:

```shell
http://192.168.1.2:22000
http://192.168.1.2:9085/upcheck
```

If both return responses, you're good to go for sending private transactions!

