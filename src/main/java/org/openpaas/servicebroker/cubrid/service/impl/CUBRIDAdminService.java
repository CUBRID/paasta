package org.openpaas.servicebroker.cubrid.service.impl;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.openpaas.servicebroker.model.CreateServiceInstanceRequest;
import org.openpaas.servicebroker.model.ServiceInstance;
import org.openpaas.servicebroker.model.ServiceInstanceBinding;
import org.openpaas.servicebroker.util.JSchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@PropertySources({ @PropertySource("classpath:session-dev.properties") })
@Service
public class CUBRIDAdminService {

	private Logger logger = LoggerFactory.getLogger(CUBRIDAdminService.class);

	/* findServiceInstanceInfo */
	public static final String SELECT_SERVICE_INSTANCE_INFO = "/* SELECT_SERVICE_INSTANCE_INFO */ SELECT [service_instance_id], [Service_definition_id], [plan_id], [organization_guid], [space_guid] FROM [service_instance_info] WHERE [service_instance_id] = ?";

	/* deleteServiceInstanceInfo */
	public static final String DELETE_SERVICE_INSTANCE_INFO = "/* DELETE_SERVICE_INSTANCE_INFO */ DELETE FROM [service_instance_info] WHERE [service_instance_id] = ?";

	/* findServiceBindInfo */
	public static final String SELECT_SERVICE_BIND_INFO = "/* SELECT_SERVICE_BIND_INFO */ SELECT [service_instance_bind_id], [service_instance_id], [application_id] FROM [service_bind_info] WHERE [service_instance_bind_id] = ?";

	/* deleteServiceBindInfo */
	public static final String DELETE_SERVICE_BIND_INFO = "/* DELETE_SERVICE_BIND_INFO */ DELETE FROM [service_bind_info] WHERE [service_instance_bind_id] = ?";

	/* isExistsUsingServicePort */
	public static final String SELECT_USING_SERVICE_PORT = "/* SELECT_USING_SERVICE_PORT */ SELECT count([service_port]) FROM [docker_service_info] WHERE [service_instance_id] = ?";

	/* deleteUsingServicePort */
	public static final String UPDATE_NULL_USING_SERVICE_PORT = "/* UPDATE_NULL_USING_SERVICE_PORT */ UPDATE [docker_service_info] SET ([service_instance_id], [purpose]) = (NULL, NULL) WHERE [service_instance_id] = ?";

	/* isExistsContainerInfo */
	public static final String SELECT_CONTAINER_NODE_IP = "/* SELECT_CONTAINER_NODE_IP */ SELECT [node_ip] FROM [docker_container_info] WHERE [container_name] = ?";

	/* deleteContainerInfo */
	public static final String DELETE_CONTAINER_INFO = "/* DELETE_CONTAINER_INFO */ DELETE FROM [docker_container_info] WHERE [container_name] = ?";

	/* isExistsServiceBind */
	public static final String SELECT_SERVICE_BIND = "/* SELECT_SERVICE_BIND */ SELECT count([service_instance_bind_id]) FROM [service_bind_info] WHERE [service_instance_id] = ?";

	/* deleteServiceBind */
	public static final String DELETE_SERVICE_BIND = "/* DELETE_SERVICE_BIND */ DELETE FROM [service_bind_info] WHERE [service_instance_id] = ?";

	/* createDockerService */
	public static final String SELECT_RANDOM_SERVICE_PORT = "/* SELECT_RANDOM_SERVICE_PORT */ SELECT [service_port] FROM [docker_service_info] WHERE [service_instance_id] IS NULL ORDER BY RANDOM() LIMIT 4";
	public static final String SELECT_PLAN_REPOSITORY_TAG = "/* SELECT_PLAN_REPOSITORY_TAG */ SELECT [repository] FROM [service_plan_info] WHERE [plan_id] = ?";
	public static final String SELECT_RANDOM_DOCKER_NODE_IP = "/* SELECT_RANDOM_DOCKER_NODE_IP */ SELECT [node_ip] FROM [docker_node_info] ORDER BY RANDOM() LIMIT 1";

