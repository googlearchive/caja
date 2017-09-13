# Caja

Caja is a tool for making third party HTML, CSS and JavaScript safe to embed in your website. It enables rich interaction between the embedding page and the embedded applications. Caja uses an object-capability security model to allow for a wide range of flexible security policies, so that your website can effectively control what embedded third party code can do with user data.

Caja supports most HTML and CSS and the recently standardized "strict mode" JavaScript version of JavaScript -- even on older browsers that do not support strict mode. It allows third party code to use new JavaScript features on older browsers that do not support them.

## Benefits of using Caja

* *New JavaScript Features.* Caja emulates all the new features of ECMAScript 5, including getters and setters, non-enumerable properties, and read-only properties. New browsers support these features natively, but older browsers still have a significant user base. Caja emulates these new features on browsers that don't support them natively.
* *Mashups.* Caja-compiled code is safe to inline directly in a page, so it's easy to put many third-party apps into the same page and allow them to exchange JavaScript objects. They can even inherit CSS styles from the host page. At the same time, the host page is protected from the embedded apps: they can't redirect pages to phishing sites, sniff internal networks or browser history, or steal cookies.
* *Safely extends JSON with code.* Until Caja, website authors that wished to consume data provided by a RESTful service had a dilemma: make their site vulnerable to the author of the JavaScript library that interacts with the service or write their own. With Caja, a service can supply both JSON and JavaScript, and websites can compile the JavaScript using Caja to make it safe to embed in their page.

* For more information on using Caja and the Caja API, see the documentation on [Google Developers](https://developers.google.com/caja/).
