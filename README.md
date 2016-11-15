# Java App Engine Flexible Tutorial


The [App Engine Flexible
Environment](https://cloud.google.com/appengine/docs/flexible/) lets you run App
Engine applications on configurable Compute Engine Virtual Machines (VMs). This
VM-based hosting environment offers more flexibility and provides more CPU and
memory options. Applications that run on App Engine Flexible are not subject to
Java and Python runtime restrictions, and they have access to all the Compute
Engine machine types. You can also add third-party libraries and frameworks to
your application. App Engine Flexible instances are
[Docker](https://www.docker.com/)-container-based, and with the Beta gcloud SDK,
it is now possible to edit the `Dockerfile` configuration used by a module's
instances.

This tutorial walks through the use of App Engine Flexible and the new gcloud
SDK for a Java Web Application. It shows how you can test App Engine Flexible
locally as well as deploy using the new SDK; and shows how to use a non-default
`Dockerfile`.

The code for this tutorial is here:
https://github.com/GoogleCloudPlatform/appengine-java-vm-guestbook-extras.
It includes several stages of a sample app.

The first stage of the example shows how you can 'escape the App Engine sandbox'
by using some Java libraries that don't run on App Engine.  The second stage
shows how you can edit a App Engine Flexible module's `Dockerfile` to further
configure its instances.  In this case, we'll install a linux utility, and also
write to the instances' local filesystem. We will also use the latest Jetty
9.3.2 runtime that needs the Open JDK8  JVM.

## Initial Setup ##

First, complete the following steps:

- [Create your project](https://console.cloud.google.com)

### Gcloud Authentication ###

Be sure to first authenticate with:

    $ gcloud auth login

### Install Maven and Git###

This tutorial uses Maven 3.1 or above to build its Java projects, so [install
Maven](http://maven.apache.org/download.cgi) as necessary.

Be familiar with the App Engine Flexible [Maven specific
documentation](https://cloud.google.com/appengine/docs/flexible/java/using-maven)

If you are new to git, please refer to the [git documentation](http://git-scm.com/docs).

### Grab the Sample Code  ###

Then, grab the starter code that we'll use for this tutorial, from this repo:
https://github.com/GoogleCloudPlatform/appengine-java-vm-guestbook-extras.

    $ git clone https://github.com/GoogleCloudPlatform/appengine-java-vm-guestbook-extras.git
    $ cd stage1

This app uses as its starting point the (familiar to many) App Engine
"guestbook" sample, but some extras are added that highlight the capabilities of
App Engine Flexible.  Here, we'll assume familiarity with the basic Guestbook
app and plunge into the new stuff.

The 2 stages shown in this tutorial are:

| Stage  |Description           |
| ------- |-------------|
| stage1 | Add a captcha library using AWT to the GuestBook application |
| stagebis | Customize the Dockerfile to install a Linux native package and call it from Java, writing to the local file system and uses Open JDK8 |

All stages use Maven and Servlet 3.1 features, with Debug enabled, and are executed inside a Docker container on the local development server. The exact same Docker container will be running in production when your deploy your application.


## Stage 1-Escape the Sandbox ##

With App Engine Flexible, you can  run outside the traditional App Engine
instance 'sandbox'.  In this section of the tutorial, we're going to use the
`java.awt.*` package, which does not run on App Engine's sandboxed instances, to
build 'captcha' support for the guestbook app.  (In the next section, we'll
write to the file system, which also is not supported by a sandboxed instance).

Go to the [stage1](stage1) directory of the downloaded sample.  Take a look at [stage1/src/main/webapp/WEB-INF/appengine-web.xml](stage1/src/main/webapp/WEB-INF/appengine-web.xml).  You'll see that it includes these settings:

    <vm>true</vm>
    <beta-settings>
        <setting name="java_quickstart" value="true"/>
    </beta-settings>
    <manual-scaling>
        <instances>1</instances>
    </manual-scaling>

This indicates that this app module (the 'default' module, in this case) is a Managed VMs module, and indicates that one instance of this module version should be started.

Notice the `java_quickstart` setting: it allows you to use some advanced Servlet 3.1 annotations processing developed for the Jetty Web Server. For more details about the `java_quickstart` feature, you can see this article: [https://webtide.com/jetty-9-quick-start/](https://webtide.com/jetty-9-quick-start/), or refer to this [JavaOne 2014 presentation](https://oracleus.activeevents.com/2014/connect/fileDownload/session/A53E3FEF3C8321FF7542202FA4B4D791/CON5100_Moussine-Pouchkine-Java%20in%20the%20Cloud-%20The%20Good%20Parts%20\(JavaOne%202014\).pdf).

While you're looking at `appengine-web.xml`,  go ahead and change the <application> id to your app id.  (This is not necessary for running locally using the development server, but is necessary for deployment).

Before running the app, take a quick look at the  [stage1/src/main/java/com/google/appengine/demos/guestbook/CaptchaServlet.java](stage1/src/main/java/com/google/appengine/demos/guestbook/CaptchaServlet.java) servlet, which is new.  It uses the `java.awt.*` package to generate and serve up a 'captcha' image, putting the corresponding 'captcha code' in the `Session`. This is also a Servlet 3.1 annotated servlet, so no need to define it in the web.xml file.

[stage1/src/main/webapp/guestbook.jsp](stage1/src/main/webapp/guestbook.jsp) displays the captcha image, and asks the user to type in the code.  [stage1/src/main/java/com/google/appengine/demos/guestbook/SignGuestbookServlet.java](stage1/src/main/java/com/google/appengine/demos/guestbook/SignGuestbookServlet.java) checks the submitted code against the value in the `Session`, and does not record the comment if the captcha code is incorrect.


#### Maven Deploy on Save ####
The Maven project is configured to enable the fast "Deploy on Save" feature that IDEs like NetBeans, Eclipse, Android Studio or Intellij support. The Deploy on Save feature will recompile the Java files in place or update the Web Content, and the Google Cloud SDK will detect the file change and trigger automatically a build of a new Docker container with the updated application, allowing very fast development cycles. This is the preferred way of working for productive developers. Some features will not be supported, like for example, when you change some appengine-web.xml or if you add or modify a Servlet 3.1 annotations, but for most changes, it is the fastest way to see them live immediately.
The trick for Deploy on Save is in the [stage1/pom.xml](stage1/pom.xml) file, regarding the `outputDirectory` setup.

    <build>
      <!-- needed for enabling compile/reload on save in modern IDEs...-->
      <outputDirectory>target/${project.artifactId}-${project.version}/WEB-INF/classes
      </outputDirectory>
     <plugins>
     ...


### Run Your Application Locally ###

First, run the `appengine:run` Maven target that will compile your project and start locally the development server and create the correct Docker container to execute your application:

    $ mvn appengine:run

If this does not work, it is possible that you did not install the Cloud SDK or it is not installed in the default location (under you home directory and the google-cloud-sdk/ directory). You can tell Maven a different location by setting the `GOOGLE_CLOUD_SDK_HOME` environment variable:

       export GOOGLE_CLOUD_SDK_HOME=/path/to/google-cloud-sdk/


After some initialization steps (validation, build of the Docker image and execution of a Docker container that contains your application)

    ...
    [INFO] <<< appengine-maven-plugin:1.0.0:run (default-cli) < package @ guestbook-stage1 <<<
    [INFO]
    [INFO] --- appengine-maven-plugin:1.0.0:run (default-cli) @ guestbook-stage1 ---
    Nov 15, 2016 11:01:20 AM com.google.cloud.tools.appengine.cloudsdk.CloudSdk logCommand
    INFO: submitting command: /path/to/google-cloud-sdk/bin/dev_appserver.py /path/to/appengine-java-vm-guestbook-extras/stage1/target/guestbook-stage1-1.0-SNAPSHOT --jvm_flag=-Dappengine.user.timezone=UTC
    [INFO] GCLOUD: 2016-11-15 19:01:21.782:INFO::main: Logging initialized @150ms
    [INFO] GCLOUD: 2016-11-15 19:01:23.840:WARN:oeja.AnnotationConfiguration:main: ServletContainerInitializers: detected. Class hierarchy: empty
    [INFO] GCLOUD: INFO     2016-11-15 19:01:23,954 application_configuration.py:431] No version specified. Generated version id: 20161115t190123
    [INFO] GCLOUD: INFO     2016-11-15 19:01:23,954 devappserver2.py:769] Skipping SDK update check.
    [INFO] GCLOUD: WARNING  2016-11-15 19:01:24,059 simple_search_stub.py:1146] Could not read search indexes from /tmp/appengine.None.user/search_indexes
    [INFO] GCLOUD: INFO     2016-11-15 19:01:24,061 api_server.py:205] Starting API server at: http://localhost:37349
    [INFO] GCLOUD: INFO     2016-11-15 19:01:24,069 dispatcher.py:197] Starting module "default" running at: http://localhost:8080
    [INFO] GCLOUD: INFO     2016-11-15 19:01:24,070 admin_server.py:116] Starting admin server at: http://localhost:8000
    [INFO] GCLOUD: 2016-11-15 19:01:24.354:INFO::main: Logging initialized @273ms
    [INFO] GCLOUD: 2016-11-15 19:01:24.452:INFO:oejs.Server:main: jetty-9.2.10.v20150310
    [INFO] GCLOUD: 2016-11-15 19:01:24.460:WARN:oejsh.RequestLogHandler:main: !RequestLog
    [INFO] GCLOUD: 2016-11-15 19:01:24.461:INFO:oejdp.ScanningAppProvider:main: Deployment monitor [file:/path/to/google-cloud-sdk/platform/google_appengine/google/appengine/tools/java/lib/jetty-base-sdk/contexts/] at interval 1
    [INFO] GCLOUD: Nov 15, 2016 7:01:24 PM com.google.apphosting.vmruntime.VmMetadataCache getMetadata
    [INFO] GCLOUD: INFO: Meta-data 'attributes/gae_affinity' path retrieval error: metadata
    [INFO] GCLOUD: Nov 15, 2016 7:01:24 PM com.google.apphosting.vmruntime.VmMetadataCache getMetadata
    [INFO] GCLOUD: INFO: Meta-data 'attributes/gae_appengine_hostname' path retrieval error: metadata
    [INFO] GCLOUD: Nov 15, 2016 7:01:24 PM com.google.apphosting.vmruntime.VmMetadataCache getMetadata
    [INFO] GCLOUD: INFO: Meta-data 'attributes/gae_use_nginx_proxy' path retrieval error: metadata
    [INFO] GCLOUD: 2016-11-15 19:01:25.348:INFO:oejsh.ContextHandler:main: Started c.g.a.v.j.VmRuntimeWebAppContext@1fe20588{/,file:/path/to/appengine-java-vm-guestbook-extras/stage1/target/guestbook-stage1-1.0-SNAPSHOT/,AVAILABLE}{/path/to/appengine-java-vm-guestbook-extras/stage1/target/guestbook-stage1-1.0-SNAPSHOT}
    [INFO] GCLOUD: 2016-11-15 19:01:25.355:INFO:oejs.ServerConnector:main: Started ServerConnector@3a52dba3{HTTP/1.1}{0.0.0.0:58863}
    [INFO] GCLOUD: 2016-11-15 19:01:25.355:INFO:oejs.Server:main: Started @1275ms
    [INFO] GCLOUD: INFO     2016-11-15 19:01:26,079 module.py:1730] New instance for module "default" serving on:
    [INFO] GCLOUD: http://localhost:8080
    [INFO] GCLOUD:
    [INFO] GCLOUD: INFO     2016-11-15 19:01:26,219 module.py:788] default: "GET /_ah/start HTTP/1.1" 404 287


