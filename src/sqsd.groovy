/**
 * <h1>SQSD</h1>
 * sqsd : A simple alternative to the Amazon SQS Daemon "sqsd".
 * <p>
 * Copyright (c) 2014 Mozart Analytics
 *
 * @author  <a href="mailto:ortiz.manuel@mozartanalytics.com">Manuel Ortiz</a>
 * @author  <a href="mailto:abdiel.aviles@mozartanalytics.com">Abdiel Aviles</a>
 */

@Grab(group='com.amazonaws', module='aws-java-sdk', version='1.9.6')

import java.util.Map.Entry

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
 * AWS Configs
 */
def awsAccessKey = "yourKeyHere"
def awsSecretKey = "yourSecretHere"

/**
 * AWS Stuff
 */
def awsCreds = new BasicAWSCredentials(awsAccessKey, awsSecretKey)

/**
 * SQS Configs
 */
def TARGET_QUEUE = "sqs-daemon-test"
def MAX_NUMBER_OF_MESSAGES_PER_REQUEST = 10
def MAX_WAIT_TIME_SECONDS = 10
def sqs = new AmazonSQSClient(awsCreds)
sqs.setRegion(Region.getRegion(Regions.US_EAST_1))
def targetQueue = sqs.getQueueUrl(TARGET_QUEUE).getQueueUrl()

try {

    println("Receiving messages from " + targetQueue)
    println("Max Number of Messages : " + MAX_NUMBER_OF_MESSAGES_PER_REQUEST)

    // Configure request
    def receiveMessageRequest = new ReceiveMessageRequest(targetQueue)
    receiveMessageRequest.setMaxNumberOfMessages(MAX_NUMBER_OF_MESSAGES_PER_REQUEST)
    if(MAX_WAIT_TIME_SECONDS)
        receiveMessageRequest.setWaitTimeSeconds(MAX_WAIT_TIME_SECONDS)

    // Consume queue until empty
    while(true){
        def messages = sqs.receiveMessage(receiveMessageRequest).getMessages()
        println("Total Received Messages : " + messages.size())

        // Break when empty
        if(messages.size() <= 0) break

        for (Message message : messages) {
            if(_handleMessage(message)) {
                // If successful, delete the message
                println("Deleting Message")
                String messageReceiptHandle = message.getReceiptHandle()
                sqs.deleteMessage(new DeleteMessageRequest(targetQueue, messageReceiptHandle))
            }
            // Else - just forget about it. The queue will handle it.
        }
    }

}
catch (AmazonServiceException ase) { ase.printStackTrace() }
catch (AmazonClientException ace) { ace.printStackTrace() }

def _handleMessage(Message message){
    // TODO implement local POST
    println("  Message")
    println("    MessageId:     " + message.getMessageId())
    println("    ReceiptHandle: " + message.getReceiptHandle())
    println("    MD5OfBody:     " + message.getMD5OfBody())
    println("    Body:          " + message.getBody())
    for (Entry<String, String> entry : message.getAttributes().entrySet()) {
        println("  Attribute")
        println("    Name:  " + entry.getKey())
        println("    Value: " + entry.getValue())
    }

    return true
}
