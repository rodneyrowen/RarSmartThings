//Release History
//		1.0 May 20, 2016
//			Initial Release


metadata {
        definition (name: "Virtual Presence Plus", namespace: "rodneyrowen", author: "rrowen") {
        capability "Switch"
        capability "Refresh"
        capability "Presence Sensor"
		capability "Sensor"
        
		command "arrived"
		command "departed"
    }

	// simulator metadata
	simulator {
	}

	// UI tile definitions
	tiles {
		standardTile("presence", "device.presence", width: 4, height: 4, canChangeBackground: true) {
			state("present", labelIcon:"st.presence.tile.mobile-present", backgroundColor:"#53a7c0")
			state("not present", labelIcon:"st.presence.tile.mobile-not-present", backgroundColor:"#ebeef2")
		}
		standardTile("setArrived", "generic", width: 2, height: 2) {
		state "default", label:'Arrive', 
			action:"arrived"
    	}
		standardTile("setDeparted", "generic", width: 2, height: 2) {
      		state "default", label:'Depart', 
			action:"departed"
    	}
			main "presence"
			details(["presence", "setArrived", "setDeparted"])
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
	}
}

def parse(String description) {
	def pair = description.split(":")
	createEvent(name: pair[0].trim(), value: pair[1].trim())
}

// handle commands
def arrived() {
	on()
}


def departed() {
    off()
}

def on() {
	sendEvent(name: "switch", value: "on")
    sendEvent(name: "presence", value: "present")

}

def off() {
	sendEvent(name: "switch", value: "off")
    sendEvent(name: "presence", value: "not present")

}