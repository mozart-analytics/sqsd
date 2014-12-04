FROM webratio/gvm:2.3.7
MAINTAINER Manuel Ortiz Bey <ortiz.manuel@mozartanalytics.com>

# Create sqsd directory
WORKDIR /
RUN mkdir /sqsd

# Copy sqsd source and config
COPY ./src /sqsd
COPY ./config

# Run sqsd
WORKDIR /sqsd
CMD ["groovy sqsd"]
