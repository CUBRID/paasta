package org.openpaas.servicebroker.cubrid.service.impl;

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

	public ServiceInstance createServiceInstance(CreateServiceInstanceRequest createServiceInstanceRequest) throws ServiceInstanceExistsException, ServiceBrokerException {
		logger.info("Creating to service instance : " + createServiceInstanceRequest.getServiceInstanceId() + "...");
		
		ServiceInstance findServiceInstance = getServiceInstance(createServiceInstanceRequest.getServiceInstanceId());
		
		if (findServiceInstance != null) {
			if (createServiceInstanceRequest.getServiceInstanceId().equals(findServiceInstance.getServiceInstanceId())
					&& createServiceInstanceRequest.getPlanId().equals(findServiceInstance.getPlanId())
					&& createServiceInstanceRequest.getServiceDefinitionId().equals(findServiceInstance.getServiceDefinitionId())) {
				findServiceInstance.setHttpStatusOK();
				
				logger.warn("already exists same service instance : " + createServiceInstanceRequest.getServiceInstanceId());
				
				return findServiceInstance;
			} else {
				throw new ServiceInstanceExistsException(findServiceInstance);
			}
		} else { /* findServiceInstance == null */
			ServiceInstance createServiceInstance = new ServiceInstance(createServiceInstanceRequest);
			
			cubridAdminService.createKubernetesResources(createServiceInstance);
			cubridAdminService.save(createServiceInstance);
			
			logger.info("Created to service instance : " + createServiceInstanceRequest.getServiceInstanceId());

			return createServiceInstance;
		}
	}

	public ServiceInstance deleteServiceInstance(DeleteServiceInstanceRequest deleteServiceInstanceRequest) throws ServiceBrokerException {
		logger.info("Deleting to service instance : " + deleteServiceInstanceRequest.getServiceInstanceId() + "...");
		
		ServiceInstance findServiceInstance = getServiceInstance(deleteServiceInstanceRequest.getServiceInstanceId());

		if (findServiceInstance != null) {
			cubridAdminService.deleteServiceInstanceInfo(findServiceInstance.getServiceInstanceId());
			cubridAdminService.deleteKubernetesResources(findServiceInstance.getServiceInstanceId());
			
			logger.info("Deleted to service instance : " + deleteServiceInstanceRequest.getServiceInstanceId());
			
			return findServiceInstance;
		} else { /* findServiceInstance == null */
			throw new ServiceBrokerException("Not exists service instance : " + deleteServiceInstanceRequest.getServiceInstanceId());
		}
	}

	public ServiceInstance getServiceInstance(String serviceInstanceId) {
		return cubridAdminService.findServiceInstanceInfo(serviceInstanceId);
	}

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
