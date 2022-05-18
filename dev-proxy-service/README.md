# 开发环境代理服务

通常用于跨地域在线调试

原理：通过frp进行内网穿透，通过N级域名来代理到对应到ip端口。

例: 内网服务为192.168.3.81:8200, 域名则为 192-168-3-81-8200.xxx.domain.com。

## 准备

1. 公网服务器一台
2. 内网服务器(主机)一台,需安装java8.
3. 公网域名
4. frp [下载](https://github.com/fatedier/frp/releases)

注意: frp安装使用自行百度。

## 安装

1. 在公网服务器中启动`frp server`,端口任意.
2. 解析泛域名`*.dev.domain.com`解析到公网服务器上,或者使用`*.local-host.cn`进行本地测试
3. 打包`mvn package`,并将`target/application.jar`复制到内网服务器上.
4. 在内网服务器上启动代理`nohup java -Dserver.port=12121 -jar application.jar & `.
5. 在内网服务器上启动`frp client`. (远程端口任意(需要开放到公网访问),本地端口:`12121`)
6. 尝试访问: {本地ip+端口 (.替换为-)}.dev.domain.com:{frp client配置到远程端口}

## 微服务

在注册到注册中心时，将服务到ip和端口设置为对应代理到ip和端口即可。

## tcp/udp代理

配置:
```yaml
proxy:
  frp:
    cmd: /home/user/frpc #frpc 命令绝对路径
    server-address: "127.0.0.1" # frp服务ip地址
    server-port: 7000   # frp服务端口
    token: token    # frp服务密码
    server-ports: 7001-7010 # 可用的frp服务端口范围,注意需要开发公网访问.
```

例: 内网服务为192.168.3.81:8200, 域名则为 tcp-192-168-3-81-8200.xxx.domain.com

请求后将返回通过公网访问内网tcp/udp 的ip和端口

