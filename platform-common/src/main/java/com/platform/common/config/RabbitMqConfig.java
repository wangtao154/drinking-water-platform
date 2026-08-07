package com.platform.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 通用配置
 * 定义各业务 Exchange 和 Queue（对应 docker/rabbitmq/definitions.json）
 * 子模块按需声明自己的消费者
 */
@Configuration
public class RabbitMqConfig {

    // ==================== Exchange ====================

    public static final String DEVICE_EXCHANGE = "device.exchange";
    public static final String ALERT_EXCHANGE = "alert.exchange";
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String WORKORDER_EXCHANGE = "workorder.exchange";
    public static final String FILTER_EXCHANGE = "filter.exchange";
    public static final String NOTIFY_EXCHANGE = "notify.exchange";

    // ==================== Routing Key ====================

    public static final String RK_DEVICE_ACTIVATED = "device.activated";
    public static final String RK_DEVICE_UNBOUND = "device.unbound";
    public static final String RK_ALERT_CHECK = "alert.check";
    public static final String RK_ALERT_NOTIFY = "alert.notify";
    public static final String RK_FILTER_LIFE_CHECK = "filter.life.check";
    public static final String RK_ORDER_PAID = "order.paid";
    public static final String RK_WORK_ORDER_NEW = "work_order.new";
    public static final String RK_WORK_ORDER_ASSIGNED = "work_order.assigned";
    public static final String RK_WORK_ORDER_COMPLETED = "work_order.completed";
    public static final String RK_SETTLEMENT_DONE = "settlement.done";
    public static final String RK_DEVICE_STATUS_CHANGE = "device.status.change";

    // ==================== Exchange Beans ====================

    @Bean
    public TopicExchange deviceExchange() {
        return ExchangeBuilder.topicExchange(DEVICE_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange alertExchange() {
        return ExchangeBuilder.topicExchange(ALERT_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(ORDER_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange workOrderExchange() {
        return ExchangeBuilder.topicExchange(WORKORDER_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange filterExchange() {
        return ExchangeBuilder.topicExchange(FILTER_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange notifyExchange() {
        return ExchangeBuilder.topicExchange(NOTIFY_EXCHANGE).durable(true).build();
    }

    // ==================== Queue Beans ====================

    @Bean
    public Queue deviceActivatedQueue() {
        return QueueBuilder.durable("device.activated.queue").build();
    }

    @Bean
    public Queue alertCheckQueue() {
        return QueueBuilder.durable("alert.check.queue").build();
    }

    @Bean
    public Queue alertNotifyQueue() {
        return QueueBuilder.durable("alert.notify.queue").build();
    }

    @Bean
    public Queue filterLifeCheckQueue() {
        return QueueBuilder.durable("filter.life.check.queue").build();
    }

    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable("order.paid.queue").build();
    }

    @Bean
    public Queue workOrderNewQueue() {
        return QueueBuilder.durable("work_order.new.queue").build();
    }

    @Bean
    public Queue workOrderAssignedQueue() {
        return QueueBuilder.durable("work_order.assigned.queue").build();
    }

    @Bean
    public Queue workOrderCompletedQueue() {
        return QueueBuilder.durable("work_order.completed.queue").build();
    }

    @Bean
    public Queue deviceStatusChangeQueue() {
        return QueueBuilder.durable("device.status.change.queue").build();
    }

    @Bean
    public Queue settlementDoneQueue() {
        return QueueBuilder.durable("settlement.done.queue").build();
    }

    // ==================== Binding ====================

    @Bean
    public Binding deviceActivatedBinding() {
        return BindingBuilder.bind(deviceActivatedQueue()).to(deviceExchange()).with(RK_DEVICE_ACTIVATED);
    }

    @Bean
    public Binding alertCheckBinding() {
        return BindingBuilder.bind(alertCheckQueue()).to(alertExchange()).with(RK_ALERT_CHECK);
    }

    @Bean
    public Binding alertNotifyBinding() {
        return BindingBuilder.bind(alertNotifyQueue()).to(alertExchange()).with(RK_ALERT_NOTIFY);
    }

    @Bean
    public Binding filterLifeCheckBinding() {
        return BindingBuilder.bind(filterLifeCheckQueue()).to(filterExchange()).with(RK_FILTER_LIFE_CHECK);
    }

    @Bean
    public Binding orderPaidBinding() {
        return BindingBuilder.bind(orderPaidQueue()).to(orderExchange()).with(RK_ORDER_PAID);
    }

    @Bean
    public Binding workOrderNewBinding() {
        return BindingBuilder.bind(workOrderNewQueue()).to(workOrderExchange()).with(RK_WORK_ORDER_NEW);
    }

    @Bean
    public Binding workOrderAssignedBinding() {
        return BindingBuilder.bind(workOrderAssignedQueue()).to(workOrderExchange()).with(RK_WORK_ORDER_ASSIGNED);
    }

    @Bean
    public Binding workOrderCompletedBinding() {
        return BindingBuilder.bind(workOrderCompletedQueue()).to(workOrderExchange()).with(RK_WORK_ORDER_COMPLETED);
    }

    @Bean
    public Binding deviceStatusChangeBinding() {
        return BindingBuilder.bind(deviceStatusChangeQueue()).to(deviceExchange()).with(RK_DEVICE_STATUS_CHANGE);
    }

    @Bean
    public Binding settlementDoneBinding() {
        return BindingBuilder.bind(settlementDoneQueue()).to(notifyExchange()).with(RK_SETTLEMENT_DONE);
    }

    // ==================== Message Converter ====================

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
