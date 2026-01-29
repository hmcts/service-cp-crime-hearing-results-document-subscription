package uk.gov.hmcts.cp.subscription.integration.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;

import java.nio.file.Files;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableWireMock({@ConfigureWireMock(name = "material-client", baseUrlProperties = "material-client.url")})
class NotificationControllerIntegrationTest extends IntegrationTestBase {

    @Test
    void should_return_created_when_posting_valid_payload() throws Exception {
        ClassPathResource resource = new ClassPathResource("stubs/requests/pcr-request.json");
        String requestJson = Files.readString(resource.getFile().toPath());

        mockMvc.perform(post("/notifications/pcr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isAccepted());
    }
}
