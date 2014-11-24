sqsd
====

sqsd : A simple alternative to the Amazon SQS Daemon "sqsd"

## USAGE

### Configuration
There are 2 ways to configure the `sqsd`'s properties: Environment Variables or a configuration file. You must set one of the two options.

##### Configuration File
Custom properties are loaded from `config/sqsd-config.groovy`.

##### Environment Variables
Environment Variables and defaults are loaded from `config/sqsd-default-config.groovy`.

- `AWS_ACCESS_KEY_ID` - Your AWS Access Key (**required**)
- `AWS_SECRET_ACCESS_KEY` - Your AWS secret access secret (**required**)
- `SQS_QUEUE_REGION_NAME` - The region name of the AWS SQS queue (default: `us-east-1`)
- `SQSD_QUEUE_URL` - Your queue URL. You can instead use the queue name but this takes precedence over queue name. (**optional/required**)
- `SQSD_QUEUE_NAME` - Your queue name (**optional/required**)
- `SQSD_MAX_MESSAGES_PER_REQUEST` - Max number of messages to retrieve per request (max: `10`, default: `10`)
- `SQSD_WAIT_TIME_SECONDS` - Long polling wait time when querying the queue (max: `20`, default: `20`)
- `SQSD_HTTP_HOST` - Host address to your service (default: `http://127.0.0.1`)
- `SQSD_HTTP_PATH` - Your service endpoint/path where to POST the messages (default: `/`)
- `SQSD_HTTP_REQUEST_CONTENT_TYPE` - Message MIME Type (default: `application/json`)

### Run

This script has been tested with Groovy 2.3.7

##### Command Line
    groovy sqsd.groovy

##### Docker - localhost service
    cd /your/sqsd/local/path
	docker build -t someImageName .
	docker run someImageName

##### Docker - remote service
TBD

##### Docker - linked containers service
TBD

