FROM webratio/gvm:2.3.7
MAINTAINER Manuel Ortiz Bey <ortiz.manuel@mozartanalytics.com>

# Create sqsd Directory
WORKDIR /
RUN mkdir /sqsd

# Copy sqsd source
COPY ./src /sqsd

# Run sqsd
WORKDIR /sqsd
CMD ["groovy sqsd"]
