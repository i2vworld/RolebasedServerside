package com.world.i2v.i2vauthapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=ThisIsATestJwtSecretKeyForTestingPurposesOnly",
        "jwt.expiration=604800000",
        "app.jwt.refresh-expiration-ms=86400000",
        "app.base-url=http://localhost:3000",
        "app.mail.from=test@i2v.com",
        "spring.mail.host=localhost",
        "spring.mail.port=3025"
})
class I2vAuthApiApplicationTests {

    @Test
    void contextLoads() {
    }

}