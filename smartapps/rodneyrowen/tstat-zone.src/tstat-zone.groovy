 /*
	Tstat Zone
    
 	Author: Rodney Rowen

        based on Mike Maxwell
    
	This software is free for Private Use. You may use and modify the software without distributing it.
 
	This software and derivatives may not be used for commercial purposes.
	You may not distribute or sublicense this software.
	You may not grant a sublicense to modify and distribute this software to third parties not included in the license.

	The software is provided without warranty and the software author/license owner cannot be held liable for damages.        
        
*/
 
definition(
    name: "Tstat Zone",
    namespace: "rodneyrowen",
    author: "rrowen",
    description: "zone application for 'Tstat Master', do not install directly.",
    category: "My Apps",
    parent: "rodneyrowen:Tstat Master",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
    )

preferences {
    page(name: "Zone Sensors", title: "Select Zone Senors", install: true, uninstall: true) {
        section("Schedule Name") {
            input "zoneName", "text", title: "Name of this Zone:", multiple: false, required: true
		}
        section("Temperature Sensors") {
            input "inTemp", "capability.temperatureMeasurement", title: "Indoor Thermometer", multiple: true, required: true
        }
    }
}

def installed() {
}

def updated() {
	unsubscribe()
	initialize()
}

def getHubID(){
    def hubID
    if (myHub){
        hubID = myHub.id
    } else {
        def hubs = location.hubs.findAll{ it.type == physicalgraph.device.HubType.PHYSICAL } 
        //log.debug "hub count: ${hubs.size()}"
        if (hubs.size() == 1) hubID = hubs[0].id 
    }
    //log.debug "hubID: ${hubID}"
    return hubID
}

def initialize() {
    state.vChild = "1.0.1"
    parent.updateVer(state.vChild)
    state.nextRunTime = 0
    state.zoneTriggerActive = false
    subscribe(inTemp, "temperature", temperatureHandler)

    //subscribe(motionSensors, "motion.inactive", inactiveHandler)
    //subscribe(motionSensors, "motion.active", activeHandler)
    app.updateLabel("Zone-${settings.zoneName}") 
    def deviceID = "${app.id}"
    def zName = "Tstat-${settings.zoneName}"
    def zoneTile = getChildDevice(deviceID)
    if (!zoneTile) {
    	log.info "create Tstat Tile ${zName}"
        zoneTile = addChildDevice("rodneyrowen", "Tstat Zone Tile", deviceID, getHubID(), [name: zName, label: zName, completedSetup: true])
    } else {
    	log.info "Tstat Tile ${zName} exists"
    }
    zoneTile.inactive()
    log.debug "Installed with settings: ${settings}"

	state.temperature = 0
    runEvery5Minutes(poll)
}

def poll() {
	// Periodic poller to read the temperature and adjust the vents
    temperatureHandler()
}

def temperatureHandler(evt) {
    def sum     = 0
    def count   = 0
    def average = 0
	def temp = 0
	if (settings.inTemp) {
        for (sensor in settings.inTemp) {
            temp = sensor.currentTemperature
            if (temp) {
                log.debug "temp Sensors Reading: ${temp}"
                count += 1
                sum += temp
            } else {
                log.debug "got Null temp"
            }
        }
        average = sum/count
    } else {
	    log.debug "No temp Sensors available set average to 60"
    	average = 60
    }
    
    state.temperature = average
    log.debug "average: ${average}"

    def zoneTile = getChildDevice("${app.id}")
	if (zoneTile) {
	    log.debug "Set Tile Temp to: ${average}"
		zoneTile.setTemperature(average)
   	}
}

def getTemperature() {
    temperatureHandler()
	return state.temperature
}


def activeHandler(evt){
    log.trace "active handler fired via [${evt.displayName}] UTC: ${evt.date.format("yyyy-MM-dd HH:mm:ss")}"
}

def inactiveHandler(evt){
}

def setThermostatMode(String value) {
    log.trace "Executing 'setThermostatMode' $value"
    state.mode = value
    def zoneTile = getChildDevice("${app.id}")
	if (zoneTile) {
		zoneTile.setThermostatMode(value)
   	}
}

def setOperatingState(String operatingState) {
    log.trace "Executing 'setOperatingState' $value"
    state.opState = operatingState
    def zoneTile = getChildDevice("${app.id}")
	if (zoneTile) {
		zoneTile.setOperatingState(operatingState)
   	}
}

def setThermostatSetpoint(Double degreesF) {
    log.trace "Executing 'setThermostatSetpoint' $degreesF"
    state.setPoint = degreesF
    def zoneTile = getChildDevice("${app.id}")
	if (zoneTile) {
		zoneTile.setThermostatSetpoint(degreesF)
   	}
}


