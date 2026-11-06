package ro.cs.tao.services.jupyter.impl;

import ro.cs.tao.services.commons.ContainerTunnelFilter;

import java.net.URI;
import java.util.regex.Pattern;

public class JupyterTunnelFilter extends ContainerTunnelFilter {
    private final Pattern pattern;

    public JupyterTunnelFilter() {
        this.pattern = Pattern.compile("^(/lab|/static|/api|/kernelspecs|/lsp|/terminals)(/.*)?$");
    }

    @Override
    public Pattern filterExpression() {
        return this.pattern;
    }

    @Override
    protected String targetScheme(URI servicesUri) {
        return "https";
    }
}
