/**
 * DO NOT CHANGE THIS FILE!!
 * Provides the default sqsd properties.
 * To change property values use the sqsd-default-config.groovy file or
 * create a new one and provide its location using the
 * `SQSD_CONFIG_FILE` env var
 * (e.g.: `export SQSD_CONFIG_FILE=~.sqsd/config.groovy`).
 */

/**
 * AWS SQS  related configuration env-vars
 */
aws.access_key_id = System.getenv("AWS_ACCESS_KEY_ID")
aws.secret_access_key = System.getenv("AWS_SECRET_ACCESS_KEY")
sqsd.queue.name = System.getenv("SQSD_QUEUE_NAME")
sqsd.queue.url = System.getenv("SQSD_QUEUE_URL")
sqsd.queue.region_name = System.getenv("SQS_QUEUE_REGION_NAME") ?: "us-east-1"
sqsd.max_messages_per_request = System.getenv("SQSD_MAX_MESSAGES_PER_REQUEST") as Integer ?: 10 // range: 1-10
sqsd.wait_time_seconds = System.getenv("SQSD_WAIT_TIME_SECONDS") as Integer ?: 20 // range: 1-20

/**
 * HTTP service related configuration env-vars
 */
sqsd.http.host = System.getenv("SQSD_HTTP_HOST") ?: "http://127.0.0.1"
sqsd.http.path = System.getenv("SQSD_HTTP_PATH") ?: "/"
sqsd.http.request.content_type = System.getenv("SQSD_HTTP_REQUEST_CONTENT_TYPE") ?: "application/json"

/**
 * Misc configuration env-vars
 */
sqsd.config_file = System.getenv("SQSD_CONFIG_FILE") ?: "config/sqsd-config.groovy"