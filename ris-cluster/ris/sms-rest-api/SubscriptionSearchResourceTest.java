package com.pearson.ed.ltg.sms.restApiTest.deleverageapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.pearson.ed.ltg.sms.domain.rest.deleverageapi.subscription.search.SubscriptionConstants;
import com.pearson.ed.ltg.sms.domain.rest.deleverageapi.subscription.search.SubscriptionRequest;
import com.pearson.ed.ltg.sms.domain.rest.deleverageapi.subscription.search.SubscriptionResponse;
import com.pearson.ed.ltg.sms.rest.deleverageapi.SubscriptionSearchResource;
import com.pearson.ed.ltg.sms.service.SmsSubscriptionServiceIF;
import com.pearson.ed.ltg.sms.service.exception.NotFoundException;
import com.pearson.ed.ltg.sms.service.spring.SpringApplicationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.apache.log4j.Logger;
import javax.ws.rs.core.Response;
import java.util.Arrays;

public class SubscriptionSearchResourceTest {

    private static final Logger log = Logger.getLogger(SubscriptionSearchResourceTest.class);
    @InjectMocks
    private SubscriptionSearchResource subscriptionSearchResource;

    @Mock
    private SmsSubscriptionServiceIF smsSubscriptionService;

    @Mock
    private ApplicationContext applicationContext;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        SpringApplicationContext springApplicationContext = new SpringApplicationContext();
        springApplicationContext.setApplicationContext(applicationContext);
        when(applicationContext.getBean("smsSubscriptionService")).thenReturn(smsSubscriptionService);
    }

    @AfterEach
    public void tearDown() throws Exception {
        org.apache.log4j.LogManager.shutdown();
    }

    @Test
    public void testGetSubscriptionsByUserCourseIdsSuccess() throws Exception {
        SubscriptionRequest request = new SubscriptionRequest();
        request.setCourseIdType(SubscriptionConstants.SMS_INTERNAL);
        request.setTaking(Arrays.asList("course1", "course2"));
        request.setTeaching(Arrays.asList("course3", "course4"));

        SubscriptionResponse response = new SubscriptionResponse();

        when(smsSubscriptionService.getSubscriptionByUserCourseIds("123", request)).thenReturn(response);
        Response result = subscriptionSearchResource.getSubscriptionsByUserCourseIds("123", request);

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        SubscriptionResponse resultEntity = (SubscriptionResponse) result.getEntity();
        assertEquals(null, resultEntity.getErrorCode());
        assertEquals(null, resultEntity.getErrorMessage());
    }

    @Test
    public void testGetSubscriptionsByUserCourseIdsInvalidInput() throws Exception {
        SubscriptionRequest invalidRequest = new SubscriptionRequest();
        Response result = subscriptionSearchResource.getSubscriptionsByUserCourseIds("123", invalidRequest);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatus());
        SubscriptionResponse resultEntity = (SubscriptionResponse) result.getEntity();
        assertEquals("400", resultEntity.getErrorCode());
        assertEquals("Invalid courseIdType / Invalid courses", resultEntity.getErrorMessage());
    }

    @Test
    public void testGetSubscriptionsByUserCourseIdsNotFound() throws Exception {
        SubscriptionRequest request = new SubscriptionRequest();
        request.setCourseIdType(SubscriptionConstants.SMS_INTERNAL);
        request.setTaking(Arrays.asList("course1", "course2"));
        request.setTeaching(Arrays.asList("course3", "course4"));

        when(smsSubscriptionService.getSubscriptionByUserCourseIds("123", request))
                .thenThrow(new NotFoundException("The IesUserId is not found"));
        Response result = subscriptionSearchResource.getSubscriptionsByUserCourseIds("123", request);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), result.getStatus());
        SubscriptionResponse resultEntity = (SubscriptionResponse) result.getEntity();
        assertEquals("404", resultEntity.getErrorCode());
        assertEquals("The IesUserId is not found", resultEntity.getErrorMessage());
    }

    @Test
    public void testGetSubscriptionsByUserCourseIdsInternalServerError() throws Exception {
        SubscriptionRequest request = new SubscriptionRequest();
        request.setCourseIdType(SubscriptionConstants.SMS_INTERNAL);
        request.setTaking(Arrays.asList("course1", "course2"));
        request.setTeaching(Arrays.asList("course3", "course4"));

        when(smsSubscriptionService.getSubscriptionByUserCourseIds("123", request))
                .thenThrow(new RuntimeException("Internal Server Error"));
        Response result = subscriptionSearchResource.getSubscriptionsByUserCourseIds("123", request);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), result.getStatus());
        SubscriptionResponse resultEntity = (SubscriptionResponse) result.getEntity();
        assertEquals("500", resultEntity.getErrorCode());
        assertEquals("Internal Server Error", resultEntity.getErrorMessage());
    }

    @Test
    public void testGetSubscriptionsByUserCourseIdsEmptyResults() throws Exception {
        SubscriptionRequest request = new SubscriptionRequest();
        request.setCourseIdType(SubscriptionConstants.SMS_INTERNAL);
        request.setTaking(Arrays.asList("course1", "course2"));
        request.setTeaching(Arrays.asList("course3", "course4"));

        SubscriptionResponse response = new SubscriptionResponse();
        // Populate response object with empty results

        when(smsSubscriptionService.getSubscriptionByUserCourseIds("123", request)).thenReturn(response);

        Response result = subscriptionSearchResource.getSubscriptionsByUserCourseIds("123", request);

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        SubscriptionResponse resultEntity = (SubscriptionResponse) result.getEntity();
        // Assertions for empty results
    }
}
