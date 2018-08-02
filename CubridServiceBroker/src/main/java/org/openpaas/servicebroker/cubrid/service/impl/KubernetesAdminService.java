package org.openpaas.servicebroker.cubrid.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.PersistentVolumeBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

// @Service
@org.springframework.stereotype.Service
public class KubernetesAdminService {
	private Logger logger = LoggerFactory.getLogger(KubernetesAdminService.class);

	private KubernetesClient kubernetesClient = new DefaultKubernetesClient();

	/* Kubernetes Namespace */
	private final String KUBERNETES_NAMESPACE = "default";
	
	/* Kubernetes Service */
	private final String KUBERNETES_SERVICE_IP = "10.96.2.10";
	private final String KUBERNETES_SERVICE_TYPE_NODE_PORT_LABEL = "NodePort";
	private final String KUBERNETES_ADD_PORT_PROTOCOL_LABEL = "TCP";
	
	/* Kubernetes Deployment */
	private final String KUBERNETES_DEPLOYMENT_LABEL = "deployment";
	
	/* Kubernetes Container & Pod */
	private final String KUBERNETES_IMAGE_NAME = "local/cubrid";
	private final String KUBERNETES_IMAGE_PULL_POLICY_NEVER_LABEL = "Never"; /* Search in local */
	private final int KUBERNETES_WAIT_POD_TIMEOUT = 60; /* 5 * 60 = 5 Min */
	private final int KUBERNETES_WAIT_POD_DELAY = 5000; /* 5 Sec */
	
	/* Kubernetes PersistentVolumeClaim & PersistentVolume */
	private final String KUBERNETES_STORAGE_CLASS_MANUAL_LABEL = "manual";
	private final String KUBERNETES_STORAGE_LABEL = "storage";
	private final String KUBERNETES_READ_WRITE_MANY_LABEL = "ReadWriteMany";
	private final String KUBERNETES_DEFAULT_QUANTITY = "10Gi";
	private final String KUBERNETES_QUANTITY_512M = "2Gi"; /* D:512M, T:256M, AT:256M, AR:256M, BK < 128M = 1.5G + @*/
	private final String KUBERNETES_QUANTITY_1G5 = "3Gi";  /* D:1.5G, T:256M, AT:256M, AR:256M, BK < 256M = 2.5G + @*/
	private final String KUBERNETES_QUANTITY_2G5 = "5Gi";  /* D:2.5G, T:512M, AT:256M, AR:256M, BK < 512M = 4G + @*/
	private final String KUBERNETES_QUANTITY_4G5 = "7Gi";  /* D:4.5G, T:1G, AT:256M, AR:256M, BK < 1G = 6G + @*/

	/* NFS */
	private final String NFS_SERVER_IP = "10.96.2.100";
	private final String NFS_SERVER_PATH = "/opt/nfs/data_001";
	
	/* CUBRID */
	private final String CUBRID_DATABASES_PATH = "/home/cubrid/CUBRID/databases";
	private final int CUBRID_MANAGER_PORT = 8001;
	private final int CUBRID_QUERY_EDITOR_PORT = 30000;
	private final int CUBRID_BROKER1_PORT = 33000;
	public final String CUBRID_MANAGER_LABEL = "cubridmanager"; /* open */
	public final String CUBRID_QUERY_EDITOR_LABEL = "queryeditor"; /* open */
	public final String CUBRID_BROKER1_LABEL = "broker1"; /* open */

	public void createKubernetesResources(String serviceInstanceId, String plan_id, String databaseName, String password) {
		logger.info("Creating kubernetes resources : " + serviceInstanceId + "...");

		this.createPersistentVolume(serviceInstanceId, plan_id);
		this.createPersistentVolumeClaim(serviceInstanceId, plan_id);
		this.createDeployments(serviceInstanceId, plan_id, databaseName, password);
		this.createServices(serviceInstanceId);
		
		logger.info("Created kubernetes resources : " + serviceInstanceId);
	}

	public void deleteKubernetesResources(String serviceInstanceId) {
		logger.info("Deleting kubernetes resources : " + serviceInstanceId + "...");

		this.deleteServices(serviceInstanceId);
		this.deleteDeployments(serviceInstanceId);
		this.deletePersistentVolumeClaim(serviceInstanceId);
		this.deletePersistentVolume(serviceInstanceId);
		
		logger.info("Deleted kubernetes resources : " + serviceInstanceId);
	}
	
