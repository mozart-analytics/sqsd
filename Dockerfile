FROM webratio/groovy:2.3.8
MAINTAINER Manuel Ortiz Bey <ortiz.manuel@mozartanalytics.com>

# Create sqsd directory
WORKDIR /
RUN mkdir /sqsd

# Copy sqsd source including
COPY ./src /sqsd

# Pre-Download sqsd dependencies
ENV AWS_SDK_VERSION 1.11.1
ENV HTTP_BUILDER_VERSION 0.7.2
RUN grape install com.amazonaws aws-java-sdk $AWS_SDK_VERSION
RUN grape install org.codehaus.groovy.modules.http-builder http-builder $HTTP_BUILDER_VERSION

# Run sqsd
WORKDIR /sqsd
ENTRYPOINT ["groovy"]
CMD ["sqsd"]
