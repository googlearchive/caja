# Caja

Caja is a tool for making third party HTML, CSS and JavaScript safe to embed in your website. It enables rich interaction between the embedding page and the embedded applications. Caja uses an object-capability security model to allow for a wide range of flexible security policies, so that your website can effectively control what embedded third party code can do with user data.

Caja supports most HTML and CSS and the recently standardized "strict mode" JavaScript version of JavaScript -- even on older browsers that do not support strict mode. It allows third party code to use new JavaScript features on older browsers that do not support them.

## Deprecation
**On January 31st, 2021, we will be archiving the Caja project. After January 31, no new features will be added, pull requests and other issues will no longer be addressed, including patches for security issues, and the repository will be marked as archived. Caja has not been actively maintained or developed to keep up with the latest research on web security. As a result, several security vulnerabilities have been reported to Caja, both by Google’s security engineers and by external researchers.**

We encourage users of Caja's HTML and CSS sanitizers to migrate to [Closure toolkit](https://developers.google.com/closure/), an open source toolkit for Javascript. Closure is used by applications, including Search, Gmail, Docs and Maps. 

The [Closure library](https://github.com/google/closure-library) has built-in HTML and CSS sanitizers and provides native support for key security mitigations like Content Security Policy and Trusted Types. Additionally, [Closure templates](https://github.com/google/closure-templates) provide a strictly contextual auto-escaping system, which can drastically reduce the risk of XSS in your application.

## Benefits of using Caja

* *New JavaScript Features.* Caja emulates all the new features of ECMAScript 5, including getters and setters, non-enumerable properties, and read-only properties. New browsers support these features natively, but older browsers still have a significant user base. Caja emulates these new features on browsers that don't support them natively.
* *Mashups.* Caja-compiled code is safe to inline directly in a page, so it's easy to put many third-party apps into the same page and allow them to exchange JavaScript objects. They can even inherit CSS styles from the host page. At the same time, the host page is protected from the embedded apps: they can't redirect pages to phishing sites, sniff internal networks or browser history, or steal cookies.
* *Safely extends JSON with code.* Until Caja, website authors that wished to consume data provided by a RESTful service had a dilemma: make their site vulnerable to the author of the JavaScript library that interacts with the service or write their own. With Caja, a service can supply both JSON and JavaScript, and websites can compile the JavaScript using Caja to make it safe to embed in their page.

* For more information on using Caja and the Caja API, see the documentation on [Google Developers](https://developers.google.com/caja/).