	public String getServiceIP() {
		logger.info("Getting kubernetes service iP : " + this.KUBERNETES_SERVICE_IP + "...");
		
		return this.KUBERNETES_SERVICE_IP;
	}
	
	public Map<String, String> getServicePort(String serviceInstanceId) {
		logger.info("Getting kubernetes service ports : " + serviceInstanceId);
		
		List<ServicePort> servicePorts = kubernetesClient.services().inNamespace(this.KUBERNETES_NAMESPACE).withName(this.getServiceName(serviceInstanceId)).get().getSpec().getPorts();
		
		Map<String, String> exposedPorts = new HashMap<String, String>();
		
		for (ServicePort servicePort : servicePorts) {
			String portName = servicePort.getName();
            String nodePort = String.valueOf(servicePort.getNodePort());
            
            exposedPorts.put(portName, nodePort);
            
            logger.info("Get kubernetes service ports : " + portName + "(" + nodePort + ")");
        }
		
		return exposedPorts;
	}
	
	private boolean createServices(String serviceInstanceId) {
		logger.info("Creating kubernetes services : " + serviceInstanceId + "...");
		
		boolean result = false;
		
		Map<String, String> selector = Collections.singletonMap(this.KUBERNETES_DEPLOYMENT_LABEL, serviceInstanceId);
		
		Service service = new ServiceBuilder()
				.withNewMetadata()
					.withName(this.getServiceName(serviceInstanceId))
					.withNamespace(this.KUBERNETES_NAMESPACE)
				.endMetadata()
				.withNewSpec()
					.addNewPort()
						.withName(this.CUBRID_MANAGER_LABEL)
						.withPort(this.CUBRID_MANAGER_PORT)
						.withProtocol(this.KUBERNETES_ADD_PORT_PROTOCOL_LABEL)
					.endPort()
					.addNewPort()
						.withName(this.CUBRID_QUERY_EDITOR_LABEL)
						.withPort(this.CUBRID_QUERY_EDITOR_PORT)
						.withProtocol(this.KUBERNETES_ADD_PORT_PROTOCOL_LABEL)
					.endPort()
					.addNewPort()
						.withName(this.CUBRID_BROKER1_LABEL)
						.withPort(this.CUBRID_BROKER1_PORT)
						.withProtocol(this.KUBERNETES_ADD_PORT_PROTOCOL_LABEL)
					.endPort()
					.withSelector(selector)
					.withType(this.KUBERNETES_SERVICE_TYPE_NODE_PORT_LABEL)
				.endSpec()
				.build();
		
		try {
			result = kubernetesClient.services().inNamespace(this.KUBERNETES_NAMESPACE).create(service) != null ? true : false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (result) {
			logger.info("Created kubernetes services : " + serviceInstanceId);
		} else {
			logger.info("[Fail] Creating kubernetes services : " + serviceInstanceId);
		}
		
		return result;
	}
	
	private boolean deleteServices(String serviceInstanceId) {
		logger.info("Deleting kubernetes services : " + serviceInstanceId + "...");
		
		boolean result = false;
		
		Service service = kubernetesClient.services().inNamespace(this.KUBERNETES_NAMESPACE).withName(this.getServiceName(serviceInstanceId)).get();
		
		try {
			result = kubernetesClient.services().inNamespace(this.KUBERNETES_NAMESPACE).delete(service);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (result) {
			logger.info("Deleted kubernetes services : " + serviceInstanceId);
		} else {
			logger.info("[Fail] Deleting kubernetes services : " + serviceInstanceId);
		}
		
		return result;
	}
	
	private boolean createDeployments(String serviceInstanceId, String plan_id, String databaseName, String password) {
		logger.info("Creating deployments : " + serviceInstanceId + "...");
		
		boolean result = false;
		
		Map<String, String> selector = Collections.singletonMap(this.KUBERNETES_DEPLOYMENT_LABEL, serviceInstanceId);
		
		List<EnvVar> envList = new ArrayList<EnvVar>();
		envList.add(new EnvVarBuilder().withName("DATABASE_NAME").withValue(databaseName).build());
		envList.add(new EnvVarBuilder().withName("PLAN_ID").withValue(plan_id).build());
		envList.add(new EnvVarBuilder().withName("DBA_PASSWORD").withValue(password).build());
		
		Deployment deployment = new DeploymentBuilder()
				.withNewMetadata()
					.withName(serviceInstanceId)
					.withNamespace(this.KUBERNETES_NAMESPACE)
				.endMetadata()
				.withNewSpec()
					.withReplicas(1)
					.withNewSelector()
						.withMatchLabels(selector)
					.endSelector()
					.withNewTemplate()
						.withNewMetadata()
							.withLabels(selector)
						.endMetadata()
						.withNewSpec()
							.addNewContainer()
								.withName(serviceInstanceId)
								.withImage(this.KUBERNETES_IMAGE_NAME)
								.withEnv(envList)
								.withImagePullPolicy(this.KUBERNETES_IMAGE_PULL_POLICY_NEVER_LABEL)
								.addNewPort()
									.withContainerPort(this.CUBRID_MANAGER_PORT)
									.withName(this.CUBRID_MANAGER_LABEL)
								.endPort()
								.addNewPort()
									.withContainerPort(this.CUBRID_QUERY_EDITOR_PORT)
									.withName(this.CUBRID_QUERY_EDITOR_LABEL)
								.endPort()
									.addNewPort()
									.withContainerPort(this.CUBRID_BROKER1_PORT)
									.withName(this.CUBRID_BROKER1_LABEL)
								.endPort()
								.addNewVolumeMount()
									.withName(serviceInstanceId)
									.withMountPath(this.CUBRID_DATABASES_PATH)
								.endVolumeMount()
							.endContainer()
							.addNewVolume()
								.withName(serviceInstanceId)
								.withNewPersistentVolumeClaim()
									.withClaimName(serviceInstanceId)
								.endPersistentVolumeClaim()
							.endVolume()
						.endSpec()
					.endTemplate()
				.endSpec()
				.build();
		
		try {
			result = kubernetesClient.extensions().deployments().inNamespace(this.KUBERNETES_NAMESPACE).create(deployment) != null ? true : false;
		} catch (Exception e) {
			this.deletePersistentVolumeClaim(serviceInstanceId);
			this.deletePersistentVolume(serviceInstanceId);
			
			e.printStackTrace();
		}
		
		if (result) {
			logger.info("Created deployments : " + serviceInstanceId);
		} else {
			logger.info("[Fail] Creating deployments : " + serviceInstanceId);
		}
		
		return result;
	}
	
	private boolean deleteDeployments(String serviceInstanceId) {
		logger.info("Deleting deployments : " + serviceInstanceId + "...");
		
		boolean result = false;
		
		Deployment deployment = kubernetesClient.extensions().deployments().inNamespace(this.KUBERNETES_NAMESPACE).withName(serviceInstanceId).get();
		
		try {
			result = kubernetesClient.extensions().deployments().inNamespace(this.KUBERNETES_NAMESPACE).delete(deployment);
		} catch (Exception e) {
			e.printStackTrace();
		}

		logger.info("Deleting Pods : " + serviceInstanceId + "...");
		
		try {
			for (int waitCount = 0; waitCount < this.KUBERNETES_WAIT_POD_TIMEOUT; waitCount++) {
				logger.info("Waiting for pod to be deleted... (" + waitCount + "/" + this.KUBERNETES_WAIT_POD_TIMEOUT + ")");
				
				List<Pod> pods = kubernetesClient.pods().inNamespace(this.KUBERNETES_NAMESPACE).withLabel(this.KUBERNETES_DEPLOYMENT_LABEL, serviceInstanceId).list().getItems();

				if (pods.size() == 0) {
					logger.info("Deleted Pods : " + serviceInstanceId);
					break;
				}

				Thread.sleep(this.KUBERNETES_WAIT_POD_DELAY);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.info("Thread interrupted while cleaning up workspace");
		}
		
		if (result) {
			logger.info("Deleted deployments : " + serviceInstanceId);
		} else {
			logger.info("[Fail] Deleting deployments : " + serviceInstanceId);
		}
		
		return result;
	}

	private boolean createPersistentVolumeClaim(String serviceInstanceId, String plan_id) {
		logger.info("Creating persistent volume claim : " + serviceInstanceId + "...");
		
		boolean result = false; 
		
		String quantity = null;
		
		switch (plan_id) {
		case "512M":
			quantity = this.KUBERNETES_QUANTITY_512M;
			break;
		case "1.5G":
			quantity = this.KUBERNETES_QUANTITY_1G5;
			break;
		case "2.5G":
			quantity = this.KUBERNETES_QUANTITY_2G5;
			break;
		case "4.5G":
			quantity = this.KUBERNETES_QUANTITY_4G5;
			break;
		default:
			quantity = this.KUBERNETES_DEFAULT_QUANTITY;
			break;
		}
		
		PersistentVolumeClaim persistentVolumeClaim = new PersistentVolumeClaimBuilder()
				.withNewMetadata()
					.withName(serviceInstanceId)
				.endMetadata()
				.withNewSpec()
					.withStorageClassName(this.KUBERNETES_STORAGE_CLASS_MANUAL_LABEL)
					.withAccessModes(this.KUBERNETES_READ_WRITE_MANY_LABEL)
					.withNewResources()
						.addToRequests(this.KUBERNETES_STORAGE_LABEL, new Quantity(quantity))
					.endResources()
				.endSpec()
				.build();
		
		try {
			result = kubernetesClient.persistentVolumeClaims().inNamespace(this.KUBERNETES_NAMESPACE).create(persistentVolumeClaim) != null ? true : false;
		} catch (Exception e) {
			this.deletePersistentVolume(serviceInstanceId);
			
			e.printStackTrace();
		}
		
		if (result) {
			logger.info("Created persistent volume claim : " + serviceInstanceId);
		} else {
			logger.info("[Fail] Creating persistent volume claim : " + serviceInstanceId);
		}
		
		return result;
	}
	
	private boolean deletePersistentVolumeClaim(String serviceInstanceId) {
		logger.info("Deleting persistent volume claims : " + serviceInstanceId + "...");
		
		boolean result = false;
		
		PersistentVolumeClaim persistentVolumeClaim = kubernetesClient.persistentVolumeClaims().inNamespace(this.KUBERNETES_NAMESPACE).withName(serviceInstanceId).get();
		
		try {
			result = kubernetesClient.persistentVolumeClaims().inNamespace(this.KUBERNETES_NAMESPACE).delete(persistentVolumeClaim);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (result) {
			logger.info("Deleted persistent volume claims : " + serviceInstanceId);
		} else {
			logger.info("[Fail] Deleting persistent volume claims : " + serviceInstanceId);
		}
		
		return result;
	}
	
	private boolean createPersistentVolume(String serviceInstanceId, String plan_id) {
		logger.info("Creating persistent volume : " + serviceInstanceId + "...");
		
		boolean result = false;
		
		String quantity = null;
		
		switch (plan_id) {
		case "512M":
			quantity = this.KUBERNETES_QUANTITY_512M;
			break;
		case "1.5G":
			quantity = this.KUBERNETES_QUANTITY_1G5;
			break;
		case "2.5G":
			quantity = this.KUBERNETES_QUANTITY_2G5;
			break;
		case "4.5G":
			quantity = this.KUBERNETES_QUANTITY_4G5;
			break;
		default:
			quantity = this.KUBERNETES_DEFAULT_QUANTITY;
			break;
		}
		
		PersistentVolume persistentVolume = new PersistentVolumeBuilder()
				.withNewMetadata()
					.withName(serviceInstanceId)
				.endMetadata()
				.withNewSpec()
					.withStorageClassName(this.KUBERNETES_STORAGE_CLASS_MANUAL_LABEL)
					.withAccessModes(this.KUBERNETES_READ_WRITE_MANY_LABEL)
					.addToCapacity(this.KUBERNETES_STORAGE_LABEL, new Quantity(quantity))
					.withNewNfs()
						.withServer(this.NFS_SERVER_IP)
						.withPath(this.NFS_SERVER_PATH + "/" + serviceInstanceId)
					.endNfs()
				.endSpec()	
				.build();
		
		try {
			result = kubernetesClient.persistentVolumes().create(persistentVolume) != null ? true : false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (result) {
			logger.info("Created persistent volume : " + serviceInstanceId);
		} else {
			logger.info("[Fail] Creating persistent volume : " + serviceInstanceId);
		}
		
		return result;
	}
	
	private boolean deletePersistentVolume(String serviceInstanceId) {
		logger.info("Deleting persistent volume : " + serviceInstanceId + "...");
		
		boolean result = false;
		
		PersistentVolume persistentVolume = kubernetesClient.persistentVolumes().withName(serviceInstanceId).get();
		
		try {
			result = kubernetesClient.persistentVolumes().delete(persistentVolume);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (result) {
			logger.info("Deleted persistent volume : " + serviceInstanceId);
		} else {
			logger.info("[Fail] Deleting persistent volume : " + serviceInstanceId);
		}
		
		return result;
	}
	
	private String getServiceName(String serviceInstanceId) {
		return "srv-" + serviceInstanceId;
	}
}
