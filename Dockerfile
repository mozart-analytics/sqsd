FROM webratio/groovy:2.3.8
MAINTAINER Manuel Ortiz Bey <ortiz.manuel@mozartanalytics.com>

# Create sqsd directory
WORKDIR /
RUN mkdir /sqsd

# Copy sqsd source including
COPY ./src /sqsd

# Run sqsd
WORKDIR /sqsd
ENTRYPOINT ["groovy"]
CMD ["sqsd"]
