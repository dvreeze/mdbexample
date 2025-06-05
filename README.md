
# Transactional Message-Driven Bean example project

This project shows an example of a message-driven bean:
* whose onMessage method is transactional, doing all its transactional work inside that single transaction
* whose onMessage method is even transactional with 2 participating transactional resources, including a database
* that runs in an Open Liberty application server

Other message-driven beans in the project are more simple than the one mentioned above.

The use of JMS is restricted to point-to-point messaging, rather than pub-sub.

The idea is to get a good understanding of transactions in message-driven beans, by diving into both *theory*
and *practice*. To a large extent, the theory is found in *Jakarta EE specifications*. The practice part
is this repository, where the code runs against Docker containers for Open Liberty and MQ, to make the
example more realistic and the conclusions more reliable.

## Creating this project

To bootstrap the project, the [Open Liberty JMS Guide](https://openliberty.io/guides/jms-intro.html) was used. Also, the
[Open Liberty JPA Guide](https://openliberty.io/guides/jpa-intro.html) was used.

## Running this project

The "messaging web application" in this project needs MQ as its JMS implementation. So "install" MQ first:

```shell
docker pull icr.io/ibm-messaging/mq:latest

docker volume create qm1data

docker run \
  --env LICENSE=accept \
  --env MQ_QMGR_NAME=QM1 \
  --volume qm1data:/mnt/mqm \
  --publish 1414:1414 \
  --publish 9443:9443 \
  --detach \
  --env MQ_APP_USER=app \
  --env MQ_APP_PASSWORD=passw0rd \
  --env MQ_ADMIN_USER=admin \
  --env MQ_ADMIN_PASSWORD=passw0rd \
  --name QM1 \
  icr.io/ibm-messaging/mq:latest
```

Also see the [IBM MQ Developer Essentials](https://developer.ibm.com/learningpaths/ibm-mq-badge/),
for example for more information on the [MQ Console](https://developer.ibm.com/learningpaths/ibm-mq-badge/setup-use-ibm-mq-console/)
which is also needed to interact with the message-driven beans in the web application.

Using the MQ Console, create extra queues DEV.QUEUE.DUMMY.1 and DEV.QUEUE.DUMMY.2.

We also need a PostgreSQL database:

```shell
docker pull postgres

mkdir ~/postgresdata

# Note we do not create any volume, in order to avoid "role does not exist" issues.

docker run -d \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=postgres \
  -p 5432:5432 \
  -v ~/postgresdata:/var/lib/postgresql/data \
  --name postgresql \
  postgres

docker exec -it postgresql psql -U postgres

# We are now inside the running postgresql container, inside psql

CREATE DATABASE messagedb;
# Check database "messagedb" exists
\list

\c messagedb;

# It seems auto-creation of tables does not happen. Let's do so manually.

CREATE SEQUENCE message_id_seq INCREMENT BY 1 NO MAXVALUE NO MINVALUE CACHE 1;
ALTER TABLE public.message_id_seq OWNER TO postgres;

CREATE TABLE message (
    id integer DEFAULT nextval('message_id_seq'::regclass) NOT NULL,
    creation_time timestamp without time zone DEFAULT now() NOT NULL,
    message_text text NOT NULL
);
ALTER TABLE public.message OWNER TO postgres;

# Constraint that is violated for blank messages (i.e. messages containing only whitespace as payload)
ALTER TABLE public.message ADD CONSTRAINT non_blank_message
    CHECK (nullif(trim(message_text), '') IS NOT NULL);

# Leaving psql and the container
exit
```

## Background on transactions

*Transactions* are a complex topic. To get some background on *ACID transactions*, it makes sense to
first consider *(local) database transactions*. An excellent explanation can be found in the article
[Spring Transaction Management](https://www.marcobehler.com/guides/spring-transaction-management-transactional-in-depth).
It may discuss local database transactions in a [Spring](https://spring.io) context, but in essence
this article is not about Spring. The gist of the article applies to resource-local database transactions
in any Java program (using annotation-based transaction management).

Then it would make sense to consider *(local) JMS transactions*. See for example
[local JMS transactions](https://developer.ibm.com/articles/an-introduction-to-local-transactions-using-mq-and-jms/).
It is important to understand the *scope* of transactions in a JMS context, and how a *rollback* of
a "message handler transaction" can lead to *redelivery* (and even infinite redelivery depending on MQ configuration
and/or message headers).

In an EJB context (see below), for *message-driven beans*, only for *container-managed transactions* message receipt is part of the
transaction. For *bean-managed transactions* this is not the case! See
[MDB transaction context](https://jakarta.ee/specifications/enterprise-beans/4.0/jakarta-enterprise-beans-spec-core-4.0#transaction-context-of-message-driven-bean-methods).

Next it is needed to get some familiarity with *distributed transactions*, spanning multiple transactional
resources, such as a messaging server and a database. Some background on that can be found when
studying *JTA* (see for example several Jakarta EE specifications, and/or study Spring's *PlatformTransactionManager*
API and its implementations). In a Jakarta EE context, it is very important to be aware of which APIs can
and cannot be used for:
* resource-local transactions
* container-managed JTA transactions (distributed if multiple transactional resources take part in the transaction)
* bean-managed JTA transactions (distributed if multiple transactional resources take part in the transaction)

The term "JTA transaction" is used rather loosely above. It does not necessarily mean that JTA as Jakarta EE API is
used ("jakarta.ejb" annotations qualify as well), but the term is used for container-managed and bean-managed
transactions in an EJB context. In other words, the term is used for transactions that are not resource-local
transactions whose boundaries can be set by using APIs such as JDBC, JPA and JMS. Recall that only "JTA transactions"
can be distributed.

We normally expect transactions to be atomic (the "A" in ACID), but this gets more complicated if non-transactional
resources (such as a remote file share) are part of the atomic "transaction". See
[Binding non-transactional resources into JTA transactions](https://www.maxant.ch/2015/08/11/1439322480000/)
to get an idea of complexities involved. Still, this is also something to think about when creating
(transactional) message-driven beans.

Note how the JMS specification and in particular the EJB specification are quite prescriptive about how to
use and not to use the JMS API w.r.t. transaction management. JTA transactions can be *container-managed* or *bean-managed*,
and we should be familiar with both styles of coding (non-local) transaction management in a JMS context.

Also note that *exception handling* for message-driven beans is closely related to transaction management.
In general, MDB message handling code should not throw any (unchecked) exceptions other than so-called
*application exceptions* (specifying whether the exception should lead to a transaction rollback),
unless the message-driven bean instance should be discarded.

A validation error on an incoming message should typically be an *application exception*. Typically, there is
little point in retrying receiving the same message if the message itself is to blame, so a rollback
(triggering retries) might be less useful than simply letting the message handling method be a no-op other
than the input validation in that code path (logging but not rethrowing the validation exception).

A failing database connection should typically be a *system exception*. Throwing a system exception from the
MDB leads to the MDB instance being discarded. This may be less desirable if it does make sense to retry
receiving the message. In that case we could treat the (briefly) failing database connection like
an application exception that should cause a rollback, but without discarding the MDB instance.

For more information on EJB exception handling, see for example:
* [EJB exception handling](https://jakarta.ee/specifications/enterprise-beans/4.0/jakarta-enterprise-beans-spec-core-4.0#a2940)
* in particular, [MDB exception handling](https://jakarta.ee/specifications/enterprise-beans/4.0/jakarta-enterprise-beans-spec-core-4.0#exceptions-from-message-driven-bean-message-listener-methods)

Important specifications concerning JMS and message-driven beans are:
* [JMS](https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1)
* [EJB](https://jakarta.ee/specifications/enterprise-beans/4.0/jakarta-enterprise-beans-spec-core-4.0)

As a foundational specification, the [CDI](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1)
specification is quite important as well.

Some specific interesting parts of the JMS specification are:
* [JMS transactions](https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1#transactions)
* [JMS distributed transactions](https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1#distributed-transactions)
* [JMS in Jakarta EE](https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1#use-of-jakarta-messaging-api-in-jakarta-ee-applications)
* [JMS examples](https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1#examples-of-the-simplified-api)

Some specific interesting parts of the EJB specification (for MDBs) are:
* [MDB](https://jakarta.ee/specifications/enterprise-beans/4.0/jakarta-enterprise-beans-spec-core-4.0#a1702)
* in particular, [MDB transaction context](https://jakarta.ee/specifications/enterprise-beans/4.0/jakarta-enterprise-beans-spec-core-4.0#transaction-context-of-message-driven-bean-methods)
* [EJB transactions](https://jakarta.ee/specifications/enterprise-beans/4.0/jakarta-enterprise-beans-spec-core-4.0#a2172)
* in particular, [sample transaction scenarios](https://jakarta.ee/specifications/enterprise-beans/4.0/jakarta-enterprise-beans-spec-core-4.0#sample-scenarios)

Two things that make transactions in Jakarta EE message-driven beans (and in JMS in general) more complex are:
* To reason about *program state*, we need to include the implicit (JTA or resource-local) transactional state, if any
  * This implicit program state is quite different for resource-local versus JTA transactions
  * There are ample opportunities to get things wrong when using EJBs and JMS
  * Exception handling is closely related to transactions, and should be programmed with care
* The Jakarta EE specs do not support *local reasoning* about code (given the restrictions in API use in different scenarios)
  * There are many restrictions on the use of the JMS and JTA APIs depending on the kind of transaction management
  * Also, annotations and deployment descriptors can both be used for the same "configuration"
  * In general, reasoning about *annotations* can be quite hard when there is a lot of machinery processing those annotations, or when combining them is easy to get wrong
  * As far as I am concerned, the Jakarta EE annotations can easily exhaust the *complexity budget* (that's why I'd rather avoid additional Lombok annotations)

Also, in practice, details about transaction management vary greatly among different "runtime environments".
For example, [transactions in Quarkus](https://quarkus.io/guides/transaction) are not distributed. So even
when using the JTA Transactional annotation, we need to let go of the assumption that the transaction can be
distributed over 2 or more transactional resources.
