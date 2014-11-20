/**
 * <h1>SQSD</h1>
 * sqsd : A simple alternative to the Amazon SQS Daemon "sqsd".
 * <p>
 * Copyright (c) 2014 Mozart Analytics
 *
 * @author  <a href="mailto:ortiz.manuel@mozartanalytics.com">Manuel Ortiz</a>
 * @author  <a href="mailto:abdiel.aviles@mozartanalytics.com">Abdiel Aviles</a>
 */


import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient

@Grab(group='com.amazonaws', module='aws-java-sdk', version='1.9.6')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7')

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.DeleteMessageRequest
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest

/**
 * AWS SQS Configs
 */
def awsAccessKey = System.getenv("AWS_ACCESS_KEY_ID") ?: "yourKeyHere"
def awsSecretKey = System.getenv("AWS_SECRET_KEY") ?: "yourSecretHere"
def awsCreds = new BasicAWSCredentials(awsAccessKey as String, awsSecretKey as String)

def REGION_NAME = System.getenv("AWS_REGION_NAME") ?: "us-east-1"
def TARGET_QUEUE = System.getenv("SQS_TARGET_QUEUE") ?: "yourSqsQueueName"
def MAX_NUMBER_OF_MESSAGES_PER_REQUEST = System.getenv("MAX_NUMBER_OF_MESSAGES") as Integer ?: 10

def LOCAL_ENDPOINT = System.getenv("LOCAL_ENDPOINT") ?: "/"

def sqs = new AmazonSQSClient(awsCreds)
sqs.setRegion(Region.getRegion(Regions.fromName(REGION_NAME as String)))
def targetQueue = sqs.getQueueUrl(TARGET_QUEUE as String).getQueueUrl()

try {

    println("Receiving messages from " + targetQueue)
    println("Max Number of Messages : " + MAX_NUMBER_OF_MESSAGES_PER_REQUEST)

    // Configure request
    def receiveMessageRequest = new ReceiveMessageRequest(targetQueue)
    receiveMessageRequest.setMaxNumberOfMessages(MAX_NUMBER_OF_MESSAGES_PER_REQUEST)

    // Consume queue until empty
    while(true){
        def messages = sqs.receiveMessage(receiveMessageRequest).getMessages()
        println("Total Received Messages : " + messages.size())

        // Break when empty
        if(messages.size() <= 0) break

        for (Message message : messages) {
            if(handleMessage(LOCAL_ENDPOINT, message)) {
                // If successful, delete the message
                println("Deleting Message")
                String messageReceiptHandle = message.getReceiptHandle()
                sqs.deleteMessage(new DeleteMessageRequest(targetQueue, messageReceiptHandle))
            }
            else // TODO we can validate that the long polling timeout is considerably longer than the visibility timeout, if true then we can skip the exception
                throw new Exception("Local Service Failure") // We should stop consuming here
        }
    }
}
catch (AmazonServiceException ase) { ase.printStackTrace() }
catch (AmazonClientException ace) { ace.printStackTrace() }

def handleMessage(String endpoint, Message message){
    def localhost = System.getenv("LOCAL_HOST") ?: "http://127.0.0.1"
    def slurper = new JsonSlurper().setType(JsonParserType.LAX)
    def payload = slurper.parseText(message.getBody())

    int status
    try {
        def resp = new RESTClient(localhost).post(
                path : endpoint,
                body : payload,
                contentType : System.getenv("MIME_TYPE") ?: "application/json"
        )
        status = resp.status
    }
    catch(HttpResponseException ex ) { status = ex.response.status }

    println "POST " + localhost + endpoint + " :: " + status

    status < 400
}
