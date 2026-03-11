package uk.gov.hmcts.cp.servicebus.integration;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.cp.material.openapi.model.MaterialMetadata;
import uk.gov.hmcts.cp.openapi.model.EventPayload;
import uk.gov.hmcts.cp.subscription.integration.config.TestContainersInitialise;
import uk.gov.hmcts.cp.subscription.services.MaterialService;

import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.filters.TracingFilter.MDC_CORRELATION_ID;
import static uk.gov.hmcts.cp.openapi.model.EventType.PRISON_COURT_REGISTER_GENERATED;
import static uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService.PCR_INBOUND_TOPIC;

@Slf4j
@SpringBootTest
@ContextConfiguration(initializers = TestContainersInitialise.class)
@TestPropertySource(properties = {
        "material-client.retry.timeoutMilliSecs=500",
        "service-bus.max-tries=2",
        "service-bus.retry-msecs=0"
})
public class ServiceBusPcrInboundIntegrationTest extends ServiceBusIntegrationTestBase {

    @MockitoBean
    MaterialService materialService;

    UUID materialId = UUID.fromString("570e1a66-1341-474c-92b2-b7e89e0a524a");

    @BeforeEach
    void setUp() {
        await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .until(adminService::isServiceBusReady);
        processorService.stopMessageProcessor(PCR_INBOUND_TOPIC);
        testService.dropTopicIfExists(PCR_INBOUND_TOPIC);
        adminService.createTopicAndSubscription(PCR_INBOUND_TOPIC);
        processorService.startMessageProcessor(PCR_INBOUND_TOPIC);
    }

    @AfterEach
    void afterEach() {
        processorService.stopMessageProcessor(PCR_INBOUND_TOPIC);
    }

    @SneakyThrows
    @Test
    void inbound_notification_should_process_material_service() {
        MaterialMetadata materialMetadata = new MaterialMetadata();
        when(materialService.waitForMaterialMetadata(materialId)).thenReturn(materialMetadata);
        EventPayload eventPayload = EventPayload.builder().eventType(PRISON_COURT_REGISTER_GENERATED).materialId(materialId).build();
        MDC.put(MDC_CORRELATION_ID, UUID.randomUUID().toString());
        clientService.queueMessage(PCR_INBOUND_TOPIC, null, jsonMapper.toJson(eventPayload), 0);
        MDC.clear();

        Thread.sleep(5000);
        verify(materialService, times(2)).waitForMaterialMetadata(materialId);
    }

    @SneakyThrows
    @Test
    void process_message_should_retry_n_times_then_send_to_DLQ() {
        when(materialService.waitForMaterialMetadata(materialId)).thenReturn(null);
        EventPayload eventPayload = EventPayload.builder().eventType(PRISON_COURT_REGISTER_GENERATED).materialId(materialId).build();
        MDC.put(MDC_CORRELATION_ID, UUID.randomUUID().toString());
        clientService.queueMessage(PCR_INBOUND_TOPIC, null, jsonMapper.toJson(eventPayload), 0);
        MDC.clear();

        Thread.sleep(5000);
        verify(materialService, times(2)).waitForMaterialMetadata(materialId);
    }
}