	/* save */
	public static final String INSERT_SERVICE_INSTANCE_INFO = "/* INSERT_SERVICE_INSTANCE_INFO */ INSERT INTO [service_instance_info] ([service_instance_id], [service_definition_id], [plan_id], [organization_guid], [space_guid]) VALUES (?, ?, ?, ?, ?)";
	public static final String INSERT_DOCKER_CONTAINRT_INFO = "/* INSERT_DOCKER_CONTAINRT_INFO */ INSERT INTO [docker_container_info] ([container_id], [container_name], [container_ip], [node_ip], [database_name]) VALUES (?, ?, ?, ?, ?)";
	public static final String UPDATE_DOCKER_SERVICE_INFO = "/* UPDATE_DOCKER_SERVICE_INFO */ UPDATE [docker_service_info] SET ([service_instance_id], [purpose], [updateDateTime]) = (?, ?, SYSDATETIME) WHERE [service_port] = ?";

	/* saveBind */
	public static final String INSERT_SERVICE_INSTANCE_BIND_INFO = "/* INSERT_SERVICE_INSTANCE_BIND_INFO */ INSERT INTO [service_bind_info] ([service_instance_bind_id], [service_instance_id], [application_id], [username], [password]) VALUES (?, ?, ?, ?, ?)";

	/* getCredentialsInfo */
	public static final String SELECT_SERVICE_CREDENTIALS_INFO = "/* SELECT_SERVICE_CREDENTIALS_INFO */ "
			+ "SELECT [T1].[SSH]                                                           "
			+ "  ,[T1].[query_editor]                                                      "
			+ "  ,[T1].[BROKER1]                                                           "
			+ "  ,[T1].[Manager]                                                           "
			+ "  ,[T2].[username]                                                          "
			+ "  ,[T2].[password]                                                          "
			+ "  ,[T3].[container_id]                                                      "
			+ "  ,[T3].[node_ip]                                                           "
			+ "  ,[T3].[database_name]                                                     "
			+ "FROM (                                                                      "
			+ "  SELECT [service_instance_id]                                              "
			+ "    ,MAX(DECODE([purpose], 'SSH', [service_port])) AS SSH                   "
			+ "    ,MAX(DECODE([purpose], 'query_editor', [service_port])) AS query_editor "
			+ "    ,MAX(DECODE([purpose], 'BROKER1', [service_port])) AS BROKER1           "
			+ "    ,MAX(DECODE([purpose], 'Manager', [service_port])) AS Manager           "
			+ "  FROM [docker_service_info]                                                "
			+ "  GROUP BY [service_instance_id]                                            "
			+ "  ) AS [T1]                                                                 "
			+ "  ,[service_bind_info] AS [T2]                                              "
			+ "  ,[docker_container_info] AS [T3]                                          "
			+ "WHERE [T1].[service_instance_id] = [T2].[service_instance_id]               "
			+ "  AND [T1].[service_instance_id] = [T3].[container_name]                    "
			+ "  AND [T2].[service_instance_bind_id] = ?                                   ";

	/* createCredentialsInfo */
	public static final String CREATE_SERVICE_CREDENTIALS_INFO = "/* CREATE_SERVICE_CREDENTIALS_INFO */ "
			+ "SELECT [T1].[SSH]                                                           "
			+ "  ,[T1].[query_editor]                                                      "
			+ "  ,[T1].[BROKER1]                                                           "
			+ "  ,[T1].[Manager]                                                           "
			+ "  ,[T3].[node_ip]                                                           "
			+ "  ,[T3].[container_id]                                                      "
			+ "  ,[T3].[database_name]                                                     "
			+ "FROM (                                                                      "
			+ "  SELECT [service_instance_id]                                              "
			+ "    ,MAX(DECODE([purpose], 'SSH', [service_port])) AS SSH                   "
			+ "    ,MAX(DECODE([purpose], 'query_editor', [service_port])) AS query_editor "
			+ "    ,MAX(DECODE([purpose], 'BROKER1', [service_port])) AS BROKER1           "
			+ "    ,MAX(DECODE([purpose], 'Manager', [service_port])) AS Manager           "
			+ "  FROM [docker_service_info]                                                "
			+ "  GROUP BY [service_instance_id]                                            "
			+ "  ) AS [T1]                                                                 "
			+ "  ,[docker_container_info] AS [T3]                                          "
			+ "WHERE [T1].[service_instance_id] = [T3].[container_name]                    "
			+ "  AND [T1].[service_instance_id] = ?                                        ";
	
