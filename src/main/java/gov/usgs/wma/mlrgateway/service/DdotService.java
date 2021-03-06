package gov.usgs.wma.mlrgateway.service;

import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.client.DdotClient;
import gov.usgs.wma.mlrgateway.controller.WorkflowController;
import gov.usgs.wma.mlrgateway.util.ClientErrorParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DdotService {

	private DdotClient ddotClient;
	private ClientErrorParser clientErrorParser;
	private Logger log = LoggerFactory.getLogger(DdotService.class);

	protected static final String STEP_NAME = "Ingest D dot File";
	protected static final String SUCCESS_MESSAGE = "D dot file parsed successfully.";
	protected static final String INTERNAL_ERROR_MESSAGE = "{\"error_message\": \"Unable to read ingestor output.\"}";
	protected static final String WRONG_FORMAT_MESSAGE = "{\"error_message\":\"Only accepting files with one transaction at this time.\",\"data\":%ddotResponse%}";

	@Autowired
	public DdotService(DdotClient ddotClient) {
		this.ddotClient = ddotClient;
	}

	public List<Map<String, Object>> parseDdot(MultipartFile file) throws HystrixBadRequestException {
		clientErrorParser = new ClientErrorParser();
		List<Map<String, Object>> ddots = null;
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<List<Map<String, Object>>> mapType = new TypeReference<List<Map<String, Object>>>() {};
		
		try {
			String ddotResponse = ddotClient.ingestDdot(file).getBody();
			ddots = mapper.readValue(ddotResponse, mapType);
		} catch (Exception e) {
			int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
			log.error(STEP_NAME + ": " + e.getMessage());
			if(e instanceof FeignBadResponseWrapper){
				WorkflowController.addWorkflowStepReport(new StepReport(STEP_NAME, ((FeignBadResponseWrapper)e).getStatus(), false, clientErrorParser.parseClientError("DdotClient#ingestDdot(MultipartFile)", ((FeignBadResponseWrapper)e).getBody())));
			} else if (e instanceof JsonParseException){
				WorkflowController.addWorkflowStepReport(new StepReport(STEP_NAME, status, false, INTERNAL_ERROR_MESSAGE));
			}	else {
				WorkflowController.addWorkflowStepReport(new StepReport(STEP_NAME, status, false, "{\"error_message\":\"" + e.getMessage() + "\"}"));
			}
			throw new FeignBadResponseWrapper(status, null, INTERNAL_ERROR_MESSAGE);
		}

		WorkflowController.addWorkflowStepReport(new StepReport(STEP_NAME, HttpStatus.SC_OK, true, SUCCESS_MESSAGE));
		return ddots;
	}
}
