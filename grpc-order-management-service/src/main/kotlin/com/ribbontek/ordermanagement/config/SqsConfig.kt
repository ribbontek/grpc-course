package com.ribbontek.ordermanagement.config

import com.amazonaws.services.sqs.AmazonSQSAsync
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.aws.core.env.ResourceIdResolver
import org.springframework.cloud.aws.messaging.config.annotation.EnableSqs
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.converter.StringMessageConverter
import javax.annotation.PostConstruct

@Configuration
@EnableSqs
class SqsConfig(
    @Value("\${sqs.queue.processor.async.uri}") private val processorAsyncQueueEndpoint: String,
    @Value("\${sqs.queue.audit.fifo.uri}") private val auditFifoQueueEndpoint: String
) {
    @Autowired
    private lateinit var simpleMessageListenerContainer: SimpleMessageListenerContainer

    @PostConstruct
    fun postConstruct() {
        simpleMessageListenerContainer.queueStopTimeout = 20000 // removes timeout exceptions shutting down queues
    }

    @Bean
    fun asyncQueueMessagingTemplate(amazonSQSAsync: AmazonSQSAsync): QueueMessagingTemplate {
        return QueueMessagingTemplate(
            amazonSQSAsync,
            null as ResourceIdResolver?,
            StringMessageConverter()
        ).apply {
            setDefaultDestinationName(processorAsyncQueueEndpoint)
        }
    }

    @Bean
    fun fifoQueueMessagingTemplate(amazonSQSAsync: AmazonSQSAsync): QueueMessagingTemplate {
        return QueueMessagingTemplate(
            amazonSQSAsync,
            null as ResourceIdResolver?
        ).apply {
            setDefaultDestinationName(auditFifoQueueEndpoint)
        }
    }
}
