/*
	Tstat Master
    
 	Author: Rodney Rowen

        Based on Mike Maxwell work
	    
	This software if free for Private Use. You may use and modify the software without distributing it.
 
	This software and derivatives may not be used for commercial purposes.
	You may not distribute or sublicense this software.
	You may not grant a sublicense to modify and distribute this software to third parties not included in the license.

	The software is provided without warranty and the software author/license owner cannot be held liable for damages.        
        
*/
import groovy.transform.Field

// enummaps
@Field final Map      MODE = [
    OFF:   "off",
    HEAT:  "heat",
    AUTO:  "auto",
    COOL:  "cool",
    EHEAT: "emergency heat"
]

@Field final Map      FAN_MODE = [
    OFF:       "off",
    AUTO:      "auto",
    CIRCULATE: "circulate",
    ON:        "on"
]

@Field final Map      OP_STATE = [
    COOLING:   "cooling",
    HEATING:   "heating",
    FAN:       "fan only",
    PEND_COOL: "pending cool",
    PEND_HEAT: "pending heat",
    VENT_ECO:  "vent economizer",
    IDLE:      "idle"
]

@Field final Map SETPOINT_TYPE = [
    COOLING: "cooling",
    HEATING: "heating"
]

@Field final List HEAT_ONLY_MODES = [MODE.HEAT, MODE.EHEAT]
@Field final List COOL_ONLY_MODES = [MODE.COOL]
@Field final List DUAL_SETPOINT_MODES = [MODE.AUTO]
@Field final List RUNNING_OP_STATES = [OP_STATE.HEATING, OP_STATE.COOLING]

// config - TODO: move these to a pref page
@Field List SUPPORTED_MODES = [MODE.OFF, MODE.HEAT, MODE.AUTO, MODE.COOL]
@Field List SUPPORTED_FAN_MODES = [FAN_MODE.OFF, FAN_MODE.ON, FAN_MODE.AUTO, FAN_MODE.CIRCULATE]

@Field final Float    THRESHOLD_DEGREES = 1.0
@Field final Integer  SIM_HVAC_CYCLE_SECONDS = 15
@Field final Integer  DELAY_EVAL_ON_MODE_CHANGE_SECONDS = 3

@Field final Integer  MIN_SETPOINT = 35
@Field final Integer  MAX_SETPOINT = 95
@Field final Integer  AUTO_MODE_SETPOINT_SPREAD = 4 // In auto mode, heat & cool setpoints must be this far apart
// end config

// derivatives
@Field final IntRange FULL_SETPOINT_RANGE = (MIN_SETPOINT..MAX_SETPOINT)
@Field final IntRange HEATING_SETPOINT_RANGE = (MIN_SETPOINT..(MAX_SETPOINT - AUTO_MODE_SETPOINT_SPREAD))
@Field final IntRange COOLING_SETPOINT_RANGE = ((MIN_SETPOINT + AUTO_MODE_SETPOINT_SPREAD)..MAX_SETPOINT)
@Field final IntRange DELAY_SETPOINT_RANGE = (1..12)

// defaults
@Field final String   DEFAULT_MODE = MODE.OFF
@Field final String   DEFAULT_FAN_MODE = FAN_MODE.AUTO
@Field final String   DEFAULT_OP_STATE = OP_STATE.IDLE
@Field final String   DEFAULT_PREVIOUS_STATE = OP_STATE.HEATING
@Field final String   DEFAULT_SETPOINT_TYPE = SETPOINT_TYPE.HEATING
@Field final Integer  DEFAULT_TEMPERATURE = 52
@Field final Integer  DEFAULT_OUT_TEMP = 49
@Field final Integer  DEFAULT_HEATING_SETPOINT = 48
@Field final Integer  DEFAULT_COOLING_SETPOINT = 13
@Field final Integer  DEFAULT_COOLING_DELAY_MAX = 12
@Field final Integer  DEFAULT_THERMOSTAT_SETPOINT = 70
@Field final String   DEFAULT_FAN_STATE = "off"

definition(
    name: "Tstat Master",
    singleInstance: true,
    namespace: "rodneyrowen",
    author: "rrowen",
    description: "Smart Thermostat with zones.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
    )


preferences {
	page(name: "main")	
	page(name: "schedules")	
    page(name: "zones")
}

def main(){
	def installed = app.installationState == "COMPLETE"
	return dynamicPage(
    	name		: "main"
        ,title		: "Tstat Settings"
        ,nextPage   : "schedules"
        ,uninstall  : true
        ){
            section("Temperature Sensors") {
                input "inTemp", "capability.temperatureMeasurement", title: "Indoor Thermometer", multiple: true
            }
            section("House Thermostat") {
                input "houseThermostat", "capability.thermostat", title: "House Thermostat"
            }
        }
}

