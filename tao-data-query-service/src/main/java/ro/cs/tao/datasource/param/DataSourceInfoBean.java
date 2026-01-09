package ro.cs.tao.datasource.param;

import ro.cs.tao.datasource.DataSourceCredentials;

import java.util.ArrayList;
import java.util.List;

/**
 * A bean class representing the information about a data source, including its parameters and authentication details.
 */
public class DataSourceInfoBean {
    final List<DataSourceParameterBean> parameter = new ArrayList<>();
    final DataSourceCredentials auth;

    public DataSourceInfoBean(List<DataSourceParameterBean> parameter, DataSourceCredentials auth) {
        this.parameter.addAll(parameter);
        this.auth = auth;
    }

    public List<DataSourceParameterBean> getParameter() {
        return parameter;
    }

    public DataSourceCredentials getAuth() {
        return auth;
    }
}
