package com.astral;

import me.internalizable.numdrassl.api.ProxyServer;
import me.internalizable.numdrassl.api.event.Subscribe;
import me.internalizable.numdrassl.api.event.proxy.ProxyInitializeEvent;
import me.internalizable.numdrassl.api.plugin.Inject;
import me.internalizable.numdrassl.api.plugin.Plugin;



@Plugin(
        id = "my-plugin",
        name = "My Plugin",
        version = "1.0.0",
        authors = {"YourName"},
        description = "My first Numdrassl plugin"
)
public final class Main {
    @Inject
    private ProxyServer server;


    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        System.out.println("Astral Proxy Initialize");

    }
}
