package ro.cs.tao.services.jupyter.impl;

import ro.cs.tao.services.commons.ContainerTunnelFilter;

import java.util.regex.Pattern;

public class JupyterTunnelFilter extends ContainerTunnelFilter {
    private final Pattern pattern;

    public JupyterTunnelFilter() {
        this.pattern = Pattern.compile("(/lab|/static/lab|/api|/kernelspecs|/lsp)");
    }

    @Override
    public Pattern filterExpression() {
        return this.pattern;
    }
}
