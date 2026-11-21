package com.arshath.docuchat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-key",
        "docuchat.storage-dir=${java.io.tmpdir}/docuchat-context-test"
})
class DocuChatApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring AI auto-configuration, vector store and web layer wire together
    }
}
