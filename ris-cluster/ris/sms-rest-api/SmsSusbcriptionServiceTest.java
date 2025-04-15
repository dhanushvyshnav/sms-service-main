package com.pearson.ed.ltg.sms.restApiTest.deleverageapi;
import cmg.sms.beans.lauchdarky.FeatureFlagClient;
import com.pearson.ed.ltg.sms.domain.rest.deleverageapi.EntitlementRequestData;
import com.pearson.ed.ltg.sms.domain.rest.deleverageapi.EntitlementResponse;
import com.pearson.ed.ltg.sms.service.SmsSubscriptionService;
import com.pearson.ed.ltg.sms.service.dao.SubscriptionDetailsDaoIF;
import com.pearson.ed.ltg.sms.service.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SmsSubscriptionServiceTest {

    @InjectMocks
    private SmsSubscriptionService smsSubscriptionService;

    @Mock
    private SubscriptionDetailsDaoIF subscriptionDetailsDao;

    @Mock
    private FeatureFlagClient featureFlagClient;

    private static final String SMS_FACADE_CONSUMER_PI_ID = "mockSmsFacadeConsumerPiId";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        Field smsFacadeField = SmsSubscriptionService.class.getDeclaredField("smsFacadeConsumerPiId");
        smsFacadeField.setAccessible(true);
        smsFacadeField.set(smsSubscriptionService, SMS_FACADE_CONSUMER_PI_ID);

        Field featureKeyField = SmsSubscriptionService.class.getDeclaredField("featureKey");
        featureKeyField.setAccessible(true);
        featureKeyField.set(smsSubscriptionService, "mockFeatureKey");

        Field defaultModeField = SmsSubscriptionService.class.getDeclaredField("defaultOperationMode");
        defaultModeField.setAccessible(true);
        defaultModeField.set(smsSubscriptionService, "DEFAULT_MODE");
    }

    @Test
    void testGetUserSubscriptionDetails_ValidAccessGrantType() throws Exception {
        // Arrange
        EntitlementRequestData requestData = new EntitlementRequestData();
        requestData.setUserId("user123");
        String apiConsumerIesUserId = SMS_FACADE_CONSUMER_PI_ID;
        String accessGrantType = "ENROLLMENT";
        String expectedOperationMode = "ENROLLMENT";

        EntitlementResponse mockResponse = new EntitlementResponse();
        when(subscriptionDetailsDao.getUserSubscriptionDetailsData(requestData, expectedOperationMode)).thenReturn(mockResponse);

        // Act
        EntitlementResponse response = smsSubscriptionService.getUserSubscriptionDetails(requestData, apiConsumerIesUserId, accessGrantType);

        // Assert
        assertNotNull(response);
    }

    @Test
    void testGetUserSubscriptionDetails_InvalidAccessGrantType() {
        // Arrange
        EntitlementRequestData requestData = new EntitlementRequestData();
        String apiConsumerIesUserId = SMS_FACADE_CONSUMER_PI_ID;
        String accessGrantType = null;

        // Act & Assert
        assertThrows(BadRequestException.class, () ->
                smsSubscriptionService.getUserSubscriptionDetails(requestData, apiConsumerIesUserId, accessGrantType));
    }

    @Test
    void testGetUserSubscriptionDetails_FeatureFlagFallback() throws Exception {
        // Arrange
        EntitlementRequestData requestData = new EntitlementRequestData();
        requestData.setUserId("user123");
        String apiConsumerIesUserId = "otherConsumerId";
        String accessGrantType = null;
        String expectedOperationMode = "FLAG_MODE";

        when(featureFlagClient.getFlagValue("mockFeatureKey", apiConsumerIesUserId, "DEFAULT_MODE")).thenReturn(expectedOperationMode);
        EntitlementResponse mockResponse = new EntitlementResponse();
        when(subscriptionDetailsDao.getUserSubscriptionDetailsData(requestData, expectedOperationMode)).thenReturn(mockResponse);

        // Act
        EntitlementResponse response = smsSubscriptionService.getUserSubscriptionDetails(requestData, apiConsumerIesUserId, accessGrantType);

        // Assert
        assertNotNull(response);
        verify(featureFlagClient).getFlagValue("mockFeatureKey", apiConsumerIesUserId, "DEFAULT_MODE");
        verify(subscriptionDetailsDao).getUserSubscriptionDetailsData(requestData, expectedOperationMode);
    }
}

