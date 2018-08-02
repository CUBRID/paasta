package org.openpaas.servicebroker.cubrid.service.impl;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpaas.servicebroker.model.CreateServiceInstanceRequest;
import org.openpaas.servicebroker.model.ServiceInstance;
import org.openpaas.servicebroker.model.ServiceInstanceBinding;
import org.openpaas.servicebroker.util.JSchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class CUBRIDAdminService {

	private Logger logger = LoggerFactory.getLogger(CUBRIDAdminService.class);

	/* Credentials Info */
	public static final String CREDENTIAL_INFO_CUBRID_MANAGER_LABEL = "cubridmanager_port";
	public static final String CREDENTIAL_INFO_CUBRID_QUERY_EDITOR_LABEL = "queryeditor_port";
	public static final String CREDENTIAL_INFO_CUBRID_BROKER1_LABEL = "broker1_port";
	public static final String CREDENTIAL_INFO_HOST_NAME_LABEL = "hostname";
	public static final String CREDENTIAL_INFO_DATABASE_NAME_LABEL = "database_name";
	public static final String CREDENTIAL_INFO_USER_NAME_LABEL = "username";
	public static final String CREDENTIAL_INFO_USER_NAME = "dba";
	public static final String CREDENTIAL_INFO_USER_PASSWORD_LABEL = "password";
	public static final String CREDENTIAL_INFO_JDBC_URL_LABEL = "jdbcUrl";

	/* findServiceInstanceInfo */
	private static final String SELECT_SERVICE_INSTANCE_INFO = "/* SELECT_SERVICE_INSTANCE_INFO */ "
			+ " SELECT [service_instance_id], [Service_definition_id], [plan_id], [organization_guid], [space_guid]"
			+ " FROM [service_instance_info]" + " WHERE [service_instance_id] = ?";

	/* findServiceBindInfo */
	private static final String SELECT_SERVICE_BIND_INFO = "/* SELECT_SERVICE_BIND_INFO */"
			+ " SELECT [service_instance_bind_id], [service_instance_id], [application_id]"
			+ " FROM [service_bind_info]" + " WHERE [service_instance_bind_id] = ?";

	/* isExistsServiceBind */
	private static final String SELECT_SERVICE_BIND = "/* SELECT_SERVICE_BIND */"
			+ " SELECT count([service_instance_bind_id])" + " FROM [service_bind_info]"
			+ " WHERE [service_instance_id] = ?";

	/* deleteServiceInstanceInfo */
	private static final String DELETE_SERVICE_INSTANCE_INFO = "/* DELETE_SERVICE_INSTANCE_INFO */ " + " DELETE"
			+ " FROM [service_instance_info]" + " WHERE [service_instance_id] = ?";

	/* deleteServiceBindInfo */
	private static final String DELETE_SERVICE_BIND_INFO = "/* DELETE_SERVICE_BIND_INFO */" + " DELETE"
			+ " FROM [service_bind_info]" + " WHERE [service_instance_bind_id] = ?";

	/* save */
	private static final String INSERT_SERVICE_INSTANCE_INFO = "/* INSERT_SERVICE_INSTANCE_INFO */"
			+ " INSERT INTO [service_instance_info] ([service_instance_id], [service_definition_id], [plan_id], [organization_guid], [space_guid])"
			+ " VALUES (?, ?, ?, ?, ?)";

	/* saveBind */
	private static final String INSERT_SERVICE_BIND_INFO = "/* INSERT_SERVICE_INSTANCE_BIND_INFO */"
			+ " INSERT INTO [service_bind_info] ([service_instance_bind_id], [service_instance_id], [application_id], [username], [password])"
			+ " VALUES (?, ?, ?, ?, ?)";

	@Autowired
	private KubernetesAdminService kubernetesAdminService;

	@Autowired
	private JSchUtil jsch;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public ServiceInstance findServiceInstanceInfo(String serviceInstanceId) {
		try {
			return jdbcTemplate.queryForObject(SELECT_SERVICE_INSTANCE_INFO, new RowMapper<ServiceInstance>() {
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

	public ServiceInstanceBinding findServiceBindInfo(String serviceInstanceBindId) {
		try {
			return jdbcTemplate.queryForObject(SELECT_SERVICE_BIND_INFO, new RowMapper<ServiceInstanceBinding>() {
				public ServiceInstanceBinding mapRow(ResultSet rs, int rowNum) throws SQLException {
					return new ServiceInstanceBinding(rs.getString("service_instance_bind_id"),
							rs.getString("service_instance_id"),
							getCredentialsInfo(rs.getString("service_instance_id")), "",
							rs.getString("application_id"));
				}
			}, serviceInstanceBindId);
		} catch (DataAccessException e) {
			logger.warn("BIND ID : " + serviceInstanceBindId + " 인 Service Instance Bind 정보가 존재하지 않습니다.");

			return null;
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

	public void deleteServiceInstanceInfo(String serviceInstanceId) {
		try {
			this.jdbcTemplate.update(DELETE_SERVICE_INSTANCE_INFO, new Object[] { serviceInstanceId });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
	}

	public void deleteServiceBindInfo(String serviceInstanceBindId) {
		try {
			this.jdbcTemplate.update(DELETE_SERVICE_BIND_INFO, new Object[] { serviceInstanceBindId });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
	}

	public void save(ServiceInstance serviceInstance) {
		try {
			this.jdbcTemplate.update(INSERT_SERVICE_INSTANCE_INFO,
					new Object[] { serviceInstance.getServiceInstanceId(), serviceInstance.getServiceDefinitionId(),
							serviceInstance.getPlanId(), serviceInstance.getOrganizationGuid(),
							serviceInstance.getSpaceGuid() });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
	}

	public void saveBind(ServiceInstanceBinding serviceInstanceBinding) {
		try {
			this.jdbcTemplate.update(INSERT_SERVICE_BIND_INFO,
					new Object[] { serviceInstanceBinding.getId(), serviceInstanceBinding.getServiceInstanceId(),
							serviceInstanceBinding.getAppGuid(),
							serviceInstanceBinding.getCredentials().get("username"),
							serviceInstanceBinding.getCredentials().get("password") });
		} catch (DataAccessException e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
	}

	public void createKubernetesResources(ServiceInstance serviceInstance) {
		String serviceInstanceId = serviceInstance.getServiceInstanceId();

		if (this.createNFSUserDirectory(serviceInstanceId) == 0) {
			logger.info("Created NFS User Directory : " + serviceInstanceId);

			kubernetesAdminService.createKubernetesResources(serviceInstanceId, serviceInstance.getPlanId(),
					this.getDatabaseName(serviceInstanceId), this.getPassword(serviceInstanceId));
		}
	}

	public void deleteKubernetesResources(String serviceInstanceId) {
		kubernetesAdminService.deleteKubernetesResources(serviceInstanceId);

		if (this.deleteNFSUserDirectory(serviceInstanceId) == 0) {
			logger.info("Deleted NFS User Directory : " + serviceInstanceId);
		}
	}

	public Map<String, Object> getCredentialsInfo(String serviceInstanceId) {
		Map<String, Object> credentials = new HashMap<String, Object>();
		Map<String, String> servicePorts = kubernetesAdminService.getServicePort(serviceInstanceId);

		credentials.put(CUBRIDAdminService.CREDENTIAL_INFO_CUBRID_MANAGER_LABEL,
				servicePorts.get(kubernetesAdminService.CUBRID_MANAGER_LABEL));
		credentials.put(CUBRIDAdminService.CREDENTIAL_INFO_CUBRID_QUERY_EDITOR_LABEL,
				servicePorts.get(kubernetesAdminService.CUBRID_QUERY_EDITOR_LABEL));
		credentials.put(CUBRIDAdminService.CREDENTIAL_INFO_CUBRID_BROKER1_LABEL,
				servicePorts.get(kubernetesAdminService.CUBRID_BROKER1_LABEL));
		credentials.put(CUBRIDAdminService.CREDENTIAL_INFO_HOST_NAME_LABEL, kubernetesAdminService.getServiceIP());
		credentials.put(CUBRIDAdminService.CREDENTIAL_INFO_DATABASE_NAME_LABEL,
				this.getDatabaseName(serviceInstanceId));
		credentials.put(CUBRIDAdminService.CREDENTIAL_INFO_USER_NAME_LABEL,
				CUBRIDAdminService.CREDENTIAL_INFO_USER_NAME);
		credentials.put(CUBRIDAdminService.CREDENTIAL_INFO_USER_PASSWORD_LABEL, this.getPassword(serviceInstanceId));
		credentials.put(CUBRIDAdminService.CREDENTIAL_INFO_JDBC_URL_LABEL,
				"jdbc:cubrid:" + credentials.get(CUBRIDAdminService.CREDENTIAL_INFO_HOST_NAME_LABEL) + ":"
						+ credentials.get(CUBRIDAdminService.CREDENTIAL_INFO_CUBRID_BROKER1_LABEL) + ":"
						+ credentials.get(CUBRIDAdminService.CREDENTIAL_INFO_DATABASE_NAME_LABEL) + ":"
						+ credentials.get(CUBRIDAdminService.CREDENTIAL_INFO_USER_NAME_LABEL) + ":"
						+ credentials.get(CUBRIDAdminService.CREDENTIAL_INFO_USER_PASSWORD_LABEL) + ":");

		return credentials;
	}

	private String getDatabaseName(String serviceInstanceId) {
		return serviceInstanceId.substring(serviceInstanceId.length() - 12, serviceInstanceId.length());
	}

	private String getPassword(String serviceInstanceId) {
		MessageDigest messageDigest = null;

		try {
			messageDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		messageDigest.update(serviceInstanceId.getBytes());

		return new BigInteger(1, messageDigest.digest()).toString(32);
	}

	private int createNFSUserDirectory(String serviceInstanceId) {
		logger.info("Creating NFS User Directory : " + serviceInstanceId + "...");

		List<String> result = null;

		jsch.connect();
		result = jsch.execute("sudo mkdir -p /opt/nfs/data_001/" + serviceInstanceId + " && echo $?");
		jsch.disconnect();

		return result.size() > 0 ? Integer.valueOf(result.get(0)) : -1;
	}

	private int deleteNFSUserDirectory(String serviceInstanceId) {
		logger.info("Deleting NFS User Directory : " + serviceInstanceId + "...");

		List<String> result = null;

		jsch.connect();
		result = jsch.execute("sudo rm -rf /opt/nfs/data_001/" + serviceInstanceId);
		jsch.disconnect();

		return result.size() > 0 ? Integer.valueOf(result.get(0)) : -1;
	}
}
