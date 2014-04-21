WebFS
=====

Simple Web File Storage System

License
=======

This software is free to be used under the terms of the GNU General Public License. For details, see the COPYING file or visit https://www.gnu.org/copyleft/gpl.html.

Warranty and Disclaimer
=======================

There is no warranty for use of this software. It is solely for my own use and experimentation with techniques, algorithms and file systems. Do not use this in a production system. Only use this system if you can provide a secure network. Pull prequests welcomed, if the GPL license is agreeable.

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
