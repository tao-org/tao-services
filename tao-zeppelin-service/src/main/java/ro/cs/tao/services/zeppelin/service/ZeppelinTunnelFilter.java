package ro.cs.tao.services.zeppelin.service;

import ro.cs.tao.services.commons.ContainerTunnelFilter;

import java.util.regex.Pattern;

public class ZeppelinTunnelFilter extends ContainerTunnelFilter {
    private final Pattern pattern;

    public ZeppelinTunnelFilter() {
        this.pattern = Pattern.compile("^(/api|/ws|/notebook|/assets|/logs|/helium|/config|/interpreter|/login|/logout|/zeppelin-web)(/.*)?$");
    }

    @Override
    public Pattern filterExpression() {
        return this.pattern;
    }
}
