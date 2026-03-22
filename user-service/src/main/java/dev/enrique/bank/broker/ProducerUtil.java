package dev.enrique.bank.broker;

import org.apache.kafka.clients.producer.ProducerRecord;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProducerUtil {
    public static <V> ProducerRecord<String, V> authHeaderWrapper(String topic, V event, String authUserId) {
        ProducerRecord<String, V> producerRecord = new ProducerRecord<>(topic, event);
        producerRecord.headers().add("auth-user-id", authUserId.getBytes());
        return producerRecord;
    }
}
