# Proxy Interceptor

It's app for Android that can send apps traffic via your proxy.

You can use this with Burp Suite.

App using iptables routing like:
> iptables -t nat -A OUTPUT -p tcp --dport <dport> -j DNAT --to-destination <proxy address>:<proxy port>
  
