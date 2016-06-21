package com.googlecode.jsonrpc4j.spring.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.UnknownHttpStatusCodeException;

public enum JsonRpcResponseErrorHandler
	implements ResponseErrorHandler {

	INSTANCE;
	
	/**
	 * for supported codes see {@link com.googlecode.jsonrpc4j.DefaultHttpStatusCodeProvider}
	 */
	private final Set<Integer> JSON_RPC_STATUES = new HashSet<Integer>();
	
	
	private JsonRpcResponseErrorHandler()
	{
		JSON_RPC_STATUES.add(HttpURLConnection.HTTP_INTERNAL_ERROR);
		JSON_RPC_STATUES.add(HttpURLConnection.HTTP_BAD_REQUEST);
		JSON_RPC_STATUES.add(HttpURLConnection.HTTP_NOT_FOUND);
	}
	
	@Override
	public boolean hasError(ClientHttpResponse response)
		throws IOException
	{
		final HttpStatus httpStatus = getHttpStatusCode(response);
		
		if (JSON_RPC_STATUES.contains(httpStatus.value()))
			return false;
		else 
			return 
				httpStatus.series() == HttpStatus.Series.CLIENT_ERROR ||
				httpStatus.series() == HttpStatus.Series.SERVER_ERROR;
	}

	@Override
	public void handleError(ClientHttpResponse response)
		throws IOException
	{
		final HttpStatus statusCode = getHttpStatusCode(response);
		
		switch (statusCode.series()) {
			case CLIENT_ERROR:
				throw new HttpClientErrorException(statusCode, response.getStatusText(),
						response.getHeaders(), getResponseBody(response), getCharset(response));
			case SERVER_ERROR:
				throw new HttpServerErrorException(statusCode, response.getStatusText(),
						response.getHeaders(), getResponseBody(response), getCharset(response));
			default:
				throw new RestClientException("Unknown status code [" + statusCode + "]");
		}
	}
	
	
	private HttpStatus getHttpStatusCode(ClientHttpResponse response) throws IOException {
		final HttpStatus statusCode;
		try {
			statusCode = response.getStatusCode();
		}
		catch (IllegalArgumentException ex) {
			throw new UnknownHttpStatusCodeException(response.getRawStatusCode(),
					response.getStatusText(), response.getHeaders(), getResponseBody(response), getCharset(response));
		}
		return statusCode;
	}

	private byte[] getResponseBody(ClientHttpResponse response) {
		try {
			final InputStream responseBody = response.getBody();
			if (responseBody != null) {
				return FileCopyUtils.copyToByteArray(responseBody);
			}
		}
		catch (IOException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
		return new byte[0];
	}

	private Charset getCharset(ClientHttpResponse response) {
		HttpHeaders headers = response.getHeaders();
		MediaType contentType = headers.getContentType();
		return contentType != null ? contentType.getCharSet() : null;
	}
	
}
