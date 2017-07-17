package ro.cs.tao.persistence.config;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.hibernate.jpa.HibernatePersistenceProvider;

import com.mchange.v2.c3p0.ComboPooledDataSource;

@Configuration
@EnableTransactionManagement
@PropertySource("classpath:persistence/persistence.properties")
public class DatabaseConfiguration {

	/**
	 * Constant for the database driver class property name (within .properties
	 * file)
	 */
	private static final String PROPERTY_NAME_DATABASE_DRIVER = "spring.database.driverClassName";

	/**
	 * TODO TO BE REPLACED from configuration
	 */
	private static final String PROPERTY_NAME_DATABASE_URL = "spring.datasource.url";

	/**
	 * TODO TO BE REPLACED from configuration
	 */
	private static final String PROPERTY_NAME_DATABASE_USERNAME = "spring.datasource.username";

	/**
	 * TODO TO BE REPLACED from configuration
	 */
	private static final String PROPERTY_NAME_DATABASE_PASSWORD = "spring.datasource.password";

	/**
	 * C3p0 Connection Pool minimum pool size
	 */
	private static final String PROPERTY_NAME_DATABASE_CONNECTION_MINPOOLSIZE = "spring.datasource.minPoolSize";

	/**
	 * C3p0 Connection Pool initial pool size
	 */
	private static final String PROPERTY_NAME_DATABASE_CONNECTION_INITIALPOOLSIZE = "spring.datasource.initialPoolSize";

	/**
	 * C3p0 Connection Pool maximum pool size
	 */
	private static final String PROPERTY_NAME_DATABASE_CONNECTION_MAXPOOLSIZE = "spring.datasource.maxPoolSize";

	/**
	 * C3p0 Connection Pool maximum statements
	 */
	private static final String PROPERTY_NAME_DATABASE_CONNECTION_MAXSTATEMENTS = "spring.datasource.maxStatements";

	/**
	 * C3p0 Connection Pool idle test period
	 */
	private static final String PROPERTY_NAME_DATABASE_CONNECTION_IDLETESTPERIOD = "spring.datasource.idleConnectionTestPeriod";

	/**
	 * C3p0 Connection Pool login timeout
	 */
	private static final String PROPERTY_NAME_DATABASE_CONNECTION_LOGINTIMEOUT = "spring.datasource.loginTimeout";

	/**
	 * Constant for the hibernate dialect property name (within .properties
	 * file)
	 */
	private static final String PROPERTY_NAME_HIBERNATE_DIALECT = "hibernate.dialect";

	/**
	 * Constant for the hibernate format sql flag property name (within
	 * .properties file)
	 */
	private static final String PROPERTY_NAME_HIBERNATE_FORMAT_SQL = "hibernate.format_sql";

	/**
	 * Constant for the hibernate naming strategy property name (within
	 * .properties file)
	 */
	private static final String PROPERTY_NAME_HIBERNATE_NAMING_STRATEGY = "hibernate.ejb.naming_strategy";

	/**
	 * Constant for the hibernate connection handling mode
	 * (https://jira.spring.io/browse/SPR-14393)
	 */
	private static final String PROPERTY_NAME_HIBERNATE_CONNECTION_HANDLING_MODE = "hibernate.connection.handling_mode";

	/**
	 * Constant for the hibernate connection release mode
	 */
	private static final String PROPERTY_NAME_HIBERNATE_CONNECTION_RELEASE_MODE = "hibernate.connection.release_mode";

	private static final String PROPERTY_NAME_HIBERNATE_TRANSACTION_AUTO_CLOSE_SESSION = "hibernate.transaction.auto_close_session";

	/**
	 * Constant for the hibernate show sql flag property name (within
	 * .properties file)
	 */
	private static final String PROPERTY_NAME_HIBERNATE_SHOW_SQL = "hibernate.show_sql";

	/**
	 * Constant for the Entity Manager packages to scan property name (within
	 * .properties file)
	 */
	private static final String PROPERTY_NAME_ENTITYMANAGER_PACKAGES_TO_SCAN = "entitymanager.packages.to.scan";

	/**
	 * Application environment, used to extract the properties
	 */
	@Resource
	private Environment environment;

	private List<ComboPooledDataSource> createdBeans = new ArrayList<>();

	/**
	 * Empty constructor
	 */
	public DatabaseConfiguration() {
		// empty constructor
	}

