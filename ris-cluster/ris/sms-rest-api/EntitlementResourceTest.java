package com.pearson.ed.ltg.sms.restApiTest.deleverageapi;

import com.pearson.ed.ltg.sms.domain.rest.deleverageapi.EntitlementResponse;
import com.pearson.ed.ltg.sms.domain.rest.deleverageapi.ProductIdentifierType;
import com.pearson.ed.ltg.sms.domain.rest.deleverageapi.SectionIdentifierType;
import com.pearson.ed.ltg.sms.domain.rest.deleverageapi.UserIdentifierType;
import com.pearson.ed.ltg.sms.rest.deleverageapi.EntitlementResource;
import com.pearson.ed.ltg.sms.rest.ex.NotFoundException;
import com.pearson.ed.ltg.sms.rest.pcom.pcomredirecturl.RedirectUrlServiceImpl;
import com.pearson.ed.ltg.sms.service.SmsSubscriptionService;
import com.pearson.ed.ltg.sms.service.SmsSubscriptionServiceIF;
import com.pearson.ed.ltg.sms.service.exception.BadRequestException;
import com.pearson.ed.ltg.sms.service.model.Subscription;
import com.pearson.ed.ltg.sms.service.spring.SmsServiceRegistry;
import com.pearson.ed.ltg.sms.service.spring.SpringApplicationContext;
import com.pearson.ed.pi.authentication.authenticator.TokenAuthenticator;
import com.pearson.ltg.reg.sms.mac.SmsProperties;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataRetrievalFailureException;

