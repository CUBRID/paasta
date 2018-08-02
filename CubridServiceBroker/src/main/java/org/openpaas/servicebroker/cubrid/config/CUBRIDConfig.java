package org.openpaas.servicebroker.cubrid.config;

import javax.sql.DataSource;

import org.openpaas.servicebroker.util.JSchUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@PropertySources({
	@PropertySource("classpath:datasource.properties"),
	@PropertySource("classpath:session.properties")
})
public class CUBRIDConfig {
	
	@Autowired
	private Environment env;

	@Bean
	public JSchUtil jschUtil() {
		String host = this.env.getRequiredProperty("kubernetes.host");
		int port = Integer.parseInt(this.env.getRequiredProperty("kubernetes.port"));
		String username = this.env.getRequiredProperty("kubernetes.userName");
		String password = this.env.getRequiredProperty("kubernetes.password");
		String privateKey = this.env.getRequiredProperty("kubernetes.privateKey");
		JSchUtil jsch = new JSchUtil(host, port, username, password, privateKey);
		return jsch;
	}

	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(this.env.getRequiredProperty("jdbc.driver"));
		dataSource.setUrl(this.env.getRequiredProperty("jdbc.url"));
		dataSource.setUsername(this.env.getRequiredProperty("jdbc.userName"));
		dataSource.setPassword(this.env.getRequiredProperty("jdbc.password"));
		return dataSource;
	}
}