	/**
	 *  Data source with connection pool
	 */
	@Bean
	public DataSource dataSource() {
		final ComboPooledDataSource dataSource = new ComboPooledDataSource();
		try {

			dataSource.setDriverClass(environment.getRequiredProperty(PROPERTY_NAME_DATABASE_DRIVER));

			// DB URL + user name + pass from persistence.properties
			dataSource.setJdbcUrl(environment.getRequiredProperty(PROPERTY_NAME_DATABASE_URL));
			dataSource.setUser(environment.getRequiredProperty(PROPERTY_NAME_DATABASE_USERNAME));
			dataSource.setPassword(environment.getRequiredProperty(PROPERTY_NAME_DATABASE_PASSWORD));

			dataSource.setInitialPoolSize(Integer
					.parseInt(environment.getRequiredProperty(PROPERTY_NAME_DATABASE_CONNECTION_INITIALPOOLSIZE)));
			dataSource.setMinPoolSize(
					Integer.parseInt(environment.getRequiredProperty(PROPERTY_NAME_DATABASE_CONNECTION_MINPOOLSIZE)));
			dataSource.setMaxPoolSize(
					Integer.parseInt(environment.getRequiredProperty(PROPERTY_NAME_DATABASE_CONNECTION_MAXPOOLSIZE)));
			dataSource.setMaxStatements(
					Integer.parseInt(environment.getRequiredProperty(PROPERTY_NAME_DATABASE_CONNECTION_MAXSTATEMENTS)));
			dataSource.setIdleConnectionTestPeriod(Integer
					.parseInt(environment.getRequiredProperty(PROPERTY_NAME_DATABASE_CONNECTION_IDLETESTPERIOD)));
			dataSource.setLoginTimeout(
					Integer.parseInt(environment.getRequiredProperty(PROPERTY_NAME_DATABASE_CONNECTION_LOGINTIMEOUT)));

			if (dataSource == null || dataSource.getConnection() == null) {
				System.err.println("Database connection cannot be established!");
			}
		} catch (SQLException | IllegalStateException | PropertyVetoException e) {
			System.err.println("Error configuring data source: " + e.getMessage());
			System.err.println(ExceptionUtils.getStackTrace(e));
		}
		// add it to the internal list to be cleaned later
		createdBeans.add(dataSource);

		return dataSource;
	}

	/**
	 * Transaction Manager
	 * @return
	 * @throws ClassNotFoundException
	 */
	@Bean
	public JpaTransactionManager transactionManager() throws ClassNotFoundException {

		final JpaTransactionManager transactionManager = new JpaTransactionManager();
		final JpaDialect jpaDialect = new HibernateJpaDialect();
		transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
		transactionManager.setJpaDialect(jpaDialect);
		return transactionManager;
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() throws ClassNotFoundException {

		final LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactoryBean.setDataSource(dataSource());
		entityManagerFactoryBean
				.setPackagesToScan(environment.getRequiredProperty(PROPERTY_NAME_ENTITYMANAGER_PACKAGES_TO_SCAN));
		entityManagerFactoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);

		final Properties jpaProperties = new Properties();
		jpaProperties.put(PROPERTY_NAME_HIBERNATE_DIALECT,
				environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_DIALECT));
		jpaProperties.put(PROPERTY_NAME_HIBERNATE_FORMAT_SQL,
				environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_FORMAT_SQL));
		jpaProperties.put(PROPERTY_NAME_HIBERNATE_NAMING_STRATEGY,
				environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_NAMING_STRATEGY));
		jpaProperties.put(PROPERTY_NAME_HIBERNATE_SHOW_SQL,
				environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_SHOW_SQL));
		jpaProperties.put(PROPERTY_NAME_HIBERNATE_CONNECTION_HANDLING_MODE,
				environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_CONNECTION_HANDLING_MODE));
		jpaProperties.put(PROPERTY_NAME_HIBERNATE_CONNECTION_RELEASE_MODE,
				environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_CONNECTION_RELEASE_MODE));
		jpaProperties.put(PROPERTY_NAME_HIBERNATE_TRANSACTION_AUTO_CLOSE_SESSION,
				environment.getRequiredProperty(PROPERTY_NAME_HIBERNATE_TRANSACTION_AUTO_CLOSE_SESSION));

		entityManagerFactoryBean.setJpaProperties(jpaProperties);
		return entityManagerFactoryBean;
	}

}