import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntitlementResourceTest {

    @InjectMocks
    private EntitlementResource entitlementResource;

    @Mock
    private SmsSubscriptionServiceIF smsSubscriptionService;

    @Mock
    private SmsProperties smsProperties;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    TokenAuthenticator tokenAuthenticator;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Use reflection to set the private CONTEXT field
        Field contextField = SpringApplicationContext.class.getDeclaredField("CONTEXT");
        contextField.setAccessible(true);
        contextField.set(null, applicationContext);


       when(applicationContext.getBean("smsSubscriptionService")).thenReturn(smsSubscriptionService);
       when(SmsServiceRegistry.getSmsSubscriptionService()).thenReturn(smsSubscriptionService);
    }

    private void setPrivateField(Object targetObject, String fieldName, Object value) throws Exception {
        Field field = targetObject.getClass().getDeclaredField(fieldName);
        field.setAccessible(true); // Allow access to private field
        field.set(targetObject, value);
    }

    @Test
    void testGetEnrollmentsForUser_MissingUserId() {
        Response response = entitlementResource.getEnrollmentsForUser("correlationId", "x-client", "token",
                "", "userIdentifierType", "organizationId", "productId", "productIdentifierType",
                "courseSectionId", "sectionIdentifierType", "contextName", 1, 1, "accessGrantType");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Required user id is missing.", ((EntitlementResponse) response.getEntity()).getStatusMessage());
    }

    @Test
    void testGetEnrollmentsForUser_MissingCorrelationId() {
        Response response = entitlementResource.getEnrollmentsForUser("", "x-client", "token",
                "userId", "userIdentifierType", "organizationId", "productId", "productIdentifierType",
                "courseSectionId", "sectionIdentifierType", "contextName", 1, 1, "accessGrantType");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Required field is missing. correlation-id is missing.", ((EntitlementResponse) response.getEntity()).getStatusMessage());
    }

    @Test
    void testGetEnrollmentsForUser_UnsupportedOrganizationId() {
        Response response = entitlementResource.getEnrollmentsForUser("correlationId", "x-client", "token",
                "userId", "userIdentifierType", "organizationId", "", "", "",
                "", "", 0, 0, "accessGrantType");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("We are not support for organizationId.", ((EntitlementResponse) response.getEntity()).getStatusMessage());
    }

    @Test
    void testGetEnrollmentsForUser_MissingFields() {
        Response response = entitlementResource.getEnrollmentsForUser("correlationId", "x-client", "token",
                "userId", "userIdentifierType", "", "", "", "",
                "", "", 0, 0, "accessGrantType");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Required field is missing. productId/courseSectionId/siteId/moduleId or organizationId is missing.", ((EntitlementResponse) response.getEntity()).getStatusMessage());
    }

    @Test
    void testUserIdentifierIsIES() {
        Response response = entitlementResource.getEnrollmentsForUser("correlationId", "x-client", "token",
                "userId", "IES", "organizationId", "productId", "productIdentifierType",
                "courseSectionId", "sectionIdentifierType", "contextName", 1, 1, "accessGrantType");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("User Identifier Type IES is not supported", ((EntitlementResponse) response.getEntity()).getStatusMessage());
    }

    @Test
    void testGetEnrollmentsForUser_NumberFormatException() {
        Response response = entitlementResource.getEnrollmentsForUser("correlationId", "x-client", "token",
                "invalidUserId", "SMS_INTERNAL", "organizationId", "productId",
                "productIdentifierType", "courseSectionId", "sectionIdentifierType",
                "contextName", 1, 1, "accessGrantType");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Not a valid user id", ((EntitlementResponse) response.getEntity()).getStatusMessage());
    }

    @Test
    void testGetEnrollmentsForUser_InternalServerError() throws Exception {
        when(smsSubscriptionService.getUserSubscriptionDetails(any(), anyString(), anyString()))
                .thenThrow(new DataRetrievalFailureException("Database error"));

        Response response = entitlementResource.getEnrollmentsForUser("correlationId", "x-client", "token",
                "123123", "SMS_INTERNAL", "organizationId", "productId",
                "productIdentifierType", "courseSectionId", "sectionIdentifierType",
                "contextName", 1, 1, "accessGrantType");

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals("INTERNAL_SERVER_ERROR", ((EntitlementResponse) response.getEntity()).getStatusMessage());
    }

    @Test
    void testGetEnrollmentsForUser_InternalServerError_BadRequest() throws Exception {
        when(smsSubscriptionService.getUserSubscriptionDetails(any(), anyString(), anyString()))
                .thenThrow(new com.pearson.ed.ltg.sms.service.exception.BadRequestException("BadRequestException error"));
        String token = fetchAuthToken();
        String expectedPiId = "testPiId";
        Properties properties = new Properties();
        properties.setProperty("com.pearson.ed.pi.auth.token.base.url", "https://int-piapi-internal.stg-openclass.com/tokens/");
        properties.setProperty("com.pearson.ed.pi.auth.token.connectTimeoutMillis", "10000");
        properties.setProperty("com.pearson.ed.pi.auth.token.readTimeoutMillis", "10000");

        setPrivateField(smsProperties, "_properties", properties);

        // Create a spy of the EntitlementResource
        EntitlementResource spyResource = spy(entitlementResource);

        // Mock SmsProperties
        when(SmsProperties.getInstance()).thenReturn(smsProperties);
        when(smsProperties.get_properties()).thenCallRealMethod();

        // Mock TokenAuthenticator behavior
        when(tokenAuthenticator.getPiId(any())).thenReturn(expectedPiId);

        Response response = entitlementResource.getEnrollmentsForUser("correlationId", "x-client", token,
                "123123", "SMS_INTERNAL", "organizationId", "productId",
                "productIdentifierType", "courseSectionId", "sectionIdentifierType",
                "contextName", 1, 1, "accessGrantType");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("userIdentifierType/productIdentifierType/sectionIdentifierType or organizationId not supported", ((EntitlementResponse) response.getEntity()).getStatusMessage());
    }

    @Test
    void testGetEnrollmentsForUser_InvalidSiteIdAndModuleId() {
        // Act
        Response response = entitlementResource.getEnrollmentsForUser(
                "correlationId", "x-client", "token", "userId", "userIdentifierType", "", "",
                "productIdentifierType", "", "sectionIdentifierType",
                "contextName", -1, -1, "");

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(),
                "Expected HTTP 400 Bad Request for invalid siteId and moduleId.");
        assertEquals("SiteId/moduleId not a valid values", ((EntitlementResponse) response.getEntity()).getStatusMessage(),
                "Expected response entity to indicate that siteId/moduleId are not valid.");
    }

    @Test
    void testGetEnrollmentsForUser_InvalidUserId_ReturnsBadRequest() throws Exception {

        Response response = entitlementResource.getEnrollmentsForUser("correlationId", "x-client", "validToken",
                "0", "SMS_INTERNAL", "organizationId", "productId",
                "PDZ", "courseSectionId", "sectionIdentifierType",
                "NONE", 1, 1, "accessGrantType");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Not a valid user id", ((EntitlementResponse) response.getEntity()).getStatusMessage());
    }


    /**
     * Fetches a fresh authentication token.
     * This method sends a request to the authentication API and retrieves a new token.
     */
    private String fetchAuthToken() throws Exception {
        String tokenUrl = "https://int-piapi-internal.stg-openclass.com/tokens";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(tokenUrl);
            httpPost.setHeader("Content-Type", "application/json");

            // Prepare JSON request body
            JSONObject json = new JSONObject();
            json.put("userName", "CSTool_stg");
            json.put("password", "Password2");

            // Attach JSON to the request
            StringEntity entity = new StringEntity(json.toString());
            httpPost.setEntity(entity);
            // Execute the request
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                // Allow both 200 (OK) and 201 (Created) responses
                if (statusCode != 200 && statusCode != 201) {
                    throw new RuntimeException("Failed to fetch token: " + response.getStatusLine());
                }
                // Parse response
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONObject responseJson = new JSONObject(responseBody);
                // Ensure response contains "data" which holds the token
                if (!responseJson.has("data")) {
                    throw new RuntimeException("Token not found in response: " + responseBody);
                }
                return responseJson.getString("data"); // Extract token from "data"
            }
        }
    }

    @Test
    public void testNotFoundException() throws Exception {
        String token = fetchAuthToken();
        // System.out.println("Generated Token: " + token);
        String expectedPiId = "testPiId";
        String userId = "232323";

        // Enum values for identifiers
        UserIdentifierType userIdentifierType = UserIdentifierType.SMS_INTERNAL;
        ProductIdentifierType productIdentifierType = ProductIdentifierType.SMS_MATERIAL_INTERNAL;
        SectionIdentifierType sectionIdentifierType = SectionIdentifierType.SMS_INTERNAL;

        Properties properties = new Properties();
        properties.setProperty("com.pearson.ed.pi.auth.token.base.url", "https://int-piapi-internal.stg-openclass.com/tokens/");
        properties.setProperty("com.pearson.ed.pi.auth.token.connectTimeoutMillis", "10000");
        properties.setProperty("com.pearson.ed.pi.auth.token.readTimeoutMillis", "10000");

        setPrivateField(smsProperties, "_properties", properties);

        // Create a spy of the EntitlementResource
        EntitlementResource spyResource = spy(entitlementResource);

        // Mock SmsProperties
        when(SmsProperties.getInstance()).thenReturn(smsProperties);
        when(smsProperties.get_properties()).thenCallRealMethod();

        // Mock TokenAuthenticator behavior
        when(tokenAuthenticator.getPiId(token)).thenReturn(expectedPiId);

        // Mock service to throw NotFoundException
        when(smsSubscriptionService.getUserSubscriptionDetails(any(), anyString(), anyString()))
                .thenThrow(new NotFoundException("No data found"));

        Response response = spyResource.getEnrollmentsForUser(
                "correlation-id", "x-client", token, userId,
                userIdentifierType.name(),  // Enum converted to String
                "organizationId", "productId",
                productIdentifierType.name(), // Enum converted to String
                "courseSectionId", sectionIdentifierType.name(), // Enum converted to String
                "contextName", 1, 1, "accessGrantType");

        // Ensure the correct error code
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(),
                "Expected 404, but got " + response.getStatus());

        // Ensure the correct error message is returned
        EntitlementResponse entitlementResponse = (EntitlementResponse) response.getEntity();
        assertEquals("No data found for given parameters", entitlementResponse.getStatusMessage(),
                "Expected 'No data found for given parameters' but got " + entitlementResponse.getStatusMessage());
    }

    @Test
    void whenBadRequestException_thenIllegalArgumentException() throws Exception {
        // Arrange
        when(SmsServiceRegistry.getSmsSubscriptionService()).thenReturn(smsSubscriptionService);
        when(smsSubscriptionService.getUserSubscriptionDetails(any(), any(), any()))
                .thenThrow(new BadRequestException("No input for subscription check"));

        // Act
        Response response = entitlementResource.getEnrollmentsForUser("correlation-id", "x-client", "token", "123",
                "USER", null, "productId", "productType",
                "courseId", "sectionType", "context", 1, 1, "accessType");

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        EntitlementResponse entitlementResponse = (EntitlementResponse) response.getEntity();
        assertEquals("IllegalArgumentException in userIdentifierType/productIdentifierType/sectionIdentifierType", entitlementResponse.getStatusMessage());
    }
    //-------------------------------------


}
