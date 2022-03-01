(function() {
	var download = function() {
		var states = ["NJ"]
		for ( var i = 0; i < states.length; i++ ) {
			var state = states[i]
			var a = document.createElement( "a" )
			a.setAttribute( "href", "covid-19-vaccine.vaccine-status." + state + ".json?vaccineinfo" )
			a.setAttribute( "download", "CVS_" + state + ".json" )
			a.click()
		}
		printTime()
	}
	
	var printTime = function() {
		var today = new Date()
		var hour = today.getHours()
		var minute = today.getMinutes()
		var second = today.getSeconds()
		var time = hour + ""
		if ( minute >= 10 )
			time += ":" + minute
		else
			time += ":0" + minute
		if ( second >= 10 )
			time += ":" + second
		else
			time += ":0" + second
		
		console.log( time )
	}
	
	setInterval( download, 59000 )
	download()
})()

javascript( !function(){function e(){document.getElementById("i1").value="14003",document.getElementById("submit").click(),setTimeout(function(){var e=document.getElementsByClassName("btn-submit");2==e.length&&e[1].click()},1e3)}setInterval(e,1e4),e()}(); )();