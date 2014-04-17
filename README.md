WebFS
=====

Simple Web File Storage System

Web
===

Designed to have the fewest dependencies, the web server simply requires PHP with sqlite3 support. The web User Interface (Web UI) requires HTML 5 support and has been tested with Google Chrome.

Java
====

About
-----

Java API and utility programs provide tools with more functionality than the Web UI. These include document writers, console file system administration tool and a file synchronization graphical user interface.

Requires
========

Java
----
Requires various libraries from the Apache project. These are outlined in the maven pom.xml file in java/core and java/console.

WebFS also requires my utility java library, javalib, which can be found at https://github.com/kd0kfo/javalib.

Web
---

Server code runs on PHP 5 and requires sqlite3 support.

Build static page requires python package Jinja2.

The following web code is included:

* dropzone.js -- a very nice drap-and-drop library for javascript. May be found at https://github.com/enyo/dropzone and is released under the terms of MIT License.
* bootstrap -- Web frontend framework. Released with MIT License. Found at http://getbootstrap.com


Build Status
============

[![Build Status](https://travis-ci.org/kd0kfo/webfs.svg?branch=master)](https://travis-ci.org/kd0kfo/webfs)
