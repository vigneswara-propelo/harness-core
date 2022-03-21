package io.harness.debezium;

// Consumer type governs what all consumer types you can have
// Right now we only have EVENTS_FRAMEWORK but can be diff like HTTP which will
// just post the event s to an http endpoint
public enum ConsumerType { EVENTS_FRAMEWORK }
