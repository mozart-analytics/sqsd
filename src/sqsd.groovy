/**
 * <h1>SQSD</h1>
 * sqsd : A simple alternative to the Amazon SQS Daemon ("sqsd") used on AWS Beanstalk worker tier instances.
 * <p>
 * Copyright (c) 2014 Mozart Analytics
 *
 * @author  <a href="mailto:abdiel.aviles@mozartanalytics.com">Abdiel Aviles</a>
 * @author  <a href="mailto:ortiz.manuel@mozartanalytics.com">Manuel Ortiz</a>
 */

@Grab(group='com.amazonaws', module='aws-java-sdk', version='1.9.6')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.2')

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.AmazonSQSClient
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
assert config.aws.access_key_id != null, "Required `aws.access_key_id` property not provided!!"
assert config.aws.secret_access_key != null, "Required `aws.secret_access_key` property not provided!!"
assert config.sqsd.queue.url != null || config.sqsd.queue.name != null, "Required `sqsd.queue.url` OR `sqsd.queue.name` property not provided!!"
assert config.sqsd.worker.http.host != null, "Required `sqsd.worker.http.host` property not provided!!"
assert config.sqsd.worker.http.path != null, "Required `sqsd.worker.http.path` property not provided!!"

// Setup sqs client.
def awsCreds = new BasicAWSCredentials(config.aws.access_key_id as String, config.aws.secret_access_key as String) // TODO: Determine if this is the correct approach when using Docker.
def sqs = new AmazonSQSClient(awsCreds)
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
            .withAttributeNames('All')

    // Consume queue until empty
    while(true){ // TODO: Limit the amount of messages to process using a property.
        println "Querying SQS for messages ..."
        def messages = sqs.receiveMessage(receiveMessageRequest).getMessages()
        println "Received Messages : " + messages.size()

        // Break when empty
        if(messages.size() <= 0) break

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
            else // TODO we might validate that the long polling timeout is considerably longer than the visibility timeout, if true then we could skip the exception. Still this is a dangerous alternative.
                throw new Exception("Local Service Failure. Message ID : " + message.getMessageId()) // We should stop consuming here
        }

        println "Done!!"
    }
}
catch (AmazonServiceException ase) { ase.printStackTrace() } // TODO: Add log4j or something similar.
catch (AmazonClientException ace) { ace.printStackTrace() }

println "SQSD finished successfully!"

def handleMessage(String httpHost, String httpPath, String contentType, Message message){
    def slurper = new JsonSlurper().setType(JsonParserType.LAX)
    def payload = slurper.parseText(message.getBody())

    int status
    try {
        def resp = new RESTClient(httpHost).post(
                path : httpPath,
                body : payload,
                contentType : contentType,
                headers: [
                    "User-Agent": "aws-sqsd/1.1",
                    "X-Aws-Sqsd-Msgid": message.getMessageId(),
                    "X-Aws-Sqsd-Queue": "",
                    "X-Aws-Sqsd-First-Received-At": message.attributes.ApproximateFirstReceiveTimestamp,
                    "X-Aws-Sqsd-Receive-Count": message.attributes.ApproximateReceiveCount,
                    "X-Aws-Sqsd-Sender-Id": message.attributes.SenderId
                ]
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
