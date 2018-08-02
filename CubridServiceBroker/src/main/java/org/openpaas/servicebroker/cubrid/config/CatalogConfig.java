package org.openpaas.servicebroker.cubrid.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpaas.servicebroker.model.Catalog;
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
					"512M of data can be stored and temporary results can be generated up to 256M. The number of connections can be from 5 to 5 in the QUERY_EDITOR broker and from 5 to 40 in the BROKER1 broker.", // plan > description*
					getPlanMetadata("512M"), // plan > metadata*
					true // Plan > isFree
			), new Plan("1.5G", // plan > id*
					"1.5G", // plan > name*
					"1.5G of data can be stored and temporary results can be generated up to 256M. The number of connections can be from 5 to 5 in the QUERY_EDITOR broker and from 5 to 40 in the BROKER1 broker.", // plan > description*
					getPlanMetadata("1.5G"), // plan > metadata*
					true // Plan > isFree
			), new Plan("2.5G", // plan > id*
					"2.5G", // plan > name*
					"2.5G of data can be stored and temporary results can be generated up to 512M. The number of connections can be from 5 to 5 in the QUERY_EDITOR broker and from 5 to 40 in the BROKER1 broker.", // plan > description*
					getPlanMetadata("2.5G"), // plan > metadata*
					true // Plan > isFree
			), new Plan("4.5G", // plan > id*
					"4.5G", // plan > name*
					"4.5G of data can be stored and temporary results can be generated up to 1G. The number of connections can be from 5 to 5 in the QUERY_EDITOR broker and from 20 to 80 in the BROKER1 broker.", // plan > description*
					getPlanMetadata("4.5G"), // plan > metadata*
					true // Plan > isFree
			)), // plans*
			Arrays.asList("cubrid", "opensource"), // tags
			getServiceDefinitionMetadata(), // metadata
			null, // getRequires(), // requires
			null // getDashboardClient() // dashboard_client
		)));
	}

	private Map<String, Object> getServiceDefinitionMetadata() {
		Map<String, Object> serviceDefinitionMetadata = new HashMap<String, Object>();
		serviceDefinitionMetadata.put("displayName", "CUBRID"); // displayName
		serviceDefinitionMetadata.put("imageUrl", "http://www.cubrid.com/files/attach/images/3771164/522cf9a9415e01599545be25bfd8eab3.png"); // imageUrl
		serviceDefinitionMetadata.put("longDescription", "CUBRID is engineered as a completely free, open-source relational database management engine, with built-in enterprise grade features. It provides unique powerful features, such as object oriented database elements relations, data sharding, a native middleware broker, high performance data caching, customizable and extendible globalization support. Not the least, it provides a high level of SQL compatibility with MySQL and other known databases."); // longDescription
		serviceDefinitionMetadata.put("providerDisplayName", "CUBRID"); // providerDisplayName
		serviceDefinitionMetadata.put("documentationUrl", "https://www.cubrid.org/documentation/manuals/"); // documentationUrl
		serviceDefinitionMetadata.put("supportUrl", "http://www.cubrid.com"); // supportUrl
		return serviceDefinitionMetadata;
	}
	
	/*
	 * -----------------------------------------------------------
	 * | Plan ID | Generic | Data, Index | Temp | Temporary Temp |
	 * -----------------------------------------------------------
	 * | 512M    | 512M    | 0           | 256M | 0              |
	 * | 1.5G    | 512M    | 1G          | 256M | 0              |
	 * | 2.5G    | 512M    | 2G          | 512M | 0              |
	 * | 4.5G    | 512M    | 4G (2G*2)   | 1G   | 0              |
	 * -----------------------------------------------------------
	 */

	private Map<String, Object> getPlanMetadata(String planId) {
		Map<String, Object> planMetadata = new HashMap<String, Object>();
		planMetadata.put("bullets", getBullets(planId)); // bullets
		planMetadata.put("costs", getCosts(planId)); // costs
		
		/*
		if ("512M".equals(planId)) {
			planMetadata.put("displayName", "CUBRID Database"); // displayName
		} else if ("1.5G".equals(planId)) {
			planMetadata.put("displayName", "CUBRID Database"); // displayName
		} else if ("2.5G".equals(planId)) {
			planMetadata.put("displayName", "CUBRID Database"); // displayName
		} else if ("4.5G".equals(planId)) {
			planMetadata.put("displayName", "CUBRID Database"); // displayName
		}
		*/
		
		return planMetadata;
	}

	private List<String> getBullets(String planId) {
		
		Arrays.asList("CUBRID Database");
		
		if ("512M".equals(planId)) {
			return Arrays.asList("512M of data can be stored and temporary results can be generated up to 256M.", "The number of connections can be from 5 to 5 in the QUERY_EDITOR broker and from 5 to 40 in the BROKER1 broker.");
		} else if ("1.5G".equals(planId)) {
			return Arrays.asList("1.5G of data can be stored and temporary results can be generated up to 256M.", "The number of connections can be from 5 to 5 in the QUERY_EDITOR broker and from 5 to 40 in the BROKER1 broker.");
		} else if ("2.5G".equals(planId)) {
			return Arrays.asList("2.5G of data can be stored and temporary results can be generated up to 512M.", "The number of connections can be from 5 to 5 in the QUERY_EDITOR broker and from 5 to 40 in the BROKER1 broker.");
		} else if ("4.5G".equals(planId)) {
			return Arrays.asList("4.5G of data can be stored and temporary results can be generated up to 1G.", "The number of connections can be from 5 to 5 in the QUERY_EDITOR broker and from 20 to 80 in the BROKER1 broker.");
		} else {
			return Arrays.asList("Disabled");
		}
	}

	private List<Map<String, Object>> getCosts(String planId) {
		Map<String, Object> costs = new HashMap<String, Object>();
		Map<String, Object> amount = new HashMap<String, Object>();

		if ("512M".equals(planId)) {
			amount.put("KRW", 0.0);
		} else if ("1.5G".equals(planId)) {
			amount.put("KRW", 0.0);
		} else if ("2.5G".equals(planId)) {
			amount.put("KRW", 0.0);
		} else if ("4.5G".equals(planId)) {
			amount.put("KRW", 0.0);
		}
		
		amount.put("KRW", 0.0);
		costs.put("amount", amount);
		costs.put("unit", "MONTHLY");

		return Arrays.asList(costs);
	}
}
