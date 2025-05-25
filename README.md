
# Transactional Message-Driven Bean example project

This project shows an example of a message-driven bean:
* whose onMessage method is transactional, doing all its transactional work inside that single transaction
* whose onMessage method is even transactional with 2 participating transactional resources, including a database
* that runs in an Open Liberty application server

Other message-driven beans in the project are more simple than the one mentioned above.

## Creating this project

To bootstrap the project, the [Open Liberty JMS Guide](https://openliberty.io/guides/jms-intro.html) was used.

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

## Background on transactions

*Transactions* are a complex topic. To get some background on *ACID transactions*, it makes sense to
first consider *(local) database transactions*. An excellent explanation can be found in the article
[Spring Transaction Management](https://www.marcobehler.com/guides/spring-transaction-management-transactional-in-depth).
It may discuss local database transactions in a [Spring](https://spring.io) context, but in essence
this article is not about Spring. In a [Jakarta EE](https://jakarta.ee) context, the content of the
article would be quite similar.

Then it would make sense to consider *(local) JMS transactions*. See for example
[local JMS transactions](https://developer.ibm.com/articles/an-introduction-to-local-transactions-using-mq-and-jms/).
It is important to understand the *scope* of transactions in a JMS context, and how a *rollback* can
lead to *redelivery* (and even infinite redelivery depending on MQ configuration and/or message headers).

Next it is needed to get some familiarity with *distributed transactions*, spanning multiple transactional
resources, such as a messaging server and a database. Some background on that can be found when
studying *JTA* (see for example several Jakarta EE specifications, and/or study Spring's *PlatformTransactionManager*
API and its implementations).
