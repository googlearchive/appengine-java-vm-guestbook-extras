#jetty9-compat is Jetty 9.3.2 and support only Open JDK8:
FROM gcr.io/_b_jenkins_images/jetty9-compat:0.1
RUN apt-get update && apt-get install -y fortunes

ADD . /app
