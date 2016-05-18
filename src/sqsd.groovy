/**
 * <h1>SQSD</h1>
 * sqsd : A simple alternative to the Amazon SQS Daemon ("sqsd") used on AWS Beanstalk worker tier instances.
 * <p>
 * Copyright (c) 2016 Mozart Analytics
 *
 * @author  <a href="mailto:abdiel.aviles@mozartanalytics.com">Abdiel Aviles</a>
 * @author  <a href="mailto:ortiz.manuel@mozartanalytics.com">Manuel Ortiz</a>
 */

@Grab(group='com.amazonaws', module='aws-java-sdk', version='1.11.1')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.2')

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest
import com.amazonaws.services.sqs.model.DeleteMessageRequest
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient

String DEFAULT_CONFIG_FILE_LOCATION = "config/sqsd-default-config.groovy"
String SQSD_VERSION = "1.0.0"

// Retrieve configuration values from environment -> default-config -> config.
def config = new ConfigSlurper().parse(new File(DEFAULT_CONFIG_FILE_LOCATION).toURI().toURL())
File customConfigFile = new File(config.sqsd.config_file as String)

// Merge default-config with provided config.
if (customConfigFile.exists()) {
    def customConfig = new ConfigSlurper().parse(customConfigFile.toURI().toURL())
    config = config.merge(customConfig)
}

// Assert that all required properties are provided.
assert config.sqsd.queue.url != null || config.sqsd.queue.name != null, "Required `sqsd.queue.url` OR `sqsd.queue.name` property not provided!!"
assert config.sqsd.worker.http.host != null, "Required `sqsd.worker.http.host` property not provided!!"
assert config.sqsd.worker.http.path != null, "Required `sqsd.worker.http.path` property not provided!!"

// Setup sqs client.
def sqs = new AmazonSQSClient()
def sqsRegion = Region.getRegion(Regions.fromName(config.sqsd.queue.region_name as String))
sqs.setRegion(sqsRegion)
String sqsQueueUrl = config.sqsd.queue.url ?: sqs.getQueueUrl(config.sqsd.queue.name as String).getQueueUrl() // Use provided queue url or name (url has priority)

try {
    println("Receiving messages from " + sqsQueueUrl)

    // Configure sqs request
    def receiveMessageRequest = new ReceiveMessageRequest()
            .withQueueUrl(sqsQueueUrl)
            .withMaxNumberOfMessages(config.sqsd.max_messages_per_request as Integer)
            .withWaitTimeSeconds(config.sqsd.wait_time_seconds as Integer) // Sets long-polling wait time seconds (long-polling has to be enabled on related SQS)

    // Consume queue until empty
    while(true){ // TODO: Limit the amount of messages to process using a property.
        def messages = sqs.receiveMessage(receiveMessageRequest).getMessages()
        println "Queried SQS, received " + messages.size() + " messages."

        // Break when empty if not running daemonized
        if(messages.size() <= 0) {
            if (config.sqsd.run_daemonized < 1) break
            else if(config.sqsd.sleep_seconds) sleep(config.sqsd.sleep_seconds * 1000) // don't want to hammer implementations that don't implement long-polling
        }

        for (Message message : messages) { // TODO: Make async.
            if(handleMessage(
                    config.sqsd.worker.http.host as String,
                    config.sqsd.worker.http.path as String,
                    config.sqsd.worker.http.request.content_type as String,
                    message)
            ) {
                // If successful, delete the message
                println("Deleting message...")
                String messageReceiptHandle = message.getReceiptHandle()
                sqs.deleteMessage(new DeleteMessageRequest(sqsQueueUrl, messageReceiptHandle))
            }
            else {
                // If unsuccessful, give the message back to the queue for a retry (or a move into a DLQ)
                println("Giving message back to queue...")
                String messageReceiptHandle = message.getReceiptHandle()
                sqs.changeMessageVisibility(new ChangeMessageVisibilityRequest(sqsQueueUrl, messageReceiptHandle, 0))
            }
        }
    }
}
catch (AmazonServiceException ase) { ase.printStackTrace() } // TODO: Add log4j or something similar.
catch (AmazonClientException ace) { ace.printStackTrace() }

println "SQSD exiting successfully!"

def handleMessage(String httpHost, String httpPath, String contentType, Message message){
    def slurper = new JsonSlurper().setType(JsonParserType.LAX)
    def payload = slurper.parseText(message.getBody())

    int status
    try {
        def resp = new RESTClient(httpHost).post(
                path : httpPath,
                body : payload,
                contentType : contentType
        )
        status = resp.status
    }
    catch(HttpResponseException ex ) {
        status = ex.response.status
        ex.printStackTrace()
    }
    catch(ConnectException ex) {
        status = 500
        ex.printStackTrace()
    }

    println "POST " + httpHost + httpPath + " :: " + status

    status == 200
}
