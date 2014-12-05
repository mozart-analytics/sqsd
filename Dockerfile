FROM webratio/groovy:2.3.8
MAINTAINER Manuel Ortiz Bey <ortiz.manuel@mozartanalytics.com>

# Create sqsd directory
WORKDIR /
RUN mkdir /sqsd
RUN mkdir /sqsd/config

# Copy sqsd source and config
COPY ./src /sqsd
COPY ./config /sqsd/config

# Run sqsd
WORKDIR /sqsd
ENTRYPOINT ["groovy"]
CMD ["sqsd"]