you can visit the URL that the development server is running on (likely: [http://localhost:8080](http://localhost:8080)). You should see a figure that looks like the following. The application is the (probably all-too-familiar) guestbook app, but with a 'captcha' image and field added.  You must type in the captcha word correctly for the entry to be posted.

![Guestbook with Captcha](http://storage.googleapis.com/amy-jo/articles/gb_captcha_local.png){: width="500"}

**Note for IDE users**: If you are using NetBeans or Eclipse, you can stop the Cloud SDK run session with a click on the little RED icon that stop a process in the IDE terminal view. There is also a RED icon button in Android Studio and Intellij, but this one will not stop correctly the Cloud SDK: The docker containers will not be stopped and you need to stop them from the command line. You can instead execute the Maven command from CLI or the IDES to safely stop the running processes:

    $ mvn appengine:stop


### Deploy Your Application ###

Next, try deploying your application to production.

First, set the project you're using with `gcloud`:

    $ gcloud config set project <your-project>

Make sure that you're using a Managed-VMs-enabled app, in [stage1/src/main/webapp/WEB-INF/appengine-web.xml](stage1/src/main/webapp/WEB-INF/appengine-web.xml), you have set `<vm>true</vm>`.  Then do:

    $ mvn gcloud:deploy

This deployment is using the 'default'  `Dockerfile`, which you can see in the `<preview_google-cloud-sdk>/docker/dockerfiles` directory.  It contains just:

    FROM gcr.io/google_appengine/java-compat
    ADD . /app


After deployment, go to your app: http://YOUR-APP-ID.appspot.com.
The app should work the same as it did with the local development server. This code would not have worked with a 'regular' App Engine instance!

## Stage bis-Configure a Dockerfile for the Application ##

In Stage bis (stage3 directory) of this application, we will upgrade the JVM to Open JDK8 and use the linux 'fortune' program to autofill in the guestbook entries with 'suggested text', in case a guest has a case of writer's block.
These changes involve a couple of new things.

First, we need to install the 'fortune' program on our Managed VM instances, so that we can access it. We will do this by defining a `Dockerfile` for the app.
Then, we will define a new class (called [stage3/src/main/java/com/google/appengine/demos/guestbook/FortuneInfo.java](stage3/src/main/java/com/google/appengine/demos/guestbook/FortuneInfo.java)), that will execute this program, save the results to a new file, then read the file and serve up the results.
Take a quick look at `FortuneInfo.java`.  Both the use of `ProcessBuilder`, and the capability of writing temporary files to the local filesystem, would not work on 'regular' App Engine instances.

![Guestbook with Fortunes](http://storage.googleapis.com/amy-jo/articles/gb_captcha_local2.png){: width="500"}

Take a look at the [stage3/src/main/webapp/Dockerfile](stage3/src/main/webapp/Dockerfile) file:
It looks like this:

     #jetty9-compat is Jetty 9.3.6 and supports only Open JDK8:
     FROM gcr.io/google_appengine/jetty9-compat
     RUN apt-get update && apt-get install -y fortunes

     ADD . /app


The file indicates to: start with the default java8 runtime docker image, and add to it an installation of the 'fortunes' program.

Build your app, via `mvn package`.

### Run Your Application Locally ###

As described above for Stage 1, build your app and run it locally:

    # Via Maven:
    $ mvn gcloud:run


You'll see an error:

      File "/Users/ludo/google-cloud-sdk/platform/google_appengine/google/appengine/tools/devappserver2/devappserver2.py", line 1026, in main
        dev_server.start(options)
      File "/Users/ludo/google-cloud-sdk/platform/google_appengine/google/appengine/tools/devappserver2/devappserver2.py", line 818, in start
        self._dispatcher.start(options.api_host, apis.port, request_data)
      File "/Users/ludo/google-cloud-sdk/platform/google_appengine/google/appengine/tools/devappserver2/dispatcher.py", line 194, in start
        _module.start()
      File "/Users/ludo/google-cloud-sdk/platform/google_appengine/google/appengine/tools/devappserver2/module.py", line 1554, in start
        self._add_instance()
      File "/Users/ludo/google-cloud-sdk/platform/google_appengine/google/appengine/tools/devappserver2/module.py", line 1706, in _add_instance
        expect_ready_request=True)
      File "/Users/ludo/google-cloud-sdk/platform/google_appengine/google/appengine/tools/devappserver2/custom_runtime.py", line 73, in new_instance
        assert self._runtime_config_getter().custom_config.custom_entrypoint
      File "/Users/ludo/google-cloud-sdk/platform/google_appengine/google/appengine/tools/devappserver2/module.py", line 382, in _get_runtime_config
        raise ValueError('The --custom_entrypoint flag must be set for '
    ValueError: The --custom_entrypoint flag must be set for custom runtimes

You can also specify a `custom_entrypoint` in your project pom.xml. This is an executable that the Cloud SDK will run to start your application locally. If you want to use the Cloud SDK bundled Jetty9 Web Server, you can define this entry point:

          <custom_entrypoint>java
            -Dgcloud.java.application=/Users/ludo/appengine-java-vm-guestbook-extras/stage3/target/guestbook-stage3-1.0-SNAPSHOT
            -Djetty.home=/Users/ludo/google-cloud-sdk/platform/google_appengine/google/appengine/tools/java/lib/java-managed-vm/appengine-java-vmruntime
            -Djetty.base=/Users/ludo/google-cloud-sdk/platform/google_appengine/google/appengine/tools/java/lib/jetty-base-sdk
            -Dcom.google.apphosting.vmruntime.VmRuntimeFileLogHandler.pattern=/SOMEWRITABLEDIR/log.%g
            -jar
            /Users/ludo/google-cloud-sdk/platform/google_appengine/google/appengine/tools/java/lib/java-managed-vm/appengine-java-vmruntime/start.jar
            -module=http
            jetty.port={port}
          </custom_entrypoint>

Please make sure you replace the hard coded paths to your Maven project location for the `gcloud.java.application` property as well as the Cloud SDK location that contains the `jetty.home` and `jetty.base` directories.

The `{port}` value is automatically set by the Cloud SDK to the correct port Jetty should be listening to.
This custom entry point basically tells the Cloud SDK to execute the Java Jett9 process witht the correct settings and with your exploded WAR directory located in the `target/guestbook-stage3-1.0-SNAPSHOT` directory.

You can of course put in the `custom_entrypoint` any process that would fit your custom execution environment.


### Deploy Your App ###

Deploy your application using the same instructions as above for the Stage 1 version of the app:

    # Via Maven:
    $ mvn gcloud:deploy


## Summary ##

This tutorial walked through use of Managed VMs and the new gcloud SDK for a Java app, based on an extended version of the "guestbook" app. It showed how you can test Managed VMs locally, as well as deploy using the new SDK; and showed how to "escape the sandbox" and use a non-default `Dockerfile`.

## Contributing changes

* See [CONTRIBUTING](CONTRIBUTING.md)

## Licensing

* See [LICENSE](LICENSE)

Copyright (C) 2014-2015 Google Inc.
