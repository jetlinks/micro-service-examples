package org.jetlinks.cloud.proxy.tcp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.util.StreamUtils;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class Frp {
    private String cmd;
    private String serverAddress;
    private int serverPort;
    private String token;

    private String serverPorts;

    private final Queue<Integer> remotePortPool = new ConcurrentLinkedDeque<>();

    private final List<Proxy> proxies = new CopyOnWriteArrayList<>();

    public Frp() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Proxy proxy : proxies) {
                proxy.dispose();
            }
        }));
    }

    public void setServerPorts(String serverPorts) {
        this.serverPorts = serverPorts;
        initPool();
    }

    private void initPool() {
        Set<Integer> set = new HashSet<>();
        for (String port : serverPorts.split(",")) {
            if (port.contains("-")) {
                String[] arr = port.split("-");
                int from = Integer.parseInt(arr[0]);
                int to = Integer.parseInt(arr[1]);
                for (int i = from; i < to; i++) {
                    set.add(i);
                }
            } else {
                set.add(Integer.parseInt(port));
            }
        }
        set.remove(serverPort);
        remotePortPool.addAll(set);
    }

    @SneakyThrows
    Proxy startProxy(InetSocketAddress address) {
        Integer port = remotePortPool.poll();
        if (port == null) {
            throw new IllegalStateException("frp server busy");
        }
        String config = "[common]\n" +
                "server_addr = %s\n" +
                "server_port = %d\n" +
                "token = %s\n" +
                "\n" +
                "[%s]\n" +
                "type = tcp\n" +
                "local_ip = %s\n" +
                "local_port = %s\n" +
                "remote_port = %d" +
                "\n" +
                "[%s]\n" +
                "type = udp\n" +
                "local_ip = %s\n" +
                "local_port = %s\n" +
                "remote_port = %d";

        String fileContent = String.format(
                config,
                serverAddress,
                serverPort,
                token,
                "tcp_proxy_" + address,
                address.getAddress().getHostAddress(),
                address.getPort(),
                port,
                "udp_proxy_" + address,
                address.getAddress().getHostAddress(),
                address.getPort(),
                port
        );

        File file = File.createTempFile("frp_" + System.currentTimeMillis(), ".conf");
        try (FileOutputStream out = new FileOutputStream(file)) {
            StreamUtils.copy(fileContent, StandardCharsets.UTF_8, out);
        }

        Process process = Runtime
                .getRuntime()
                .exec(new String[]{cmd, "-c", file.getAbsolutePath()});

        FrpProcess frpProcess = new FrpProcess(process, new InetSocketAddress(serverAddress, port));
        proxies.add(frpProcess);

        frpProcess.doOnDispose(() -> {
            remotePortPool.add(port);
            proxies.remove(frpProcess);
        });
        frpProcess.init();

        if (frpProcess.isDisposed()) {
            throw new IllegalStateException("start proxy error");
        }

        return frpProcess;
    }

    @AllArgsConstructor
    private static class FrpProcess implements Proxy {
        Process process;

        InetSocketAddress address;

        private final Disposable.Composite disposable = Disposables.composite();

        @Override
        public InetSocketAddress address() {
            return address;
        }

        protected void init() {
            Scanner std = new Scanner(process.getInputStream());
            Scanner error = new Scanner(process.getErrorStream());

            disposable.add(
                    Schedulers
                            .elastic()
                            .schedule(() -> {
                                while (!disposable.isDisposed()) {
                                    try {
                                        String line = std.nextLine();
                                        System.out.println(address + ": " + line);
                                        if (line.contains("already")) {
                                            dispose();
                                        }
                                    } catch (NoSuchElementException ignore) {
                                    }
                                }
                            })
            );

            disposable.add(
                    Schedulers
                            .elastic()
                            .schedule(() -> {
                                while (!disposable.isDisposed()) {
                                    try {
                                        System.err.println(address + ": " + error.nextLine());
                                        process.destroy();
                                        dispose();
                                    } catch (NoSuchElementException ignore) {

                                    }
                                }
                            })
            );
        }

        public void doOnDispose(Disposable disposable) {
            if (isDisposed()) {
                disposable.dispose();
                this.disposable.dispose();
                return;
            }
            this.disposable.add(disposable);
        }

        @Override
        public void dispose() {
            System.out.println("shutdown proxy " + address);
            disposable.dispose();
            process.destroy();
        }

        @Override
        public boolean isDisposed() {
            return !process.isAlive();
        }
    }
}
