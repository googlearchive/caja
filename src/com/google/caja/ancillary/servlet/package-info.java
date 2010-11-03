/**
A servlet that provides a web interface to the linter, minifier, and
jsdoc documentation generator.

<p>
{@link com.google.caja.ancillary.servlet.CajaWebToolsServlet} is the main
entry point for this package.  It stores information about a request
in a {@link com.google.caja.ancillary.servlet.Request} instance, then
invokes the {@link com.google.caja.ancillary.servlet.Processor} to
transform one or more {@link com.google.caja.ancillary.servlet.Job}s.
Error messages and warnings end up on a message queue as normal.
The servlet then packages those jobs together and, depending on the
{@link com.google.caja.ancillary.servlet.Verb} requested, serves the
output, serves the output embedded in HTML, with messages, or packages
documentation up into a
{@link com.google.caja.ancillary.servlet.ZipFileSystem ZIP file} for
easy download.
*/
  package com.google.caja.ancillary.servlet;
