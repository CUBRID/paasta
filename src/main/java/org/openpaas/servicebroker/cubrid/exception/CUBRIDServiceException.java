package org.openpaas.servicebroker.cubrid.exception;

import org.openpaas.servicebroker.exception.ServiceBrokerException;

public class CUBRIDServiceException extends ServiceBrokerException {
	
	private static final long serialVersionUID = 8667141725171626000L;

	public CUBRIDServiceException(String message) {
		super(message);
	}
}
