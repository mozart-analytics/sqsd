/**
 * <h1>SQSD</h1>
 * sqsd : A simple alternative to the Amazon SQS Daemon "sqsd".
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

// TODO: Use Groovy's ConfigSlurper to enable configuration using properties file or env vars.
/**
 * AWS SQS  related configuration env-vars
 */
String AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID")
String AWS_SECRET_KEY = System.getenv("AWS_SECRET_KEY")
String REGION_NAME = System.getenv("AWS_REGION_NAME") ?: "us-east-1"
String SQS_QUEUE_NAME = System.getenv("SQS_QUEUE_NAME")
String SQS_QUEUE_URL = System.getenv("SQS_QUEUE_URL")
int MAX_NUMBER_OF_MESSAGES_PER_REQUEST = System.getenv("MAX_NUMBER_OF_MESSAGES") as Integer ?: 10

/**
 * HTTP service related configuration env-vars
 */
String HTTP_HOST = System.getenv("HTTP_HOST") ?: "http://127.0.0.1:10000"
String HTTP_PATH = System.getenv("HTTP_PATH") ?: "/comments-db-service/comments.json"
String HTTP_REQUEST_CONTENT_TYPE = System.getenv("HTTP_REQUEST_CONTENT_TYPE") ?: "application/json"

// TODO: Assertions of required variables without defaults.
// TODO: Print properties.

// Setup sqs client.
def awsCreds = new BasicAWSCredentials(AWS_ACCESS_KEY_ID as String, AWS_SECRET_KEY as String)
def sqs = new AmazonSQSClient(awsCreds)
def sqsRegion = Region.getRegion(Regions.fromName(REGION_NAME as String))
sqs.setRegion(sqsRegion)
String sqsQueueUrl = SQS_QUEUE_URL ?: sqs.getQueueUrl(SQS_QUEUE_NAME as String).getQueueUrl() // Use provided queue url or name (url has priority)

try {
    println("Receiving messages from " + sqsQueueUrl)
    println("Max Number of Messages : " + MAX_NUMBER_OF_MESSAGES_PER_REQUEST)

    // Configure sqs request
    def receiveMessageRequest = new ReceiveMessageRequest()
            .withQueueUrl(sqsQueueUrl)
            .withMaxNumberOfMessages(MAX_NUMBER_OF_MESSAGES_PER_REQUEST)
            .withWaitTimeSeconds(20) // TODO: Is this necessary?

    // Consume queue until empty
    while(true){
        def messages = sqs.receiveMessage(receiveMessageRequest).getMessages()
        println "Total Received Messages : " + messages.size()

        // Break when empty
        if(messages.size() <= 0) break

        for (Message message : messages) {
            if(handleMessage(HTTP_HOST, HTTP_PATH, HTTP_REQUEST_CONTENT_TYPE, message)) {
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
catch (AmazonServiceException ase) { ase.printStackTrace() } // TODO: Research how to add log4j or something similar.
catch (AmazonClientException ace) { ace.printStackTrace() }

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

    println "POST " + httpHost + httpPath + " :: " + status // TODO: Find out the correct way of displaying this message.

    status < 400
}
