
Copyright (C) 2014 Google Inc.

# Java Managed VMs Tutorial #

The [App Engine Managed VMs](https://developers.google.com/appengine/docs/managed-vms/) hosting environment lets you run App Engine applications on configurable Compute Engine Virtual Machines (VMs). This VM-based hosting environment offers more flexibility and provides more CPU and memory options. Applications that run on Managed VMs are not subject to Java and Python runtime restrictions, and they have access to all the Compute Engine machine types. You can also add third-party libraries and frameworks to your app.
Managed VMs instances are [Docker](https://www.docker.com/)-container-based, and with the Preview gcloud SDK, it is now possible to edit the `Dockerfile` configuration used by a module's instances.

This tutorial walks through use of Managed VMs and the new gcloud SDK for a Java app; shows how you can test Managed VMs locally as well as deploy using the new SDK; and shows how to use a non-default `Dockerfile`.

The code for this tutorial is here: [https://github.com/GoogleCloudPlatform/appengine-java-vm-guestbook-extras/](https://github.com/GoogleCloudPlatform/appengine-java-vm-guestbook-extras/).
It includes several stages of a sample app.

The first stage of the example shows how you can 'escape the App Engine sandbox' by using some Java libraries that don't run on App Engine.
The second stage shows how you can edit a Managed VM module's `Dockerfile` to further configure its instances.  In this case, we'll install a linux utility, and also write to the instances' local filesystem.
The third stage shows how you can install Java 8 on your Managed VM instance via `Dockerfile` config.

## Initial Setup ##

First, complete the following steps:

- [Create your project](https://developers.google.com/appengine/docs/managed-vms/) and have it enabled for Managed VMs.
- Install Docker in a local VM, as [described here](http://goo.gl/bpxIuj).
  **Note: make sure your VirtualBox VM has 4Gb or RAM (or more). Otherwise, the Java runtime may have issues.**
- Download and install [the Preview build of the Google Cloud SDK](https://console.developers.google.com/m/cloudstorage/b/managed-vm-sdk/o/managed-vm-sdk-latest.zip),  as [described here](http://goo.gl/bpxIuj).

### Gcloud Authentication ###

Be sure to first authenticate with:

	gcloud auth login

### Install Maven ###

This tutorial uses Maven to build its Java projects, so [install Maven](http://maven.apache.org/download.cgi) as necessary.

### Grab the Sample Code  ###

Then, grab the starter code that we'll use for this tutorial, from this repo: [https://user.git.corp.google.com/amyu/java-mvms-guestbook/](https://user.git.corp.google.com/amyu/java-mvms-guestbook/).
This app uses as its starting point the (familiar to many) App Engine "guestbook" sample, but some extras are added that highlight the capabilities of Managed VMs.  Here, we'll assume familiarity with the basic Guestbook app and plunge into the new stuff.

**Make sure that Docker is running** before starting the next steps.  You may also want to run:
`export DOCKER_HOST=tcp://192.168.59.103:2375`
in your shell.


## Stage 1 of the Sample App: Escape the Sandbox ##

With Managed VMs, you can  run outside the traditional App Engine instance 'sandbox'.
In this section of the tutorial, we're going to use the java.awt.*  package, which does not run on App Engine's sandboxed instances, to build 'captcha' support for the guestbook app.
(In the next section, we'll write to the file system, which also is not supported by a sandboxed instance).

Go to the `stage1` directory of the downloaded sample.  Take a look at `src/main/webapp/WEB-INF/appengine-web.xml`.  You'll see that it includes these settings:

    <vm>true</vm>
    <vm-settings>
        <setting name="machine_type" value="n1-standard-1"/>
    </vm-settings>
    <manual-scaling>
        <instances>1</instances>
    </manual-scaling>

This indicates that this app module (the 'default' module, in this case) is a Managed VMs module, and indicates that one instance of this module version should be started.

While you're looking at `appengine-web.xml`,  go ahead and change the <application> id to your app id.  (This is not necessary for running locally using the development server, but is necessary for deployment).

Before running the app, take a quick look at the  `src/main/java/com/google/appengine/demos/guestbook/CaptchaServlet.java` servlet, which is new.  It uses the java.awt.* package to generate and serve up a 'captcha' image, putting the corresponding 'captcha code' in the `Session`.

`guestbook.jsp` displays the captcha image, and asks the user to type in the code.  `SignGuestbookServlet` checks the submitted code against the value in the `Session`, and does not record the comment if the captcha code is incorrect.

### Run Your App Locally ###

First, run your app locally.  Build the app first:

	mvn package

Then, from the `stage1` directory, run:

	<google-cloud-sdk>/bin/gcloud  preview app run target/guestbook-1.0-SNAPSHOT

Then, visit the URL that the development server is running on (likely: [http://localhost:8080](http://localhost:8080)). You should see a figure that looks like the following. The app is the (probably all-too-familiar) guestbook app, but with a 'captcha' image and field added.  You must type in the captcha word correctly for the entry to be posted.

<img src="http://storage.googleapis.com/amy-jo/articles/gb_captcha_local.png" width="500" alt="Guestbook with Captcha"/>


### Deploy Your App ###

Next, try deploying your app to production.

First, set the project you're using with `gcloud`:

	<preview_google-cloud-sdk>/bin/gcloud config set project <your-project>

Make sure that you're using a Managed-VMs-enabled app, and have edited `src/main/webapp/WEB-INF/appengine-web.xml` to use that app id. (When you change the app id, remember to rebuild using `mvn package`).  Then do:

	<preview_google-cloud-sdk>/bin/gcloud preview app deploy \
	  --server preview.appengine.google.com target/guestbook-1.0-SNAPSHOT

This deployment is using the 'default'  `Dockerfile`, which you can see in the `<preview_google-cloud-sdk>/docker/dockerfiles` directory.  It contains just:

	FROM google/appengine-java
	ADD . /home/vmagent/appengine-java-vmruntime/webapps/root


After deployment, go to your app: http://<your-app-id>.appspot.com.
The app should work the same as it did with the local development server.    You'll see the captcha image— this code wouldn't have worked with 'regular' App Engine instances!

## Stage 2: Configure a Dockerfile for the app ##

In Stage 2 of this app, we will to use the linux 'fortune' program to autofill in the guestbook entries with 'suggested text', in case a guest has a case of writer's block.  You'll find this version of the app in the `stage2` directory.

These changes involve a couple of new things.

First, we need to install the 'fortune' program on our Managed VM instances, so that we can access it. We will do this by defining a `Dockerfile` for the app.
Then, we will define a new class (called `FortuneInfo`), that will exec this program, save the results to a new file, then read the file and serve up the results.
Take a quick look at `FortuneInfo.java`.  Both the use of `ProcessBuilder`, and and the temp file writes to the local filesystem, would not work on 'regular' App Engine instances.

<img src="http://storage.googleapis.com/amy-jo/articles/gb_captcha_local2.png" width="500" alt="Guestbook with Fortunes"/>

Take a look at the `Dockerfile` in the `stage2/src/main/webapp` directory.
It looks like this:

	FROM google/appengine-java
	ADD . /home/vmagent/appengine-java-vmruntime/webapps/root

	RUN apt-get update && apt-get install -y fortunes

The file indicates to: start with the default java runtime docker image, and add to it an installation of the 'fortunes' program.

Build your app, via `mvn package`.

### Run Your App Locally ###

As described above for Stage 1, build your app and run it locally:

	<preview_google-cloud-sdk>/bin/gcloud  preview app run target/guestbook-1.0-SNAPSHOT

You should now see the guestbook entry field autofilled with a randomly-selected 'fortune'.

### Deploy Your App ###

Deploy your app using the same instructions as above for the Stage 1 version of the app:

	<preview_google-cloud-sdk>/bin/gcloud preview app deploy \
	  --server preview.appengine.google.com target/guestbook-1.0-SNAPSHOT
It should look just the same as you saw in the local development server— again, with code that would not have run on sandboxed 'regular' App Engine instances.

## Stage 3: Install Java 8 on Your Managed VM Instances ##

As a final stage of this tutorial, we will show how you can run your app using Java 8 (not yet supported on 'regular' App Engine instances), by adding additional commands to the app's `Dockerfile`.  You can find the code for this version of the app in the `stage3` directory.

First, edit your Dockerfile to look like the following.

	FROM google/appengine-java
	RUN apt-get update && apt-get install -y fortunes
	# Install java8
	RUN echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu precise main" | tee -a /etc/apt/sources.list
	RUN echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu precise main" | tee -a /etc/apt/sources.list
	RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
	RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys EEA14886
	RUN apt-get update
	RUN apt-get install -y oracle-java8-installer

        ADD . /home/vmagent/appengine-java-vmruntime/webapps/root


Then, `guestbook.jsp` has been modified to grab the Java version from the system properties, and print it at the top of the page.

### Local Testing and Deployment ###

The local testing and deployment process is the same as above.
This time, when you visit your app, you should see at the top of the page an indication of the java version you're running.  It should look something like the figure below.

<img src="http://storage.googleapis.com/amy-jo/articles/java8_mvms.png" width="500" alt="Guestbook on Java 8"/>


## Summary ##

This tutorial walked through use of Managed VMs and the new gcloud SDK for a Java app, based on an extended version of the "guestbook" app. It showed how you can test Managed VMs locally, as well as deploy using the new SDK; and showed how to "escape the sandbox" and use a non-default `Dockerfile`.










