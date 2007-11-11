// Copyright (C) 2007 Google Inc.
//      
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// .............................................................................

// This module is written in the Caja subset of Javascript. It should
// work whether run translated or untranslated. Either way, it depends
// on caja.js and Brand.js, but not on anything else.


function Mint(name) {
    caja.requireType(name,'string');
    var brand = Brand(name);
    function Purse(balance) {
        caja.requireNat(balance);
        function decr(amount) {
            caja.requireNat(amount);
            balance = caja.requireNat(balance - amount);
        }
        return caja.freeze({
            toString:   function() { return '<'+balance+' '+name+' bucks>'; },
            getBalance: function() { return balance; },
            makePurse:  function() { return Purse(0); },
            getDecr:    function() { return brand.seal(decr); },
            deposit:    function(amount, src) {
		var newBal = caja.requireNat(balance + amount);
                brand.unseal(src.getDecr())(amount);
                balance = newBal;
            }
        });
    }
    return caja.freeze({
        toString: function() { return '<'+name+' bank>'; },
	makeAcct: Purse
    });
}

/*
Squarefree session to be turned into a unit test:

var usd = Mint('usd')
var aliceAcct = usd.makeAcct(100)
var bobAcct = usd.makeAcct(200)
var payment = aliceAcct.makePurse()
payment.deposit(10,aliceAcct)
aliceAcct
<90 usd bucks>

payment
<10 usd bucks>

bobAcct.deposit(10,payment)
[aliceAcct,payment,bobAcct]
<90 usd bucks>,<0 usd bucks>,<210 usd bucks>

var euro = Mint('euro')
var carolAcct = euro.makeAcct(300)
carolAcct.deposit(10,bobAcct)
Error on line 98: not my box: <usd box>

[aliceAcct,payment,bobAcct,carolAcct]
<90 usd bucks>,<0 usd bucks>,<210 usd bucks>,<300 euro bucks>

*/