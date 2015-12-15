# Java Managed VMs Tutorial


The [App Engine Managed VMs](https://developers.google.com/appengine/docs/managed-vms/) hosting environment lets you run App Engine applications on configurable Compute Engine Virtual Machines (VMs). This VM-based hosting environment offers more flexibility and provides more CPU and memory options. Applications that run on Managed VMs are not subject to Java and Python runtime restrictions, and they have access to all the Compute Engine machine types. You can also add third-party libraries and frameworks to your application.
Managed VMs instances are [Docker](https://www.docker.com/)-container-based, and with the Beta gcloud SDK, it is now possible to edit the `Dockerfile` configuration used by a module's instances.

This tutorial walks through the use of Managed VMs and the new gcloud SDK for a Java Web Application. It shows how you can test Managed VMs locally as well as deploy using the new SDK; and shows how to use a non-default `Dockerfile`.

The code for this tutorial is here: [https://github.com/GoogleCloudPlatform/appengine-java-vm-guestbook-extras](https://github.com/GoogleCloudPlatform/appengine-java-vm-guestbook-extras).
It includes several stages of a sample app.

The first stage of the example shows how you can 'escape the App Engine sandbox' by using some Java libraries that don't run on App Engine.
The second stage shows how you can edit a Managed VM module's `Dockerfile` to further configure its instances.  In this case, we'll install a linux utility, and also write to the instances' local filesystem. We will also use the latest Jetty 9.3.2 runtime that needs the Open JDK8  JVM.

## Initial Setup ##

First, complete the following steps:

- [Create your project](https://developers.google.com/appengine/docs/managed-vms/) and have it enabled for Managed VMs.

### Gcloud Authentication ###

Be sure to first authenticate with:

	$ gcloud auth login

### Install Maven and Git###

This tutorial uses Maven 3.1 or above to build its Java projects, so [install Maven](http://maven.apache.org/download.cgi) as necessary.
Be familiar with the Managed VMs Maven specific documentation located at [https://cloud.google.com/appengine/docs/java/managed-vms/maven](https://cloud.google.com/appengine/docs/java/managed-vms/maven)

If you are new to git, please refer to the [git documentation](http://git-scm.com/docs).

### Grab the Sample Code  ###

Then, grab the starter code that we'll use for this tutorial, from this repo: [https://github.com/GoogleCloudPlatform/appengine-java-vm-guestbook-extras](https://github.com/GoogleCloudPlatform/appengine-java-vm-guestbook-extras).

    $ git clone https://github.com/GoogleCloudPlatform/appengine-java-vm-guestbook-extras.git
    $ cd stage1

This app uses as its starting point the (familiar to many) App Engine "guestbook" sample, but some extras are added that highlight the capabilities of Managed VMs.  Here, we'll assume familiarity with the basic Guestbook app and plunge into the new stuff.

The 2 stages shown in this tutorial are:

| Stage  |Description           |
| ------- |-------------|
| stage1 | Add a captcha library using AWT to the GuestBook application |
| stagebis | Customize the Dockerfile to install a Linux native package and call it from Java, writing to the local file system and uses Open JDK8 |

All stages use Maven and Servlet 3.1 features, with Debug enabled, and are executed inside a Docker container on the local development server. The exact same Docker container will be running in production when your deploy your application.


## Stage 1-Escape the Sandbox ##

With Managed VMs, you can  run outside the traditional App Engine instance 'sandbox'.
In this section of the tutorial, we're going to use the java.awt.*  package, which does not run on App Engine's sandboxed instances, to build 'captcha' support for the guestbook app.
(In the next section, we'll write to the file system, which also is not supported by a sandboxed instance).

Go to the [stage1](stage1) directory of the downloaded sample.  Take a look at [stage1/src/main/webapp/WEB-INF/appengine-web.xml](stage1/src/main/webapp/WEB-INF/appengine-web.xml).  You'll see that it includes these settings:

    <vm>true</vm>
    <beta-settings>
        <setting name="java_quickstart" value="true"/>
    </beta-settings>
    <manual-scaling>
        <instances>1</instances>
    </manual-scaling>

This indicates that this app module (the 'default' module, in this case) is a Managed VMs module, and indicates that one instance of this module version should be started.

Notice the `java_quickstart` setting: it allows you to use some advanced Servlet 3.1 annotations processing developed for the Jetty Web Server. For more details about the `java_quickstart`feature, you can see this article: [https://webtide.com/jetty-9-quick-start/](https://webtide.com/jetty-9-quick-start/), or refer to this [JavaOne 2014 presentation](https://oracleus.activeevents.com/2014/connect/fileDownload/session/A53E3FEF3C8321FF7542202FA4B4D791/CON5100_Moussine-Pouchkine-Java%20in%20the%20Cloud-%20The%20Good%20Parts%20\(JavaOne%202014\).pdf).

While you're looking at `appengine-web.xml`,  go ahead and change the <application> id to your app id.  (This is not necessary for running locally using the development server, but is necessary for deployment).

Before running the app, take a quick look at the  [stage1/src/main/java/com/google/appengine/demos/guestbook/CaptchaServlet.java](stage1/src/main/java/com/google/appengine/demos/guestbook/CaptchaServlet.java) servlet, which is new.  It uses the java.awt.* package to generate and serve up a 'captcha' image, putting the corresponding 'captcha code' in the `Session`. This is also a Servlet 3.1 annotated servlet, so no need to define it in the web.xml file.

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

First, run the gcloud:run Maven target that will compile your project and start locally the development server and create the correct Docker container to execute your application:

	$ mvn gcloud:run

If this does not work, it is possible that you did not install the Cloud SDK or it is not installed in the default location (under you home directory and the google-cloud-sdk/ directory). You can tell Maven a different location by changing the pom.xml and using the gcloud_directory parameter:

       <plugin>
        <groupId>com.google.appengine</groupId>
        <artifactId>gcloud-maven-plugin</artifactId>
        <version>2.0.9.88.v20151125</version>
        <configuration>
          <gcloud_directory>/YOUR/OWN/GCLOUD/INSTALLATION/DIR</gcloud_directory>
          ...


After some initialization steps (validation, build of the Docker image and execution of a Docker container that contains your application)

    ...
		<<< gcloud-maven-plugin:2.0.9.81.v20151008:run (default-cli) < package @ guestbook-stage1
		
		--- gcloud-maven-plugin:2.0.9.81.v20151008:run (default-cli) @ guestbook-stage1 ---
		
		Running gcloud app run...
		Creating staging directory in: /Users/ludo/appengine-java-vm-guestbook-extras/stage1/target/appengine-staging
		Running appcfg --enable_quickstart --disable_update_check -A notused stage /Users/ludo/appengine-java-vm-guestbook-extras/stage1/target/guestbook-stage1-1.0-SNAPSHOT /Users/ludo/appengine-java-vm-guestbook-extras/stage1/target/appengine-staging
		Reading application configuration data...
		Oct 11, 2015 5:19:28 PM com.google.apphosting.utils.config.IndexesXmlReader readConfigXml
		INFO: Successfully processed /Users/ludo/appengine-java-vm-guestbook-extras/stage1/target/guestbook-stage1-1.0-SNAPSHOT/WEB-INF/datastore-indexes.xml
		
		
		Beginning interaction for module default...
		Success.
		Temporary staging for module default directory left in /Users/ludo/appengine-java-vm-guestbook-extras/stage1/target/appengine-staging
		Running python -S /Users/ludo/google-cloud-sdk/platform/google_appengine/dev_appserver.py --skip_sdk_update_check=true -A app /Users/ludo/appengine-java-vm-guestbook-extras/stage1/target/guestbook-stage1-1.0-SNAPSHOT/app.yaml
		INFO     2015-10-12 00:19:30,519 application_configuration.py:403] No version specified. Generated version id: 20151012t001930
		INFO     2015-10-12 00:19:30,520 devappserver2.py:763] Skipping SDK update check.
		INFO     2015-10-12 00:19:30,567 api_server.py:205] Starting API server at: http://localhost:50916
		INFO     2015-10-12 00:19:30,576 dispatcher.py:197] Starting module "default" running at: http://localhost:8080
		INFO     2015-10-12 00:19:30,584 admin_server.py:116] Starting admin server at: http://localhost:8000
		2015-10-12 00:19:30.888:INFO::main: Logging initialized @293ms
		2015-10-12 00:19:31.056:INFO:oejs.Server:main: jetty-9.2.10.v20150310
		2015-10-12 00:19:31.068:WARN:oejsh.RequestLogHandler:main: !RequestLog
		2015-10-12 00:19:31.070:INFO:oejdp.ScanningAppProvider:main: Deployment monitor [file:/Users/ludo/google-cloud-sdk/platform/google_appengine/google/appengine/tools/java/lib/jetty-base-sdk/contexts/] at interval 1
		Oct 12, 2015 12:19:31 AM com.google.apphosting.vmruntime.VmMetadataCache getMetadata
		INFO: Meta-data 'attributes/gae_affinity' path retrieval error: metadata
		Oct 12, 2015 12:19:31 AM com.google.apphosting.vmruntime.VmMetadataCache getMetadata
		INFO: Meta-data 'attributes/gae_appengine_hostname' path retrieval error: metadata
		Oct 12, 2015 12:19:31 AM com.google.apphosting.vmruntime.VmMetadataCache getMetadata
		INFO: Meta-data 'attributes/gae_use_nginx_proxy' path retrieval error: metadata
		Oct 12, 2015 12:19:31 AM com.google.apphosting.vmruntime.VmMetadataCache getMetadata
		INFO: Meta-data 'attributes/gae_tmp_force_reuse_api_connection' path retrieval error: metadata
		2015-10-12 00:19:31.625:INFO:oejsh.ContextHandler:main: Started c.g.a.v.j.VmRuntimeWebAppContext@6ce139a4{/,file:/Users/ludo/appengine-java-vm-guestbook-extras/stage1/target/guestbook-stage1-1.0-SNAPSHOT/,AVAILABLE}{/Users/ludo/a/remove/appengine-java-vm-guestbook-extras/stage1/target/guestbook-stage1-1.0-SNAPSHOT}
		2015-10-12 00:19:31.638:INFO:oejs.ServerConnector:main: Started ServerConnector@489115ef{HTTP/1.1}{0.0.0.0:35807}
		INFO     2015-10-12 00:19:32,594 module.py:1735] New instance for module "default" serving on:
    http://localhost:8080

you can visit the URL that the development server is running on (likely: [http://localhost:8080](http://localhost:8080)). You should see a figure that looks like the following. The application is the (probably all-too-familiar) guestbook app, but with a 'captcha' image and field added.  You must type in the captcha word correctly for the entry to be posted.

<img src="http://storage.googleapis.com/amy-jo/articles/gb_captcha_local.png" width="500" alt="Guestbook with Captcha"/>


**Note for IDE users**: If you are using NetBeans or Eclipse, you can stop the Cloud SDK run session with a click on the little RED icon that stop a process in the IDE terminal view. There is also a RED icon button in Android Studio and Intellij, but this one will not stop correctly the Cloud SDK: The docker containers will not be stopped and you need to stop them from the command line. You can instead execute the Maven command from CLI or the IDES to safely stop the running processes:

    $ mvn gcloud:run_stop


### Deploy Your Application ###

Next, try deploying your application to production.

First, set the project you're using with `gcloud`:

	$ gcloud config set project <your-project>

Make sure that you're using a Managed-VMs-enabled app, ins [stage1/src/main/webapp/WEB-INF/appengine-web.xml](stage1/src/main/webapp/WEB-INF/appengine-web.xml).  Then do:

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

<img src="http://storage.googleapis.com/amy-jo/articles/gb_captcha_local2.png" width="500" alt="Guestbook with Fortunes"/>

Take a look at the [stage3/src/main/webapp/Dockerfile](stage3/src/main/webapp/Dockerfile) file:
It looks like this:

     #jetty9-compat is Jetty 9.3.2 and support only Open JDK8:
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

You can also specify a custom_entrypoint in your project pom.xml. This is an executable that the Cloud SDK will run to start your application locally. If you want to use the Cloud SDK bundled Jetty9 Web Server, you can define this entry point:

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

You can of course put in the custom_entrypoint any process that would fit your custom execution environment.


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
