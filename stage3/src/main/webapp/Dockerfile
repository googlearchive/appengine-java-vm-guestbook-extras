#jetty9-compat is Jetty 9.3.2 and support only Open JDK8:
FROM gcr.io/google_appengine/jetty9-compat
RUN apt-get update && apt-get install -y fortunes

ADD . /app
