package gov.usgs.wma.mlrgateway.controller;

import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;
import gov.usgs.wma.mlrgateway.GatewayReport;
import gov.usgs.wma.mlrgateway.SiteReport;
import gov.usgs.wma.mlrgateway.StepReport;
import gov.usgs.wma.mlrgateway.UserSummaryReport;
import gov.usgs.wma.mlrgateway.exception.InvalidEmailException;
import gov.usgs.wma.mlrgateway.service.NotificationService;
import gov.usgs.wma.mlrgateway.service.UserAuthService;
import gov.usgs.wma.mlrgateway.util.UserSummaryReportBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public abstract class BaseController {
	protected NotificationService notificationService;
	protected UserAuthService userAuthService;
	
	@Value("${notification.email.cc-list:}")
	private String notificationEmailCCListString;
	
	@Value("${environmentTier:}")
	protected String environmentTier;

	@Value("${uiHost:}")
	protected String uiDomainName;
	
	protected String SUBJECT_PREFIX = "[%environment%] MLR Report for ";
		
	private Logger log = LoggerFactory.getLogger(BaseController.class);
	private static ThreadLocal<GatewayReport> gatewayReport = new ThreadLocal<>();
	private UserSummaryReport userSummaryReport = new UserSummaryReport();
	private UserSummaryReportBuilder userSummaryReportBuilder = new UserSummaryReportBuilder();
	
	public BaseController() {};
	public BaseController(NotificationService notificationService, UserAuthService userAuthService) {
		this.notificationService = notificationService;
		this.userAuthService = userAuthService;
	}

	public static GatewayReport getReport() {
		return gatewayReport.get();
	}

	public static void setReport(GatewayReport report) {
		gatewayReport.set(report);
	}
	
	public static void addSiteReport(SiteReport siteReport) {
		GatewayReport report = gatewayReport.get();
		report.addSiteReport(siteReport);
		gatewayReport.set(report);
	}
	
	public static void addWorkflowStepReport(StepReport stepReport) {
		GatewayReport report = gatewayReport.get();
		report.addWorkflowStepReport(stepReport);
		gatewayReport.set(report);
	}

	public static void remove() {
		gatewayReport.remove();
	}

	protected void notificationStep(String subject, String attachmentFileName, Authentication authentication, Boolean includeCCList) {
		List<String> ccList = new ArrayList<>();;
		//Send Notification
		try {
			if(includeCCList && notificationEmailCCListString != null && !notificationEmailCCListString.trim().isEmpty()){
				//Note List returned from Arrays.asList does not implement .add() thus the need for the additional ArrayList<> constructor
				ccList = new ArrayList<>(Arrays.asList(StringUtils.split(notificationEmailCCListString.trim(), ',')));
			}
			
			String userEmail = userAuthService.getUserEmail(authentication);
				
			if(userEmail == null || userEmail.isEmpty()){
				throw new InvalidEmailException("Could not find valid user email in security context.");
			}

			String fullSubject = SUBJECT_PREFIX.replace("%environment%", environmentTier != null && environmentTier.length() > 0 ? environmentTier : "") + subject;
			userSummaryReport = userSummaryReportBuilder.buildUserSummaryReport(getReport());
			notificationService.sendNotification(userEmail, ccList, fullSubject, getReport().getUserName(), attachmentFileName, userSummaryReport);
		} catch(Exception e) {
			log.error("An error occurred while attempting to send the notification email: ", e);
			if (e instanceof FeignBadResponseWrapper) {
				int status = ((FeignBadResponseWrapper) e).getStatus();
				WorkflowController.addWorkflowStepReport(new StepReport(NotificationService.NOTIFICATION_STEP, status, false, ((FeignBadResponseWrapper) e).getBody()));
			} else {
				int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				WorkflowController.addWorkflowStepReport(new StepReport(NotificationService.NOTIFICATION_STEP, status, false, e.getMessage()));
			}
		}
	}
}
