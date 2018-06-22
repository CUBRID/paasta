package org.openpaas.servicebroker.cubrid.service.impl;

import org.openpaas.servicebroker.exception.ServiceBrokerException;
import org.openpaas.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.openpaas.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.openpaas.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.openpaas.servicebroker.model.ServiceInstance;
import org.openpaas.servicebroker.model.ServiceInstanceBinding;
import org.openpaas.servicebroker.service.ServiceInstanceBindingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CubridServiceInstanceBindingService implements ServiceInstanceBindingService {
	
	private static final Logger logger = LoggerFactory.getLogger(CubridServiceInstanceBindingService.class);
	
	@Autowired
	private CUBRIDAdminService cubridAdminService;

	@Autowired
	public CubridServiceInstanceBindingService(CUBRIDAdminService cubridAdminService) {
		this.cubridAdminService = cubridAdminService;
	}

	@Override
	public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest) throws ServiceInstanceBindingExistsException, ServiceBrokerException {
		logger.info("Creating bind to service instance : " + createServiceInstanceBindingRequest.getBindingId());
		
		ServiceInstanceBinding findServiceInstanceBinding = cubridAdminService.findServiceBindInfo(createServiceInstanceBindingRequest.getBindingId());
		
		if (findServiceInstanceBinding != null) {
			if (createServiceInstanceBindingRequest.getBindingId().equals(findServiceInstanceBinding.getId())
					&& createServiceInstanceBindingRequest.getServiceInstanceId().equals(findServiceInstanceBinding.getServiceInstanceId())
					&& createServiceInstanceBindingRequest.getAppGuid().equals(findServiceInstanceBinding.getAppGuid())) {
				findServiceInstanceBinding.setHttpStatusOK();
				
				return findServiceInstanceBinding;
			} else {
				throw new ServiceInstanceBindingExistsException(findServiceInstanceBinding);
			}
		} else { /* findServiceInstanceBinding == null */
			ServiceInstance findServiceInstance = cubridAdminService.findServiceInstanceInfo(createServiceInstanceBindingRequest.getServiceInstanceId());
			
			if (findServiceInstance != null) {
				ServiceInstanceBinding createServiceInstanceBinding = new ServiceInstanceBinding(createServiceInstanceBindingRequest.getBindingId(),
						createServiceInstanceBindingRequest.getServiceInstanceId(),
						cubridAdminService.createCredentialsInfo(createServiceInstanceBindingRequest.getBindingId(), createServiceInstanceBindingRequest.getServiceInstanceId()),
						"",
						createServiceInstanceBindingRequest.getAppGuid()
					);
				
				cubridAdminService.saveBind(createServiceInstanceBinding);
			
				return createServiceInstanceBinding;
			} else { /* (findServiceInstanceBinding == null) && (findServiceInstance == null) */
				throw new ServiceBrokerException("Not Exists ServiceInstance.");
			}
		}
	}
	
	@Override
	public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest deleteServiceInstanceBindingRequest) throws ServiceBrokerException {
		logger.info("Deleting bind to service instance : " + deleteServiceInstanceBindingRequest.getBindingId());
		
		ServiceInstanceBinding findServiceInstanceBinding = cubridAdminService.findServiceBindInfo(deleteServiceInstanceBindingRequest.getBindingId());
		
		if (findServiceInstanceBinding != null) {
			ServiceInstance findServiceInstance = cubridAdminService.findServiceInstanceInfo(findServiceInstanceBinding.getServiceInstanceId());
			
			if (findServiceInstance != null) {
				// cubridAdminService.deleteUser(findServiceInstance.getServiceInstanceId(), findServiceInstanceBinding.getId());
				cubridAdminService.deleteServiceBindInfo(findServiceInstanceBinding.getId());
			} else { /* (findServiceInstanceBinding != null) && (findServiceInstance != null) */
				return null;
			}
		} else { /* findServiceInstanceBinding == null */
			return null;
		}
		
		return findServiceInstanceBinding;
	}
}
