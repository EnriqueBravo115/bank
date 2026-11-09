# User events contract

The Kafka events user-service publishes and account-service consumes, one file per topic:

- `topic`: the topic.
- `type`: the `__TypeId__` header, i.e. the event class name. Both services have a class with that
  name and package; `JsonDeserializer` picks the class from the header.
- `payload`: an example of the JSON value, with every field.

Every record also carries the `auth-user-id` header (the user's Keycloak id, also the record key)
and an `event-id` header.

Both services test against these files: user-service checks that what it publishes matches them
(`UserEventContractTest`) and account-service consumes them as they are (`UserConsumerTest`).
Changing an event means changing its file, and both tests tell which side still has to change.