	/* getPassword */
	public static final String SELECT_BASE64_STRING = "SELECT TO_BASE64(?)";
	
	@Autowired
	private JSchUtil jsch;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public ServiceInstance findServiceInstanceInfo(String serviceInstanceId) {
		try {
			return jdbcTemplate.queryForObject(SELECT_SERVICE_INSTANCE_INFO, new RowMapper<ServiceInstance>() {
				@Override
				public ServiceInstance mapRow(ResultSet rs, int rowNum) throws SQLException {
					CreateServiceInstanceRequest createServiceInstanceRequest = new CreateServiceInstanceRequest();

					createServiceInstanceRequest.withServiceInstanceId(rs.getString("service_instance_id"));
					createServiceInstanceRequest.setServiceDefinitionId(rs.getString("Service_definition_id"));
					createServiceInstanceRequest.setPlanId(rs.getString("plan_id"));
					createServiceInstanceRequest.setOrganizationGuid(rs.getString("organization_guid"));
					createServiceInstanceRequest.setSpaceGuid(rs.getString("space_guid"));

					return new ServiceInstance(createServiceInstanceRequest);
				}
			}, serviceInstanceId);
		} catch (DataAccessException e) {
			logger.warn("ID : " + serviceInstanceId + " 인 Service Instance가 존재하지 않습니다.");

			return null;
		}
	}

