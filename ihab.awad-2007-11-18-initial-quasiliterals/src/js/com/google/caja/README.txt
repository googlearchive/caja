Caja modules that can run correctly whether translated or untranslated
should be named "<ident>-caja.js". But if they use only the Cajita
subset of Caja, they should be named "<ident>-cajita.js" instead. In
both cases, <ident> should be the main name the module exports.

caja.js             The Caja runtime library
                    caja.js also exports the global "___".

JSON.js             A Caja-friendly JSON library
                    Will probably be replaced with a JSON-cajita.js

Brand-cajita.js     Makes sealer/unsealer pairs for rights amplification

Mint-cajita.js      A simple money example

Q-caja.js           Asynchronous promise-based distributed capability
                    messaging with JSON over https using
                    web-keys. This will likely be extended for
                    asynchronous local inter-vat messaging as well.
