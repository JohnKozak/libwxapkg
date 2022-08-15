A tool to unpacking Wechat applet packages.

Usage:

Install via:
```
bash <(curl -sSL https://raw.githubusercontent.com/JohnKozak/libwxapkg/master/install)
```
Then
```
unwxapkg <filename>
```

Use `-h | --help` to see how to use.

The jar file will be installed into `~/.local/lib/classpath` (Modify it by given INSTALL_PATH variable), 
and the executable script will be installed into `~/.local/bin` (Modify it by given BIN_PATH variable).

If you want to install it from source, give `FROM_SOURCE=true` to the installer script:

```
bash <(FROM_SOURCE=true curl -sSL https://raw.githubusercontent.com/JohnKozak/libwxapkg/master/install)
```