	public void deleteServiceInstanceInfo(String serviceInstanceId) {
		try {
			this.jdbcTemplate.update(DELETE_SERVICE_INSTANCE_INFO, new Object[] { serviceInstanceId });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
	}

	public ServiceInstanceBinding findServiceBindInfo(String serviceInstanceBindId) {
		try {
			return jdbcTemplate.queryForObject(SELECT_SERVICE_BIND_INFO, new RowMapper<ServiceInstanceBinding>() {
				@Override
				public ServiceInstanceBinding mapRow(ResultSet rs, int rowNum) throws SQLException {
					return new ServiceInstanceBinding(rs.getString("service_instance_bind_id"),
							rs.getString("service_instance_id"), getCredentialsInfo(serviceInstanceBindId), "",
							rs.getString("application_id"));
				}
			}, serviceInstanceBindId);
		} catch (DataAccessException e) {
			logger.warn("BIND ID : " + serviceInstanceBindId + " 인 Service Instance Bind 정보가 존재하지 않습니다.");

			return null;
		}
	}

	public void deleteServiceBindInfo(String serviceInstanceBindId) {
		try {
			this.jdbcTemplate.update(DELETE_SERVICE_BIND_INFO, new Object[] { serviceInstanceBindId });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
	}

	public boolean isExistsUsingServicePort(String serviceInstanceId) {
		Integer count = 0;
		try {
			count = jdbcTemplate.queryForObject(SELECT_USING_SERVICE_PORT, Integer.class, serviceInstanceId);
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}

		return count > 0 ? true : false;
	}

	public void deleteUsingServicePort(String serviceInstanceId) {
		try {
			this.jdbcTemplate.update(UPDATE_NULL_USING_SERVICE_PORT, new Object[] { serviceInstanceId });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
	}

	public String isExistsContainerInfo(String serviceInstanceId) {
		String dockerNodeIp = null;
		try {
			dockerNodeIp = jdbcTemplate.queryForObject(SELECT_CONTAINER_NODE_IP, String.class, serviceInstanceId);
		} catch (DataAccessException e) {
			logger.warn("ID : " + serviceInstanceId + " 인 Service Instance가 사용 중인 Container 정보가 없습니다.");
		}

		return dockerNodeIp;
	}

	public void deleteContainerInfo(String serviceInstanceId) {
		try {
			this.jdbcTemplate.update(DELETE_CONTAINER_INFO, new Object[] { serviceInstanceId });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
	}

	public boolean isExistsServiceBind(String serviceInstanceId) {
		Integer count = 0;
		try {
			count = jdbcTemplate.queryForObject(SELECT_SERVICE_BIND, Integer.class, serviceInstanceId);
		} catch (DataAccessException e) {
			logger.warn("ID : " + serviceInstanceId + " 인 Service Instance Bind 정보가 존재하지 않습니다.");
		}

		return count > 0 ? true : false;
	}

	public void deleteServiceBind(String serviceInstanceId) {
		try {
			this.jdbcTemplate.update(DELETE_SERVICE_BIND, new Object[] { serviceInstanceId });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
	}

	public void deleteDockerService(String serviceInstanceId, String dockerNodeIp) {
		StringBuffer command = new StringBuffer();
		List<String> result = null;

		jsch.connect(dockerNodeIp);

		command.setLength(0);

		command.append("docker ps -a -f name=" + serviceInstanceId + " --no-trunc -q");

		result = jsch.execute(command.toString());
		if (result.size() > 0) {
			command.setLength(0);

			command.append("docker container stop " + serviceInstanceId);
			command.append(" && docker container rm " + serviceInstanceId);

			result = jsch.execute(command.toString());
		}

		jsch.disconnect();
	}

	public Map<String, String> createDockerService(ServiceInstance serviceInstance) {
		String serviceInstanceId = serviceInstance.getServiceInstanceId();
		String planId = serviceInstance.getPlanId();
		
		String dockerContainerId = null;
		List<String> dockerPortList = null;
		String databaseName = null;
		
		Map<String, String> dockerServiceInfo = new HashMap<String, String>();
		
		StringBuffer command = new StringBuffer();
		List<String> result = null;
		
		try {
			dockerPortList = jdbcTemplate.queryForList(SELECT_RANDOM_SERVICE_PORT, String.class);
		} catch (DataAccessException e) {
			logger.warn("Service Broker에서 사용 가능한 PORT가 없습니다.");
			
			return null;
		}

		String dockerRepository = null;
		try {
			dockerRepository = jdbcTemplate.queryForObject(SELECT_PLAN_REPOSITORY_TAG, String.class, planId);
		} catch (DataAccessException e) {
			logger.warn("PLAN ID : " + planId + " 로 사용 가능한 Docker Image가 없습니다.");
			
			return null;
		}

		if ((dockerPortList.size() == 4) && (dockerRepository != null)) {
			String dockerNodeIp = null;
			try {
				dockerNodeIp = jdbcTemplate.queryForObject(SELECT_RANDOM_DOCKER_NODE_IP, String.class);
			} catch (DataAccessException e) {
				logger.warn(e.getLocalizedMessage(), e);
				
				return null;
			}

			jsch.connect(dockerNodeIp);

			command.setLength(0);

			command.append("docker run -d --privileged");
			command.append(" --name " + serviceInstanceId);
			command.append(" -p " + dockerPortList.get(0) + ":22");
			command.append(" -p " + dockerPortList.get(1) + ":30000");
			command.append(" -p " + dockerPortList.get(2) + ":33000");
			command.append(" -p " + dockerPortList.get(3) + ":8001");
			command.append(" " + dockerRepository);

			result = jsch.execute(command.toString());
			if (result.size() > 0) {
				dockerServiceInfo.put("dockerNodeIp", dockerNodeIp);
				
				dockerContainerId = result.get(0);
				databaseName = dockerContainerId.substring(0, 12);
				
				dockerServiceInfo.put("dockerContainerId", dockerContainerId);
				dockerServiceInfo.put("dockerContainerName", serviceInstanceId);
				dockerServiceInfo.put("dockerServicePort_SSH", dockerPortList.get(0));
				dockerServiceInfo.put("dockerServicePort_Broker1", dockerPortList.get(1));
				dockerServiceInfo.put("dockerServicePort_Broker2", dockerPortList.get(2));
				dockerServiceInfo.put("dockerServicePort_Manager", dockerPortList.get(3));
				dockerServiceInfo.put("databaseName", databaseName);
			}

			result = jsch.execute("docker inspect -f '{{ .NetworkSettings.IPAddress }}' " + dockerContainerId);
			if (result.size() > 0) {
				dockerServiceInfo.put("dockerContainerIp", result.get(0));
			}

			command.setLength(0);
			
			String dockerCommnad = "docker exec -u cubrid " + dockerContainerId + " /bin/bash -c ";
			String dockerBackgrounCommnad = "docker exec -d -u cubrid " + dockerContainerId + " /bin/bash -c ";
			
			jsch.execute(dockerCommnad + "'cd $CUBRID_DATABASES && mkdir -p " + databaseName + "'");
			jsch.execute(dockerCommnad + "'cd $CUBRID_DATABASES/" + databaseName +" && mkdir -p vol log java'");
			
			if ("512M".equals(planId)) { // DATA(Generic + Data + Index) : 512M, TEMP : 512M
				jsch.execute(dockerCommnad + "'cd $CUBRID/conf && cp cubrid.conf_512M cubrid.conf && cp cubrid_broker.conf_5-40 cubrid_broker.conf'");
				jsch.execute(dockerCommnad + "'cubrid createdb -F $CUBRID_DATABASES/" + databaseName + " -L $CUBRID_DATABASES/" + databaseName + "/log " + databaseName + " ko_kr.utf8'");
				jsch.execute(dockerCommnad + "'cubrid addvoldb --db-volume-size=512M -F $CUBRID_DATABASES/" + databaseName + "/vol -p temp " + databaseName + " -S'");
			} else if ("1.5G".equals(planId)) { // DATA(Generic + Data + Index) : 1.5G, TEMP : 512M
				jsch.execute(dockerCommnad + "'cd $CUBRID/conf && cp cubrid.conf_1.5G cubrid.conf && cp cubrid_broker.conf_5-40 cubrid_broker.conf'");
				jsch.execute(dockerCommnad + "'cubrid createdb -F $CUBRID_DATABASES/" + databaseName + " -L $CUBRID_DATABASES/" + databaseName + "/log " + databaseName + " ko_kr.utf8'");
				jsch.execute(dockerCommnad + "'cubrid addvoldb --db-volume-size=1G -F $CUBRID_DATABASES/" + databaseName + "/vol -p data " + databaseName + " -S'");
				jsch.execute(dockerCommnad + "'cubrid addvoldb --db-volume-size=512M -F $CUBRID_DATABASES/" + databaseName + "/vol -p temp " + databaseName + " -S'");
			} else if ("2.5G".equals(planId)) { // DATA(Generic + Data + Index) : 2.5G, TEMP : 1G
				jsch.execute(dockerCommnad + "'cd $CUBRID/conf && cp cubrid.conf_2.5G cubrid.conf && cp cubrid_broker.conf_5-40 cubrid_broker.conf'");
				jsch.execute(dockerCommnad + "'cubrid createdb -F $CUBRID_DATABASES/" + databaseName + " -L $CUBRID_DATABASES/" + databaseName + "/log " + databaseName + " ko_kr.utf8'");
				jsch.execute(dockerCommnad + "'cubrid addvoldb --db-volume-size=2G -F $CUBRID_DATABASES/" + databaseName + "/vol -p data " + databaseName + " -S'");
				jsch.execute(dockerCommnad + "'cubrid addvoldb --db-volume-size=1G -F $CUBRID_DATABASES/" + databaseName + "/vol -p temp " + databaseName + " -S'");
			} else if ("4.5G".equals(planId)) { // DATA(Generic + Data + Index) : 4.5G, TEMP : 2G
				jsch.execute(dockerCommnad + "'cd $CUBRID/conf && cp cubrid.conf_4.5G cubrid.conf && cp cubrid_broker.conf_20-80 cubrid_broker.conf'");
				jsch.execute(dockerCommnad + "'cubrid createdb -F $CUBRID_DATABASES/" + databaseName + " -L $CUBRID_DATABASES/" + databaseName + "/log " + databaseName + " ko_kr.utf8'");
				jsch.execute(dockerCommnad + "'cubrid addvoldb --db-volume-size=2G -F $CUBRID_DATABASES/" + databaseName + "/vol -p data " + databaseName + " -S'");
				jsch.execute(dockerCommnad + "'cubrid addvoldb --db-volume-size=2G -F $CUBRID_DATABASES/" + databaseName + "/vol -p data " + databaseName + " -S'");
				jsch.execute(dockerCommnad + "'cubrid addvoldb --db-volume-size=2G -F $CUBRID_DATABASES/" + databaseName + "/vol -p temp " + databaseName + " -S'");
			}
			
			jsch.execute(dockerCommnad + "'chmod -w $CUBRID_DATABASES/" + databaseName + "'");
			jsch.execute(dockerCommnad + "'sudo chown -R cubrid. /home/cubrid\"");
			
			jsch.execute(dockerCommnad + "'sed -e s/#server=foo,bar/server=" + databaseName + "/g -i $CUBRID/conf/cubrid.conf'");
		
			jsch.execute(dockerCommnad + "\"csql -u dba " + databaseName + " -c \\\"ALTER USER dba PASSWORD '" + getPassword(serviceInstanceId) + "'\\\"\"");
			
			jsch.execute(dockerBackgrounCommnad + "'cubrid service start'");

			jsch.disconnect();
		}

		return dockerServiceInfo;
	}

	public void save(ServiceInstance serviceInstance, Map<String, String> dockerServiceInfo) {
		try {
			this.jdbcTemplate.update(INSERT_SERVICE_INSTANCE_INFO,
					new Object[] { serviceInstance.getServiceInstanceId(), serviceInstance.getServiceDefinitionId(),
							serviceInstance.getPlanId(), serviceInstance.getOrganizationGuid(),
							serviceInstance.getSpaceGuid() });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}

		try {
			this.jdbcTemplate.update(INSERT_DOCKER_CONTAINRT_INFO,
					new Object[] { dockerServiceInfo.get("dockerContainerId"),
							dockerServiceInfo.get("dockerContainerName"), dockerServiceInfo.get("dockerContainerIp"),
							dockerServiceInfo.get("dockerNodeIp"), dockerServiceInfo.get("databaseName") });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}

		try {
			this.jdbcTemplate.update(UPDATE_DOCKER_SERVICE_INFO,
					new Object[] { dockerServiceInfo.get("dockerContainerName"), "SSH",
							dockerServiceInfo.get("dockerServicePort_SSH") });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}

		try {
			this.jdbcTemplate.update(UPDATE_DOCKER_SERVICE_INFO,
					new Object[] { dockerServiceInfo.get("dockerContainerName"), "query_editor",
							dockerServiceInfo.get("dockerServicePort_Broker1") });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}

		try {
			this.jdbcTemplate.update(UPDATE_DOCKER_SERVICE_INFO,
					new Object[] { dockerServiceInfo.get("dockerContainerName"), "BROKER1",
							dockerServiceInfo.get("dockerServicePort_Broker2") });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}

		try {
			this.jdbcTemplate.update(UPDATE_DOCKER_SERVICE_INFO,
					new Object[] { dockerServiceInfo.get("dockerContainerName"), "Manager",
							dockerServiceInfo.get("dockerServicePort_Manager") });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
	}

	public void saveBind(ServiceInstanceBinding serviceInstanceBinding) {
		try {
			this.jdbcTemplate.update(INSERT_SERVICE_INSTANCE_BIND_INFO,
					new Object[] { serviceInstanceBinding.getId(), serviceInstanceBinding.getServiceInstanceId(),
							serviceInstanceBinding.getAppGuid(),
							serviceInstanceBinding.getCredentials().get("username"),
							serviceInstanceBinding.getCredentials().get("password") });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
	}

	public Map<String, Object> getCredentialsInfo(String serviceInstanceBindId) {
		Map<String, Object> credentials = null;
		try {
			credentials = jdbcTemplate.queryForObject(SELECT_SERVICE_CREDENTIALS_INFO,
					new RowMapper<Map<String, Object>>() {
						@Override
						public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
							Map<String, Object> credentials = new HashMap<String, Object>();

							credentials.put("port_ssh", rs.getString("SSH"));
							credentials.put("port_queryeditor", rs.getString("query_editor"));
							credentials.put("port_broker1", rs.getString("BROKER1"));
							credentials.put("port_manager", rs.getString("Manager"));
							credentials.put("username", rs.getString("username"));
							credentials.put("password", rs.getString("password"));
							credentials.put("hostname", rs.getString("node_ip"));
							credentials.put("name", rs.getString("database_name"));
							credentials.put("jdbcUrl", "jdbc:cubrid:" + rs.getString("node_ip") + ":" + rs.getString("BROKER1") + ":" + rs.getString("database_name") + ":" + rs.getString("username") +  ":" + rs.getString("password") + ":");

							return credentials;
						}
					}, serviceInstanceBindId);
		} catch (DataAccessException e) {
			logger.warn("BIND ID : " + serviceInstanceBindId + " 인 Credentials 정보가 존재하지 않습니다.");
		}
		
		return credentials;
	}
	
	public Map<String, Object> createCredentialsInfo(String serviceInstanceBindId, String serviceInstanceId) {
		Map<String, Object> credentials = null;
		
		try {
			credentials = jdbcTemplate.queryForObject(CREATE_SERVICE_CREDENTIALS_INFO,
					new RowMapper<Map<String, Object>>() {
						@Override
						public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
							Map<String, Object> credentials = new HashMap<String, Object>();

							credentials.put("port_ssh", rs.getString("SSH"));
							credentials.put("port_queryeditor", rs.getString("query_editor"));
							credentials.put("port_broker1", rs.getString("BROKER1"));
							credentials.put("port_manager", rs.getString("Manager"));
							credentials.put("hostname", rs.getString("node_ip"));
							credentials.put("name", rs.getString("database_name"));

							return credentials;
						}
					}, serviceInstanceId);
		} catch (DataAccessException e) {
			logger.warn("ID : " + serviceInstanceId + " BIND ID : " + serviceInstanceBindId + " 로 Credentials 정보 생성에 필요한 데이터가 없습니다.");
		}
		
		credentials.put("username", "dba");
		credentials.put("password", getPassword(serviceInstanceId));
				
		if (credentials != null) {
			credentials.put("jdbcUrl", "jdbc:cubrid:" + credentials.get("hostname") + ":" + credentials.get("port_broker1") + ":" + credentials.get("name") + ":" + credentials.get("username") + ":" + credentials.get("password") + ":");
		}
		
		return credentials;
	}
	
	/* 미사용 */
	private String getUsername() {
		MessageDigest messageDigest = null;
		String username = null;

		do {
			try {
				messageDigest = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		
			messageDigest.update(UUID.randomUUID().toString().getBytes());
			
			username = new BigInteger(1, messageDigest.digest()).toString(16).replaceAll("/[^a-zA-Z]+/", "").substring(0, 16);
		} while (!username.matches("^[a-zA-Z][a-zA-Z0-9]+"));
		
		return username;
	}
	
	/* 미사용 */
	private String getPassword() {
		MessageDigest messageDigest = null;
		String password = null;
		
		try {
			messageDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	
		messageDigest.update(UUID.randomUUID().toString().getBytes());
		
		password = new BigInteger(1, messageDigest.digest()).toString(16).replaceAll("/[^a-zA-Z]+/", "").substring(0, 16);
		
		try {
			password = jdbcTemplate.queryForObject(SELECT_BASE64_STRING, String.class, password);
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
		
		return password;
	}
	
	private String getPassword(String serviceInstanceId) {
		MessageDigest messageDigest = null;
		String password = null;
		
		try {
			messageDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	
		messageDigest.update(serviceInstanceId.getBytes());
		
		password = new BigInteger(1, messageDigest.digest()).toString(16).replaceAll("/[^a-zA-Z]+/", "").substring(0, 16);
		
		try {
			password = jdbcTemplate.queryForObject(SELECT_BASE64_STRING, String.class, password);
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
		
		return password;
	}
}
