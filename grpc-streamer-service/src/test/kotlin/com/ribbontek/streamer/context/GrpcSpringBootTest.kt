package com.ribbontek.streamer.context

import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@ActiveProfiles("integration")
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
annotation class GrpcSpringBootTest
