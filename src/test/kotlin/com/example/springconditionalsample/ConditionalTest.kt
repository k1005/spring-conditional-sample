package com.example.springconditionalsample

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.*
import org.springframework.core.type.AnnotatedTypeMetadata

class ConditionalTest {

    @Test
    fun conditionalTest() {
        // true
        ApplicationContextRunner().withUserConfiguration(Config1::class.java, Config2::class.java).run {
            assertThat(it)
                .hasSingleBean(MyBean1::class.java)
                .hasSingleBean(Config1::class.java)
                .doesNotHaveBean(MyBean2::class.java)
                .doesNotHaveBean(Config2::class.java)
        }
    }

    @Configuration
    @TrueConditional
    class Config1 {
        @Bean
        fun myBean() = MyBean1()
    }

    @Configuration
    @BooleanConditional(false)
    class Config2 {
        @Bean
        fun myBean() = MyBean2()
    }

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
    @BooleanConditional(true)
    annotation class TrueConditional

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
    @Conditional(BooleanCondition::class)
    annotation class BooleanConditional(
        val value: Boolean
    )

    class MyBean1
    class MyBean2

    class BooleanCondition : Condition {
        override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata) : Boolean {
            val condition = metadata.annotations[BooleanConditional::class.java]
            return condition.getBoolean("value")
        }
    }
}
