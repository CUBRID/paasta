package org.openpaas.servicebroker.cubrid.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpaas.servicebroker.model.Catalog;
import org.openpaas.servicebroker.model.DashboardClient;
import org.openpaas.servicebroker.model.Plan;
import org.openpaas.servicebroker.model.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogConfig {
	

	@Bean
	public Catalog catalog() {
		return new Catalog(Arrays.asList(new ServiceDefinition("cubrid", // id*
			"CUBRID", // name*
			"CUBRID is engineered as a completely free, open-source relational database management engine, with built-in enterprise grade features.", // description*
			true, // bindable*
			false, // plan_updatable
			Arrays.asList(new Plan("512M", // plan > id*
					"512M", // plan > name*
					"CUBRID Database, Store up to 512M of data.", // plan > description*
					getPlanMetadata("512M"), // plan > metadata*
					true
			), new Plan("1.5G", // plan > id*
					"1.5G", // plan > name*
					"CUBRID Database, Store up to 1.5G of data.", // plan > description*
					getPlanMetadata("1.5G"), // plan > metadata*
					true
			), new Plan("2.5G", // plan > id*
					"2.5G", // plan > name*
					"CUBRID Database, Store up to 2.5G of data.", // plan > description*
					getPlanMetadata("2.5G"), // plan > metadata*
					true
			), new Plan("4.5G", // plan > id*
					"4.5G", // plan > name*
					"CUBRID Database, Store up to 4.5G of data.", // plan > description*
					getPlanMetadata("4.5G"), // plan > metadata*
					true
			)), // plans*
			Arrays.asList("cubrid", "opensource"), // tags
			getServiceDefinitionMetadata(), // metadata
			getRequires(), // requires
			// getDashboardClient() // dashboard_client
			null
		)));
	}

	private Map<String, Object> getServiceDefinitionMetadata() {
		Map<String, Object> serviceDefinitionMetadata = new HashMap<String, Object>();
		serviceDefinitionMetadata.put("displayName", "CUBRID DB"); // displayName
		serviceDefinitionMetadata.put("imageUrl", "http://www.cubrid.com/files/attach/images/3771164/522cf9a9415e01599545be25bfd8eab3.png"); // imageUrl
		serviceDefinitionMetadata.put("longDescription", "CUBRID is engineered as a completely free, open-source relational database management engine, with built-in enterprise grade features. It provides unique powerful features, such as object oriented database elements relations, data sharding, a native middleware broker, high performance data caching, customizable and extendible globalization support. Not the least, it provides a high level of SQL compatibility with MySQL and other known databases."); // longDescription
		serviceDefinitionMetadata.put("providerDisplayName", "CUBRID"); // providerDisplayName
		serviceDefinitionMetadata.put("documentationUrl", "https://www.cubrid.org/documentation/manuals/"); // documentationUrl
		serviceDefinitionMetadata.put("supportUrl", "http://www.cubrid.com"); // supportUrl
		return serviceDefinitionMetadata;
	}

	private Map<String, Object> getPlanMetadata(String planId) {
		Map<String, Object> planMetadata = new HashMap<String, Object>();
		planMetadata.put("bullets", getBullets(planId)); // bullets
		planMetadata.put("costs", getCosts(planId)); // costs
		
		if ("512M".equals(planId)) {
			planMetadata.put("displayName", "CUBRID Database, Store up to 512M of data."); // displayName
		} else if ("1.5G".equals(planId)) {
			planMetadata.put("displayName", "CUBRID Database, Store up to 1.5G of data."); // displayName
		} else if ("2.5G".equals(planId)) {
			planMetadata.put("displayName", "CUBRID Database, Store up to 2.5G of data."); // displayName
		} else if ("4.5G".equals(planId)) {
			planMetadata.put("displayName", "CUBRID Database, Store up to 4.5G of data."); // displayName
		}
		
		return planMetadata;
	}

	private List<String> getBullets(String planId) {
		if ("512M".equals(planId)) {
			return Arrays.asList("CUBRID Database, Store up to 512M of data.");
		} else if ("1.5G".equals(planId)) {
			return Arrays.asList("CUBRID Database, Store up to 1.5G of data.");
		} else if ("2.5G".equals(planId)) {
			return Arrays.asList("CUBRID Database, Store up to 2.5G of data.");
		} else if ("4.5G".equals(planId)) {
			return Arrays.asList("CUBRID Database, Store up to 4.5G of data.");
		} else {
			return Arrays.asList("Disabled");
		}
	}

	private List<Map<String, Object>> getCosts(String planId) {
		Map<String, Object> costs = new HashMap<String, Object>();
		Map<String, Object> amount = new HashMap<String, Object>();

		if ("512M".equals(planId)) {
			amount.put("won", new Integer(0));
			costs.put("amount", amount);
			costs.put("unit", "MONTHLY");
		} else if ("1.5G".equals(planId)) {
			amount.put("won", new Integer(0));
			costs.put("amount", amount);
			costs.put("unit", "MONTHLY");
		} else if ("2.5G".equals(planId)) {
			amount.put("won", new Integer(0));
			costs.put("amount", amount);
			costs.put("unit", "MONTHLY");
		} else if ("4.5G".equals(planId)) {
			amount.put("won", new Integer(0));
			costs.put("amount", amount);
			costs.put("unit", "MONTHLY");
		}

		return Arrays.asList(costs);
	}

	private List<String> getRequires() {
		/*
		return Arrays.asList("Windows 32/64 Bit XP, 2003, Vista, Windows 7",
			"Linux family 32/64 Bit(Linux kernel 2.4, glibc 2.3.4 or higher",
			"Requires a 500 MB of free disk space on the initial installation; requires approximately 1.5 GB of free disk space with a database creating with default options.",
			"JRE/JDK 1.6 or higher (Required when Java Stored Procedure is required");
			*/
		return Arrays.asList("syslog_drain");
	}

	/*
	private DashboardClient getDashboardClient() {
		return new DashboardClient(
			"CUBRID Manager", // id
			null, // secret
			"http://www.cubrid.com/downloads"); // redirect_uri
	}
	*/
}
