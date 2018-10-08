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
            section("Debug") {
                input("logFilter", "number",title: "(1=ERROR only,2=<1+WARNING>,3=<2+INFO>,4=<3+DEBUG>,5=<4+TRACE>)",  range: "1..5",
                    description: "optional" )  
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
            if (installed){
            	section("Schedules"){
                    app(name: "childSchedules", appName: "Tstat Schedule", namespace: "rodneyrowen", title: "Create New Schedule...", multiple: true)
                }
            } else {
            	section(){
                	paragraph("Tap done to finish the initial installation.\nRe-open the app from the smartApps flyout to create your zones.")
                }
            }
            section("Default Setpoints (if no schedule applies") {
                input "coolingTemp", "number", title: "Cooling Setpoint"
                input "heatingTemp", "number", title: "Heating Setpoint"
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
            	section("Tstat Zones") {
                    app(name: "childZones", appName: "Tstat Zone", namespace: "rodneyrowen", title: "Create New Zone...", multiple: true)
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
    subscribe(location, "mode", modeChangeHandler)
    log.debug "Subscribed to devices"

    runEvery5Minutes(poll)

    // save defaults to state
    log.debug "Save defaults"
    changeMode(MODE.OFF, OP_STATE.IDLE, SETPOINT_TYPE.OFF)
    changeSchedule("Default", settings.coolingTemp, settings.heatingTemp) 

}

def modeChangeHandler(evt) {
    log.trace "Location Mode Change ${location.mode}"
    evaluateState()
}

def modeHandler(evt) {
	def tstatThermostat = getChildDevice("${app.id}")
    def mode = tstatThermostat.currentValue('thermostatMode')
    log.trace "Got Mode ${mode}"
    evaluateMode(mode)
}

def setpointHandler(evt) {
	def tstatThermostat = getChildDevice("${app.id}")
    def setpoint = tstatThermostat.currentValue('thermostatSetpoint')
    updateSetpoint(setpoint)
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

def Integer getThermostatSetpoint() {
    def ts = state.heatingSetpoint
    if (state.setpointType == SETPOINT_TYPE.COOLING) {
    	ts = state.coolingSetpoint
    }
    return ts ? ts : DEFAULT_THERMOSTAT_SETPOINT
}

def poll() {
	// Periodic poller since event listening does not seem to be working
    evaluateState()
}

private evaluateMode(def newMode) {
	//if (newMode != state.mode) {
        log.debug "Set mode: $newMode"
        switch (newMode) {
            case MODE.AUTO:
                changeMode(MODE.AUTO, OP_STATE.HEATING, SETPOINT_TYPE.HEATING)
                break;
            case MODE.HEAT:
                changeMode(MODE.HEAT, OP_STATE.HEATING, SETPOINT_TYPE.HEATING)
                break;
            case MODE.COOL:
                changeMode(MODE.COOL, OP_STATE.COOLING, SETPOINT_TYPE.COOLING)
                break;
            case MODE.OFF: 
                changeMode(MODE.OFF, OP_STATE.IDLE, SETPOINT_TYPE.HEATING)
                break;
            default:
                changeMode(MODE.OFF, OP_STATE.IDLE, SETPOINT_TYPE.HEATING)
                log.warn "'$newMode' is not a supported state. Please set one of ${MODE.values().join(', ')}"
                break;
        }
        evaluateState()
    //}
}

private evaluateState() {
    evaluateChildren()
    doProcessing()
}

private evaluateChildren() {
	def scheduleName = "Default"
	def bestState = 0
    def coolingSet = settings.coolingTemp
    def heatingSet = settings.heatingTemp
    childApps.each {child ->
        def childName = child.label.split('-')
        def type = childName[0]
        def value = childName[1]
        if (type == "Schedule") {
            log.info "evalute Schedule type for: ${value}"
            def state = child.isActive()
            if (state > bestState) {
                bestState = state
                scheduleName = value
                coolingSet = child.getCoolingSetpoint()
                heatingSet = child.getHeatingSetpoint()
                log.info "Selected new Schedule ${scheduleName} Cool: ${coolingSet} Heat: ${heatingSet}"
            }
        } else if (type == "Zone") {
            log.info "evalute Zone for: ${value}"
            def temp = child.getTemperature()
            
            log.info "Zone returned ${temp}"
        } else {
            log.info "Unknown Child type: ${child.label}"
            log.info "Got type: ${type}, value = ${value}"
        }
    }

//    if (scheduleName != state.scheduleName)
//    {
        changeSchedule(scheduleName, coolingSet, heatingSet)
//    }
}


private changeMode(newMode, newOpState, newSetpointType) {
    log.trace "Change Mode: ${newMode} op ${newOpState} type ${newSetpointType}"
    // Save it to the state
    state.mode = newMode
    state.opState = newOpState
    state.setpointType = newSetpointType

    // push it to the theromstat device
	def tstatThermostat = getChildDevice("${app.id}")
    tstatThermostat.setOperatingState(newOpState)
}

private changeSchedule(scheduleName, coolingSet, heatingSet) {
    log.trace "Change Schedule: ${scheduleName} cool ${coolingSet} heat ${heatingSet}"
    // Save it to the state
    state.scheduleName = scheduleName
    state.coolingSetpoint = coolingSet
    state.heatingSetpoint = heatingSet
    updateSetpoint(getThermostatSetpoint())

    // push it to the theromstat device
	def tstatThermostat = getChildDevice("${app.id}")
    tstatThermostat.setSchedule(scheduleName)
    tstatThermostat.setThermostatSetpoint(state.setpoint)
    tstatThermostat.setCoolingSetpoint(coolingSet)
    tstatThermostat.setHeatingSetpoint(heatingSet)

}

private updateSetpoint(setpoint) {
    log.trace "Change Setpoint: ${setpoint}"
    // Save it to the state
    state.setpoint = setpoint

    // push it to the theromstat device
	def tstatThermostat = getChildDevice("${app.id}")
    tstatThermostat.setThermostatSetpoint(state.setpoint)
}

private String determineHouseMode(mode) {
	// For right now
    def houseMode = MODE.OFF
    switch (mode) {
        case MODE.COOL:
    		houseMode = MODE.COOL
            break;
        case MODE.AUTO:
        case MODE.HEAT:
    		houseMode = MODE.HEAT
            break;
        case MODE.OFF: 
    	default:
        	houseMode = MODE.OFF
            break;
        }
	return houseMode       
}

private doProcessing() {
	def tstatThermostat = getChildDevice("${app.id}")
    def temperature = getTemparture()
    tstatThermostat.setTemperature(temperature.round(1))

    // Read the inputs to the processing
    def houseMode = houseThermostat.currentValue('thermostatMode')
    def houseTemp = houseThermostat.currentValue('temperature')
    def houseSetpoint = houseThermostat.currentValue('thermostatSetpoint')
    def houseCooling = houseThermostat.currentValue('coolingSetpoint')
    def houseHeating = houseThermostat.currentValue('heatingSetpoint')
    log.trace "Evalutate: ${mode} temp ${temperature}/${state.setpoint} house ${houseTemp}/${houseSetpoint}(${houseHeating}-${houseCooling})"

    def neededHouseMode = determineHouseMode(state.mode)
    if (houseMode != neededHouseMode) {
	    log.trace "Seting House Mode to ${neededHouseMode}"
        houseThermostat.setThermostatMode(neededHouseMode)
    }

	def tempDelta = temperature - houseTemp
    def newHeating = state.heatingSetpoint + tempDelta
    def newCooling = state.coolingSetpoint + tempDelta
    log.trace "Seting House setpoints to ${newHeating} and ${newCooling}"
    houseThermostat.setHeatingSetpoint(newHeating.round(0))
    houseThermostat.setCoolingSetpoint(newCooling.round(0))
}

def getVersionInfo(){
	return "Versions:\n\tZone Motion Manager: ${state.vParent ?: "No data available yet."}\n\tZone Configuration: ${state.vChild ?: "No data available yet."}"
}

def updateVer(vChild){
    state.vChild = vChild
}
