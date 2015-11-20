# compassApp
Android -> Linux 

### install necessary packages

`$> sudo apt-get install build-essential libgtk2.0-dev libssl-dev xsltproc libxml2-dev libcap-dev`

`$> sudo apt-get install python scons`

### build thin core library and samples

```
$> cd ~/compassApp/ajtcl

$> scons WS=off
```

### build alljoin standard core library and samples

```
$> cd ~/compassApp/alljoyn

$> scons BINDINGS=cpp WS=off BT=off ICE=off SERVICES="about,notification,controlpanel,config,onboarding,sample_apps"
```

### run alljoin daemon

```
$> export LD_LIBRARY_PATH=~/compassApp/alljoyn/build/linux/x86_64/debug/dist/cpp/lib

$> ~/compassApp/alljoyn/build/linux/x86_64/debug/dist/cpp/bin/alljoyn-daemon
AllJoyn Message Bus Daemon version: v0.00.01
Copyright AllSeen Alliance.

Build: AllJoyn Library v0.00.01 (Built Tue Jul 07 17:59:57 UTC 2015 by dea - Git: alljoyn.git branch: 'master' tag: 'v15.04x-rc2' (+199 changes) commit ref: 578d67de81fb67c3924386acc333cee8d7bb558b)
Setting up transport for address: tcp:iface=*,port=9955
Setting up transport for address: udp:iface=*,port=9955
Setting up transport for address: unix:abstract=alljoyn
```
## run Android-application
### in another terminal run Compass client

```
$> cd
$> ~/compassApp/kjclient/kjclient

```
