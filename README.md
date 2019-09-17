# Proxy Interceptor

It's app for Android that can send apps traffic via your proxy. Your device need to be **rooted**.

You can use this with [Burp Suite](https://portswigger.net/burp) proxy.

App using iptables routing like this:
> iptables -t nat -A OUTPUT -p tcp --dport \<dport\> -j DNAT --to-destination \<proxy address\>:\<proxy port\>

## How to use it

Using guide can be found [here](https://github.com/JohnJacket/proxyinterceptor/wiki).

<img src="/main-screen.png" width="40%">
