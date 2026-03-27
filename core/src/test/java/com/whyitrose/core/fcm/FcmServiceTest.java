package com.whyitrose.core.fcm;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @Mock
    FirebaseMessaging firebaseMessaging;

    @InjectMocks
    FcmService fcmService;

    @Test
    void 멀티캐스트_발송_성공() throws FirebaseMessagingException {
        BatchResponse mockResponse = mock(BatchResponse.class);
        given(mockResponse.getSuccessCount()).willReturn(2);
        given(mockResponse.getFailureCount()).willReturn(0);
        given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(mockResponse);

        BatchResponse result = fcmService.sendMulticast(List.of("token1", "token2"), "제목", "내용");

        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(0);
    }

    @Test
    void 멀티캐스트_발송_실패시_RuntimeException_래핑() throws FirebaseMessagingException {
        given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                .willThrow(mock(FirebaseMessagingException.class));

        assertThatThrownBy(() -> fcmService.sendMulticast(List.of("token1"), "제목", "내용"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("FCM multicast 발송 실패");
    }
}