package org.openpaas.servicebroker.cubrid.exception;

import org.openpaas.servicebroker.exception.ServiceBrokerException;

@SuppressWarnings("serial")
public class CUBRIDServiceException extends ServiceBrokerException {
	
	public CUBRIDServiceException(String message) {
		super(message);
	}
}
