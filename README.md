sqsd
====

sqsd : A simple alternative to the Amazon SQS Daemon "sqsd"

## USAGE
### ENVIRONMENT VARIABLES
- `AWS_ACCESS_KEY_ID` - Your AWS Access Key (**required**)
- `AWS_SECRET_KEY` - Your AWS app secret (**required**)
- `SQS_QUEUE_NAME` - Your queue name (**required**)
- `AWS_REGION_NAME` - The AQS region (default: `us-east-1`)
- `MAX_NUMBER_OF_MESSAGES` - Max number of messages to consume per request (default: `10`)
- `HTTP_HOST` - Host address to your service (default: `http://127.0.0.1`)
- `HTTP_PATH` - Your service endpoint (default: `/`)
- `HTTP_REQUEST_CONTENT_TYPE` - Message MIME Type (default: `application/json`)