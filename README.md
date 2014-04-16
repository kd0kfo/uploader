uploader
========

Simple File Uploader System

Java
====

About
-----

Includes a java helper class/jar that simplifies uploading a file.

Requires
--------

Requires Apache Common and HttpComponents libraries. See java/lib/README.md for as list of tested jar files.

Requires my utility java library, javalib, which can be found at https://github.com/kd0kfo/javalib.

Build static page requires python package Jinja2.

Uses
----

* dropzone.js -- a very nice drap-and-drop library for javascript. May be found at https://github.com/enyo/dropzone and is released under the terms of MIT License.
* bootstrap -- Web frontend framework. Released with MIT License. Found at http://getbootstrap.com


Build
-----

To build, in java/ directory, run "ant jar". Required libraries must be placed in java/lib/ directory.


Build Status
------------

[![Build Status](https://travis-ci.org/kd0kfo/webfs.svg?branch=master)](https://travis-ci.org/kd0kfo/webfs)
