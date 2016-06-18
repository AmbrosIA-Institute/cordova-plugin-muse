# cordova-plugin-muse
Cordova Plugin for the InteraXon Muse EEG headset

This plugin requires that you also have a copy of version 1.3.0 of Android LibMuse. 

Copy the following files from LibMuse into the relevant directories:

libmuse/libmuse_android_1.3.0/libs/libmuseandroid.jar -> src/android/libs  
libmuse/libmuse_android_1.3.0/libs/x86/libmuse.so     -> src/android/libs/x86  
libmuse/libmuse_android_1.3.0/libs/armeabi/libmuse.so -> src/android/libs/armeabi  

Apache Commons Collections, Apache Commons Lang, and Apache Commons Math are conveniently included here under version 2.0 of the Apache License. 

All other included files are Copyright 2016 The AmbrosIA Institute and released under version 3 of the GNU LGPL.
