package org.openpaas.servicebroker.cubrid.service.impl;

import java.util.Map;
import java.util.UUID;

import org.openpaas.servicebroker.exception.ServiceBrokerException;
import org.openpaas.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.openpaas.servicebroker.exception.ServiceInstanceExistsException;
import org.openpaas.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.openpaas.servicebroker.model.CreateServiceInstanceRequest;
import org.openpaas.servicebroker.model.DeleteServiceInstanceRequest;
import org.openpaas.servicebroker.model.ServiceInstance;
import org.openpaas.servicebroker.model.UpdateServiceInstanceRequest;
import org.openpaas.servicebroker.service.ServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CubridServiceInstanceService implements ServiceInstanceService {
	
	private static final Logger logger = LoggerFactory.getLogger(CubridServiceInstanceService.class);
	
	@Autowired
	private CUBRIDAdminService cubridAdminService;
	
	@Autowired
	public CubridServiceInstanceService(CUBRIDAdminService cubridAdminService) {
		this.cubridAdminService = cubridAdminService;
	}

	@Override
	public ServiceInstance createServiceInstance(CreateServiceInstanceRequest createServiceInstanceRequest) throws ServiceInstanceExistsException, ServiceBrokerException {
		logger.info("Creating to service instance : " + createServiceInstanceRequest.getServiceInstanceId());
		
		ServiceInstance findServiceInstance = getServiceInstance(createServiceInstanceRequest.getServiceInstanceId());
		
		if (findServiceInstance != null) {
			if (createServiceInstanceRequest.getServiceInstanceId().equals(findServiceInstance.getServiceInstanceId())
					&& createServiceInstanceRequest.getPlanId().equals(findServiceInstance.getPlanId())
					&& createServiceInstanceRequest.getServiceDefinitionId().equals(findServiceInstance.getServiceDefinitionId())) {
				findServiceInstance.setHttpStatusOK();
				
				return findServiceInstance;
			} else {
				throw new ServiceInstanceExistsException(findServiceInstance);
			}
		} else { /* findServiceInstance == null */
			ServiceInstance createServiceInstance = new ServiceInstance(createServiceInstanceRequest);
			
			if (cubridAdminService.isExistsServiceBind(createServiceInstance.getServiceInstanceId())) {
				cubridAdminService.deleteServiceBindInfo(createServiceInstance.getServiceInstanceId());
			}
			
			String dockerNodeIp = cubridAdminService.isExistsContainerInfo(createServiceInstance.getServiceInstanceId());
			if (dockerNodeIp != null) {
				cubridAdminService.deleteContainerInfo(createServiceInstance.getServiceInstanceId());
				cubridAdminService.deleteDockerService(createServiceInstance.getServiceInstanceId(), dockerNodeIp);
			}
			
			if (cubridAdminService.isExistsUsingServicePort(createServiceInstance.getServiceInstanceId())) {
				cubridAdminService.deleteUsingServicePort(createServiceInstance.getServiceInstanceId());
			}
			
			Map<String, String> dockerServiceInfo = cubridAdminService.createDockerService(createServiceInstance);
						
			if (dockerServiceInfo == null) {
				logger.error("Failed to create new docker service.");
				
				return null;
			}
			
			cubridAdminService.save(createServiceInstance, dockerServiceInfo);

			return createServiceInstance;
		}
	}

	@Override
	public ServiceInstance deleteServiceInstance(DeleteServiceInstanceRequest deleteServiceInstanceRequest) throws ServiceBrokerException {
		logger.info("Deleting to service instance : " + deleteServiceInstanceRequest.getServiceInstanceId());
		
		ServiceInstance findServiceInstance = getServiceInstance(deleteServiceInstanceRequest.getServiceInstanceId());

		if (findServiceInstance != null) {
			if (cubridAdminService.isExistsServiceBind(findServiceInstance.getServiceInstanceId())) {
				cubridAdminService.deleteServiceBind(findServiceInstance.getServiceInstanceId());
			}
			
			String dockerNodeIp = cubridAdminService.isExistsContainerInfo(findServiceInstance.getServiceInstanceId());
			if (dockerNodeIp != null) {
				cubridAdminService.deleteContainerInfo(findServiceInstance.getServiceInstanceId());
				cubridAdminService.deleteDockerService(findServiceInstance.getServiceInstanceId(), dockerNodeIp);
			}
			
			if (cubridAdminService.isExistsUsingServicePort(findServiceInstance.getServiceInstanceId())) {
				cubridAdminService.deleteUsingServicePort(findServiceInstance.getServiceInstanceId());
			}
			
			cubridAdminService.deleteServiceInstanceInfo(findServiceInstance.getServiceInstanceId());
			
			return findServiceInstance;
		} else { /* findServiceInstance == null */
			return null;
		}
	}

	@Override
	public ServiceInstance getServiceInstance(String serviceInstanceId) {
		return cubridAdminService.findServiceInstanceInfo(serviceInstanceId);
	}

	@Override
	public ServiceInstance updateServiceInstance(UpdateServiceInstanceRequest updateServiceInstanceRequest) throws ServiceInstanceUpdateNotSupportedException, ServiceBrokerException, ServiceInstanceDoesNotExistException {
		logger.info("Updating to service instance : " + updateServiceInstanceRequest.getServiceInstanceId());
		
		ServiceInstance findServiceInstance = getServiceInstance(updateServiceInstanceRequest.getServiceInstanceId());

		if (findServiceInstance != null) {
			throw new ServiceInstanceUpdateNotSupportedException("Not Supported.");
		} else { /* findServiceInstance == null */
			throw new ServiceInstanceDoesNotExistException(updateServiceInstanceRequest.getServiceInstanceId());
		}
	}
}
