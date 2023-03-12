# Spring @Conditional 활용하기

Spring 설정중 특정 조건에 따라 적용 하고 싶을 때 활용할 수 있는 애노테이션이 있다.
`@Configuration` class, `@Bean` method 등에 활용할 조건에 따라 적용 할 수 있다.

## Condition class

`@Conditional` 애노테이션을 적용하기 위해서는 `Condition` 클래스를 포함해야 한다.
`Condition` 클래스는 애노테이션 선언시 적용 할 것인지 안 할 것인지 판단해 주는 클래스이다.

`matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean`
메서드를 오버라이드 하여 적용 여부를 판단해 반환해 주면 된다.

```kotlin
class MyCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata) : Boolean {
        return Boolean() // true or false
    }
}
```

## Conditional annotation

앞서 작성한 `Condition` 클래스를 등록해 조건형 `Bean` 혹은 조건형 `Configuration` 클래스를 만들 수 있다.

```kotlin
@Configuration
@Conditional(MyCondition::class)
class MyConfiguration
```

## example

`@Conditional` 적용이 잘 되는지 판단할 수 있는 간단한 예제를 만들어 보자.

### Configuration

우선 `MyBean` 클래스를 만들어 Bean 등록되는 두개의 설정 클래스를 생성한다.
하나는 Spring `ApplicationContext`에 등록되도록 하고 하나는 등록되지 않도록 만드는 것이 목표다.

아직 `Conditional` 생성을 하지 않았으니 우선 두개의 `Configuration`을 만들기만 하자.

```kotlin
class MyTest {
    class MyBean
    
    @Configuration
    class Config1 {
        @Bean
        fun myBean() = MyBean()
    }


    @Configuration
    class Config2 {
        @Bean
        fun myBean() = MyBean()
    }
}
```

### ApplicationContextRunner

테스트를 위한 `ApplicationContextRunner`를 생성해 빈이 포함되었는지 아닌지 확인하는 테스트 코드를 작성하자.

다음은 `Config1` 클래스를 `@Configuration` 등록하여 `Config1` 클래스와 `MyBean` 클래스가 등록되었는지 확인하는 코드다. 

```kotlin
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class MyTest {
    ...
    
    @Test
    fun conditionalTest() {
        ApplicationContextRunner().withUserConfiguration(Config1::class.java).run {
            assertThat(it)
                .hasSingleBean(MyBean::class.java)
                .hasSingleBean(Config1::class.java)
        }
    }
}
```

그리고 이 테스트를 실행해 정상 동작하는지 확인해 본다.
테스트 결과가 성공하면 이상없이 잘 등록되었다는 것이다.

### Condition

이제 두개의 설정이 각각 하나는 적용되고 하나는 적용되지 않도록 만들어 주어야 한다.

우선 가장 단순하게 `true`, `false`를 반환하는 두 개의 `Condition` 클래스를 만들어 보자.

```kotlin
class TrueCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata) = true
}

class FalseCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata) = false
}
```

### Conditional 애노테이션

그리고 이 클래스를 통해 `Confi1` 클래스에는 `TrueCondition` 등록한다.
`Config2` 클래스에는 `FalseCondition` 등록을 해 주자.

기존 만들었던 `Config` 클래스에 Conditional 애노테이션을 등록한다.

```kotlin
    @Configuration
    @Conditaional(TrueCondition::class)
    class Config1 {
        @Bean
        fun myBean() = MyBean()
    }


    @Configuration
    @Conditional(FalseCondition::class)
    class Config2 {
        @Bean
        fun myBean() = MyBean()
    }
```

### 등록되지 않은 클래스 테스트

앞서 만들어 두었던 테스트 코드는 등록된 클래스를 테스트 할 수 있는 코드였다.
`Config1` 클래스는 기존 그대로 등록되어 있어야 하고 `Config2` 클래스는 등록되지 않아야 한다.

이를 위해 `MyBean` 클래스를 `MyBean1`, `MyBean2` 두 개로 분리한다.
`Config1` 에는 `MyBean1` 클래스를 등록하고, `Config2` 에는 `MyBean2` 클래스를 등록한다.

```kotlin
//class MyBean
class MyBean1
class MyBean2

@Configuration
@Conditaional(TrueCondition::class)
class Config1 {
    @Bean
    fun myBean() = MyBean1()
}


@Configuration
@Conditional(FalseCondition::class)
class Config2 {
    @Bean
    fun myBean() = MyBean2()
}
```

다음으로 ApplicationContextRunner 에서 Config2 Configuration 클래스도 등록한다.
그리고 `assertThat({context}).doesNotHaveBean({class})` 테스트를 추가해 보자.

```kotlin
    @Test
    fun conditionalTest() {
        ApplicationContextRunner()
            .withUserConfiguration(
                Config1::class.java,
                Config2::class.java
            )
            .run {
                assertThat(it)
                    .hasSingleBean(MyBean1::class.java)
                    .hasSingleBean(Config1::class.java)
                    .doesNotHaveBean(MyBean2::class.java)
                    .doesNotHaveBean(Config2::class.java)
            }
    }
```

이와 같이 테스트를 실행해 보자.

테스트 결과가 정산이라면 `Condition` 클래스와 이를 통한 `@Conditional` 애노테이션을 잘 활용한 것이다.

## 샘플코드

위의 내용을 이해 했다면 아래와 같은 `MetaAnnotation` 을 사용해 `Condition`을 구성해 보는 것도 좋겠다. 

아래 샘플을 참조해 스스로 테스트 코드를 작성해 보는 것을 추천한다.
[GitHub 저장소](https://github.com/k1005/spring-conditional-sample) 에서
[src/test/kotlin/com/example/springconditionalsample](https://github.com/k1005/spring-conditional-sample/tree/master/src/test/kotlin/com/example/springconditionalsample) 디렉토리에 넣어 두었다.

```kotlin
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
```