def schedules(){
	return dynamicPage(
    	name		: "schedules"
        ,title		: "Tstat Schedules"
        ,nextPage   : "zones"
        ,uninstall  : true
        ){
            section("Deafult Schedule") {
                input "coolingTemp", "number", title: "Cooling Setpoint"
                input "heatingTemp", "number", title: "Heating Setpoint"
            }
            if (installed){
            	section(){
                    app(name: "childSchedules", appName: "Tstat Schedule", namespace: "rodneyrowen", title: "Create Schedules...", multiple: true)
                }
            } else {
            	section(){
                	paragraph("Tap done to finish the initial installation.\nRe-open the app from the smartApps flyout to create your zones.")
                }
            }
        }
}

def zones(){
	return dynamicPage(
    	name		: "zones"
        ,title		: "Tstat Zones"
        ,install    : true
        ,uninstall  : true
        ){
             if (installed){
            	section {
                    app(name: "childZones", appName: "Tstat Zone", namespace: "rodneyrowen", title: "Create Zone...", multiple: true)
            	}
				section (getVersionInfo()) { }
            } else {
            	section(){
                	paragraph("Tap done to finish the initial installation.\nRe-open the app from the smartApps flyout to create your zones.")
                }
            }
        }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
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
	state.vParent = "1.0.0"
    def deviceID = "${app.id}"
    def zName = "Tstat Thermostat"
    def tstatThermostat = getChildDevice(deviceID)
    if (!tstatThermostat) {
    	log.info "create Tstat Theromstat"
        tstatThermostat = addChildDevice("rar", "Tstat Thermostat", deviceID, getHubID(), [name: zName, label: zName, completedSetup: true])
    } else {
    	log.info "Tstat Theromstat exists"
    }
    //tstatThermostat.inactive()
    // Subscribe to things in the the thermostat
    subscribe(tstatThermostat, "thermostatMode", modeHandler)
    //subscribe(tstatThermostat, "thermostatFanMode", fanModeHandler)
    subscribe(tstatThermostat, "coolingSetpoint", coolingSetpointHandler)
    subscribe(tstatThermostat, "heatingSetpoint", heatingSetpointHandler)
    subscribe(tstatThermostat, "thermostatSetpoint", setpointHandler)

    runEvery5Minutes(poll)

    log.debug "Subscribed to devices:"
}

def modeHandler(evt) {
	def tstatThermostat = getChildDevice("${app.id}")
    def mode = tstatThermostat.currentValue('thermostatMode')
    log.trace "Got Mode ${mode}"
    evaluateState()
}

def setpointHandler(evt) {
	def tstatThermostat = getChildDevice("${app.id}")
    def setpoint = tstatThermostat.currentValue('thermostatSetpoint')
    log.trace "Setpoint Changed ${setpoint}"
    evaluateState()
}

def coolingSetpointHandler(evt) {
	def tstatThermostat = getChildDevice("${app.id}")
    def setpoint = tstatThermostat.currentValue('coolingSetpoint')
    log.trace "Cooling Setpoint Changed ${setpoint}"
    evaluateState()
}

def heatingSetpointHandler(evt) {
	def tstatThermostat = getChildDevice("${app.id}")
    def setpoint = tstatThermostat.currentValue('heatingSetpoint')
    log.trace "Heating Setpoint Changed ${setpoint}"
    evaluateState()
}

private double getTemparture() {
    def sum     = 0
    def count   = 0
    def average = 0

    for (sensor in settings.inTemp) {
        count += 1
        sum   += sensor.currentTemperature
    }

    average = sum/count
    return average
}

def poll() {
	// Periodic poller since event listening does not seem to be working
    evaluateState()
}

private evaluateState() {
    evaluateSchedules()
    doProcessing()
}

private evaluateSchedules() {
    def schedules = settings.childSchedules
    log.debug "$schedules.size() child scheules installed"
    schedules.each { schedule ->
        log.debug "Child app id: $schedule.id"
    }    
}

private doProcessing() {
	def tstatThermostat = getChildDevice("${app.id}")
    // Only to auto processing in auto mode so check this first
    def mode = tstatThermostat.currentValue('thermostatFanMode')
    def setpoint = tstatThermostat.currentValue('thermostatSetpoint')
    def temperature = getTemparture()
    tstatThermostat.setTemperature(temperature)

    // Read the inputs to the processing
    def houseMode = houseThermostat.currentValue('thermostatMode')
    def houseTemp = houseThermostat.currentValue('temperature')

    log.trace "Evalutate: ${mode} temp ${temperature} house ${houseTemp} setpoint ${setpoint} windows ${openWindow}"
}

def getVersionInfo(){
	return "Versions:\n\tZone Motion Manager: ${state.vParent ?: "No data available yet."}\n\tZone Configuration: ${state.vChild ?: "No data available yet."}"
}

def updateVer(vChild){
    state.vChild = vChild
}
