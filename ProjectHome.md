Device Management Demo for Android is a sample project that demonstrates a technique to secure an Android application by using the [Android Device Management API](http://developer.android.com/guide/topics/admin/device-admin.html).  This project consists of an Android client and a server-side application written for [Google App Engine for Java](http://code.google.com/appengine/docs/java/overview.html).  The Android client has dependency on the [c2dm library](http://code.google.com/p/chrometophone/source/browse/#svn%2Ftrunk%2Fandroid%2Fc2dm) in the [chrometophone](http://http://code.google.com/p/chrometophone/) project.

Features
  * Create and configure simple password policies on server
  * Enforce password policies on Android client
  * Remotely activate screen-lock on Android client

**Note:** Android 2.2 or above required.  Android Device Management Sample is powered by the Android Device Management API and Android Cloud to Device Messaging (C2DM) service, which require Android 2.2 or later.


---

Demo Instance
  * A live policy server demo instance is hosted at [https://android-dm-demo.appspot.com](https://android-dm-demo.appspot.com).
  * Note that the server is used for demonstration purposes only.  Information stored there may be deleted periodically.