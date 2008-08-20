var tame_Test___ =

{
	"properties": {
		"name": {
			"perm": "deny",
			"comment": "the tester's name is top secret"
		},
		"age": {
			"perm": "allow",
			"rw": "read-only",
			"comment": "because i said so"
		},
		"height": {
			"perm": "allow",
			"rw": "write",
			"RHS_arr": ["6'1\"","5'6\""] 
		}
	},
	
	"functions": { 
		"wave": {
			"perm": "deny",
			"comment": "You may not wave."
		},
		"highFive": {
			"perm": "allow",
			"arg1_arr": ["5","50"],
			"arg2_filter": "fivefilter"
		},
		"useArgs": {
			"perm": "allow"
		},
		"privateprop": {
			"perm": "deny"
		}
	}
}

var fivefilter = function (arg) {
	if (arg === 5) return true;
	return false;
}

var tame_Pet___ =

{
	"properties": {
		"species": {
			"perm": "allow",
			"rw": "write"
		},
		"petpet": {
			"perm": "allow",
			"rw": "write"
		}
  },
	
  "functions": { }
}

var tame_Person___ =

{
  "properties": {
    "name": {
			"perm": "allow",
			"rw": "write"
		},
    "age": {
			"perm": "allow",
			"rw": "write",
			"RHS_arr": ["12","13","14","15","16","17"]
		},
		"location": {
			"perm": "allow",
			"rw": "write",
			"RHS_arr": ["Sunnyvale","Michigan","Saratoga"]
		},
		"pet": { 
			"perm": "allow",
			"rw": "write"
		},
  },
	
  "functions": { 
    "dance": {
			"perm": "allow"
		},
    "giveNumber": {
			"perm": "allow",
			"arg1_arr": ["1","2","3"],
			"arg2_arr": ["1","2","3"],
			"arg3_arr": ["1","2","3"]
		}
  }
}