# sym-kafka-stream-app

The SymetryML Kafka Stream application automatically makes predictions 
from a `source` kafka topic and saves the prediction results into another `sink` kafka 
topic. Prediction can either be done using the SymetryML REST-API or an embedded
model inside the Kafka Stream application.


## Launching the sym-kafka-stream-app

Launching the SymetryML Kafka Stream application is easy. One
needs to setup a Java system property to specify the Kafka Stream
application configuration file:

`-Dsml.kstream.config.file`=/datasets/etc/kdpush.props 

Additionally, one can also provide a log4j properties file:

`-Dlog4j.configuration`=file:/datasets/etc/log4j-f.properties


## Embedding Model vs Using REST API

The model used to make predictions can be either embedded into the
Kafka Stream application or accessed via using the SymetryML REST-API.
Depending on which way is used, the following properties need to be 
configured.


### Embedded Models

To use an embedded model, the `sml.kstream.model.b64file` property must configured. Basically, the model must
first be exported from SymetryML and saved locally to a file. It
can then be used by the Kafka Stream application like in the following:

`sml.kstream.model.b64file=/datasets/rez/ml1-a8.model`


#### Example sym-kafka-stream-app Properties File for Embedded Model

```
# 
# SYMETRYML PROPS
#
# THIS IS THE SOURCE TOPIC TO USE
sml.kstream.source.topic=sml-ks-test
# THIS IS THE SINK TOPCI TO USE, IF IT DOES NOT EXISTS IT WILL BE CREATED
sml.kstream.sink.topic=sml-ks-pres-test

# THIS IS THE NAME OF THE APPLICATION; IT IS USED BY KAFKA INTERNALLY FOR THE PURPOSE
# OF OFFSET MANAGEMENT
sml.kstream.app.id=smlks-app-v1
# THIS IS THE NAME OF THE CLIENT
sml.kstream.client.id=smlks-client-v1

# THIS IS AN EXPORTED MODEL FILE, USE THIS WHEN EMBEDDING THE MODEL IN THE KAFKASTREAM APPLICATION
sml.kstream.model.b64file=/datasets/rez/ml1-a8.model

# THIS IS THE MAXIMUM NUMBER OF RECORDS TO PROCESS IN THE RECORD PROCESSING TASK
sml.kstream.record.poll.max=500
# THIS IS THE TIME BETWEEN EACH INVOKATION OF THE RECORD PROCESSING TASK
sml.kstream.schedule.ms=5000

# THE FOLLOWING ARE KAFKA PROPERTIES
bootstrap.servers=charm:9092
schema.registry.url=http://charm:8081
enable.auto.commit=false
#commit.interval.ms
# default.value.serde

```

### REST API Models

If using the SymetryML REST-API then the following properties need to be
correctly configured:

```
# SPECIFY WHETHER TO USE REST API OR NOT
sml.kstream.rest=true
# THIS IS THE ID/NAME OF THE SYMETRYML PROJECT TO USE WHEN USING THE REST API FOR PREDICTION
sml.kstream.project.id=projectName
# THIS IS THE ID/NAME OF THE SYMETRYML MODEL TO USE WHEN USING THE REST API FOR PREDICTION
#sml.kstream.model.id=modelName
# THIS IS THE SYMETRYML USER TO USE WHEN USING THE REST API
sml.kstream.rest.user=userId
# THIS IS THE SECRET KEY USED TO AUTHENTICATE EVERY REST API CALL
sml.kstream.rest.secret=XXXXX_YOUR_SECRET_KEY_XXXXX
# THIS IS THE URL WHERE THE REST SERVER IS TO BE REACHED AT
sml.kstream.rest.host=http://YOURSERVER:8080
```

#### Example sym-kafka-stream-app Properties File for REST API

```
# 
# SYMETRYML PROPS
#
# THIS IS THE SOURCE TOPIC TO USE
sml.kstream.source.topic=sml-ks-test
# THIS IS THE SINK TOPCI TO USE, IF IT DOES NOT EXISTS IT WILL BE CREATED
sml.kstream.sink.topic=sml-ks-pres-test

# THIS IS THE NAME OF THE APPLICATION; IT IS USED BY KAFKA INTERNALLY FOR THE PURPOSE
# OF OFFSET MANAGEMENT
sml.kstream.app.id=smlks-app-v1
# THIS IS THE NAME OF THE CLIENT
sml.kstream.client.id=smlks-client-v1

# SPECIFY WHETHER TO USE REST API OR NOT
sml.kstream.rest=true
# THIS IS THE ID/NAME OF THE SYMETRYML PROJECT TO USE WHEN USING THE REST API FOR PREDICTION
sml.kstream.project.id=p1
# THIS IS THE ID/NAME OF THE SYMETRYML MODEL TO USE WHEN USING THE REST API FOR PREDICTION
#sml.kstream.model.id=m1
# THIS IS THE SYMETRYML USER TO USE WHEN USING THE REST API
sml.kstream.rest.user=user1
# THIS IS THE SECRET KEY USED TO AUTHENTICATE EVERY REST API CALL
sml.kstream.rest.secret=XXXXX_YOUR_SECRET_KEY_XXXXX
# THIS IS THE URL WHERE THE REST SERVER IS TO BE REACHED AT
sml.kstream.rest.host=http://server:8080

# THIS IS THE MAXIMUM NUMBER OF RECORDS TO PROCESS IN THE RECORD PROCESSING TASK
sml.kstream.record.poll.max=500
# THIS IS THE TIME BETWEEN EACH INVOKATION OF THE RECORD PROCESSING TASK
sml.kstream.schedule.ms=5000

# THE FOLLOWING ARE KAFKA PROPERTIES
bootstrap.servers=charm:9092
schema.registry.url=http://charm:8081
enable.auto.commit=false
#commit.interval.ms
# default.value.serde

```


## About Record Processing

Records coming from the SOURCE topic are processed by a task that runs periodically.
How often this task runs and how many records it can process at a time can
both be controlled with the following properties:`sml.kstream.record.poll.max` and 
`sml.kstream.schedule.ms`. `sml.kstream.record.poll.max` controls the maximum number of
records that will be processed and `sml.kstream.schedule.ms` controls how often
the task is run. For an application that needs high throughput, set 
`sml.kstream.schedule.ms` to a low number. But finding the optimal values for
these 2 properties depends upon many external factors like the hosting computer
number of cores, total memory, network bandwidth, over CPU utilization,
etc...


# Example log4j.properties File

```
# Root logger option
log4j.rootCategory=ERROR, file, stdout

log4j.category.com.sml.kafka.streams=DEBUG

# Direct log messages to stdout
log4j.appender.file=org.apache.log4j.DailyRollingFileAppender
log4j.appender.file.File=/opt/symetry/sml-ks-out.txt
log4j.appender.file.layout=org.apache.log4j.PatternLayout
log4j.appender.file.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n

log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.Target=System.out
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n
```


# Example Start Script on MacOS X

```
#!/bin/sh
#
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.0.10.jdk/Contents/Home/

# SPECIFY THE CONFIGURATION FILE HERE
JAVA_OPTIONS="-Dsml.kstream.config.file=/opt/symetry/sym-kafkastream-app/etc/simple.props"

# LOG4J CONFIG FILE
JAVA_OPTIONS="$JAVA_OPTIONS -Dlog4j.configuration=file:/opt/symetry/sym-kafkastream-app/etc/log4j-f.properties"

$JAVA_HOME/bin/java $JAVA_OPTIONS -cp "lib/*:/opt/symetry/libExt/*" com.sml.kafka.streams.app.SMLKafkaStreamAppV1
```

