/* A set of test objects used to test membrane-caja.js.
 * @author: Adrienne Felt (adriennefelt@gmail.com)
 */

// Used for the basic membrane tests -- the corresponding
// policies in taming/MyAPI.js are "allow all" for everything

function Person(name,age,location) {
    this.name = name;
    this.age = age;
    this.location = location;
    this.pet = new Pet("Gorilla");
    this.pet.petpet = new Pet("Kitten");
}

function Pet(type) {
    this.species = type;
}

Person.prototype.dance = function () {
    document.write(this.name + " dances");
}

Person.prototype.giveNumber = function(a,b,c) {
	var ret = (a+b+48)/c;
    return ret;
}

var Alice = new Person("Alice",16,"Wonderland");
var Bob = new Person("Bob",15,"California");
var Carol = new Person("Carol",45,"New Jersey");

Alice.clear___ = function() {
    this.name="Alice";
    this.age=16;
    this.location="Wonderland";
}

// Used for the policy tests

function Test (age) {
	this.name = "TESTER";
	this.age = age;
	this.height = "5'1\"";
}

Test.prototype.wave = function () {
	document.write(this.name + " waves<br />");
}

Test.prototype.highFive = function (five1, five2) {
	return five1+five2;
}

// The API sets privateprop to perm:deny, so this will
// only return true if the function has access to the 
// original (and not the wrapped) w
Test.prototype.useArgs = function (w) {
	if (w.privateprop()) { return true; }
	return false;
}

Test.prototype.privateprop = function () {
	return true;
}