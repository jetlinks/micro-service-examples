package org.jetlinks.cloud.proxy.tcp;

import reactor.core.Disposable;

import java.net.InetSocketAddress;

public interface Proxy extends Disposable {

    InetSocketAddress address();

    void doOnDispose(Disposable disposable);
}
