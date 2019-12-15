/**
 *
 *  Copyright 2018 RAR
 *  Modified to create a Thermostat for a Tstat room zone
 *  Only supports a limited set of display and abilities
 *
 *  thermostatOperatingState
 *   This string is abstracted to provide status from the app as to what
 *   is happening.  This code has been modified to accept any value
 *   and display it on the tiles for statusing the system
 *
 *  Thermostat Modes:
 *   is not used used to control the fan - only the fan mode is used
 *   However, the thermostat mode is read from the normal theromstat
 *   and updated from that.  It is used as follow by the controlling app:
 *   in auto mode, the fan will only turn on when the thermostat is 'off'
 *
 *  Cooling set point:
 *   This is used to determine the default set point for cooling in auto mode
 *
 *  thermostatSetpoint:
 *   The user can change the default set point and this is stored here
 *
 *  temperature
 *   The app will set the Temperature to the current temperature.  The app
 *   can support computing an average from a list of sensors, and this provides
 *   feedback on the actual temperature the app is determining
 *
 *  Copyright 2017 SmartThings
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
import groovy.transform.Field

// enummaps
@Field final Map      ZONE_MODE = [
    INACTIVE:   "inactive",
    ACTIVE:  "active",
    AUTO:  "auto"
]

@Field final Map      ZONE_OCCUPANCY = [
    INACTIVE: "unoccupied",
    ACTIVE:   "occupied",
    DISABLED: "disabled"
]

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
@Field List SUPPORTED_FAN_MODES = [FAN_MODE.OFF, FAN_MODE.AUTO, FAN_MODE.CIRCULATE, FAN_MODE.ON]

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


metadata {
    // Automatically generated. Make future change here.
	definition (name: "Tstat Zone Tile", namespace: "rodneyrowen", author: "rrowen", parent: "rodneyrowen:Tstat Zone") {
        capability "Sensor"
        capability "Actuator"
        capability "Health Check"
//		capability "Motion Sensor"
        capability "Switch"

        capability "Thermostat"
        capability "Configuration"
        capability "Refresh"

		attribute "zone", "string"
		attribute "occupancy", "string"
		attribute "lastUpdate", "string"
		attribute "zoneBase", "number"
		attribute "zoneDelta", "number"
		attribute "zoneTemporary", "number"
        
        command "zoneAuto"
        command "zoneActive"
        command "zoneInactive"

		command "setThermostatMode", ["string"]
		command "off"
		command "heat"
		command "auto"
		command "emergencyHeat"
		command "cool"

		command "setThermostatFanMode", ["string"]
		command "fanOff"
		command "fanOn"
		command "fanAuto"
		command "fanCirculate"

        command "setHeatingSetpoint", ["number"]
        command "setCoolingSetpoint", ["number"]
        command "setThermostatSetpoint", ["number"]
        command "setOperatingState", ["string"]
        
        command "heatUp"
        command "heatDown"
        command "coolUp"
        command "coolDown"
        command "setpointUp"
        command "setpointDown"

        command "cycleMode"
        command "cycleFanMode"

        command "setTemperature", ["number"]
        command "setZoneBase", ["number"]
        command "setZoneDelta", ["number"]
        command "setZoneTemporary", ["number"]
        command "setZoneOccupancy", ["string"]
    }
    
    tiles(scale: 2) {
        multiAttributeTile(name:"thermostatMulti", type:"thermostat", width:6, height:4) {
			tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
				attributeState("default", label:'${currentValue}°F', unit:"dF")
			}
			tileAttribute("device.thermostatSetpoint", key: "VALUE_CONTROL") {
				attributeState("VALUE_UP", action: "setpointUp")
				attributeState("VALUE_DOWN", action: "setpointDown")
			}
			tileAttribute("device.thermostatOperatingState", key: "OPERATING_STATE") {
				attributeState("idle", backgroundColor:"#00A0DC")
				attributeState("heating", backgroundColor:"#e86d13")
				attributeState("cooling", backgroundColor:"#00A0DC")
			}
			tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
				attributeState("off", label:'${name}')
				attributeState("heat", label:'${name}')
				attributeState("cool", label:'${name}')
				attributeState("auto", label:'${name}')
			}
			tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
                attributeState("coolingSetpoint", label:'${currentValue}', unit:"dF", defaultState: true)
				attributeState("heatingSetpoint", label:'${currentValue}', unit:"dF")
			}
			tileAttribute("device.coolingSetpoint", key: "COOLING_SETPOINT") {
				attributeState("coolingSetpoint", label:'${currentValue}', unit:"dF", defaultState: true)
			}
		}

        valueTile("thermostatMode", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
            state "off", label:'Thermostat Off', backgroundColor:"#00a0dc"
            state "cool", label:'Thermostat Cool', backgroundColor:"#cccccc"
            state "heat", label:'Thermostat Heat', backgroundColor:"#cccccc"
            state "auto", label:'Thermostat Auto', backgroundColor:"#cccccc"
            state "default", label: '${currentValue}', backgroundColor:"#cccccc"
        }

		standardTile("ventMode", "device.thermostatFanMode", width: 2, height: 2, decoration: "flat") {
            state "off",       action: "fanAuto", nextState: "auto", icon: "st.vents.vent-closed", backgroundColor: "#FFFFFF", defaultState: true
            state "auto",      action: "fanOn", label: "AUTO", nextState: "on", icon: "st.vents.vent-open-text", backgroundColor: "#00a0dc"
            state "circulate", action: "fanOn", label: "AUTO", nextState: "on", icon: "st.vents.vent-closed", backgroundColor: "#00a0dc"
            state "on",        action: "fanOff", nextState: "off", icon: "st.vents.vent-open-text", backgroundColor: "#00a0dc"
            state "updating", label: "Working", backgroundColor: "#00a0dc"
        }

        valueTile("zone", "device.zone", width: 2, height: 2, decoration: "flat") {
            state "active", label:'Active', icon: "st.motion.motion.active", action: "zoneInactive", backgroundColor: "#53a7c0"
            state "inactive", label:'Inactive', icon: "st.motion.motion.inactive", action: "zoneActive", backgroundColor: "#ffffff"
            state "auto", label:'Auto', icon: "st.motion.motion.active", action: "zoneInactive", backgroundColor: "#53a7c0"
		}

        valueTile("vent", "device.switch", width: 2, height: 2, canChangeIcon: false) {
            state "on", icon: "st.vents.vent-open-text", backgroundColor: "#53a7c0"
            state "off", icon: "st.vents.vent-closed", backgroundColor: "#ffffff"
            state "obstructed", icon: "st.vents.vent-closed", backgroundColor: "#ff0000"
            state "clearing", icon: "st.vents.vent-closed", backgroundColor: "#ffff33"
        }

        valueTile("occupancy", "device.occupancy", width: 2, height: 2, decoration: "flat") {
            state "occupied", label:'occupied', icon: "st.motion.motion.active", backgroundColor: "#53a7c0"
            state "unoccupied", label:'empty', icon: "st.motion.motion.inactive", backgroundColor: "#ffffff"
            state "disabled", label:'disabled', icon: "st.motion.motion.inactive", backgroundColor: "#ffffff"
		}

		valueTile("zonebase", "device.zoneBase", width: 2, height: 2, decoration: "flat") {
            state "default", label:'Base\n${currentValue}°F', unit: "°F"
        }

		valueTile("zonedelta", "device.zoneDelta", width: 2, height: 2, decoration: "flat") {
            state "default", label:'Zone\n${currentValue}°F', unit: "°F"
        }

		valueTile("zonetemporary", "device.zoneTemporary", width: 2, height: 2, decoration: "flat") {
            state "default", label:'Offset\n${currentValue}°F', unit: "°F"
        }

        valueTile("refresh", "device.switch", width: 2, height: 1, decoration: "flat") {
            state "default", label: "Refresh", action: "refresh"
        }
        
        valueTile("reset", "device.switch", width: 2, height: 1, decoration: "flat") {
            state "default", label: "Reset to Defaults", action: "configure"
        }

		valueTile("lastUpdate", "device.lastUpdate", width: 2, height: 1, decoration: "flat") {
			state "default", label:'Last update:\n${currentValue}'
		}

        valueTile("statusLabel", "device.switch", width: 2, height: 1, decoration: "flat") {
            state "default", label: "Input Status"
        }
        valueTile("blank21", "device.switch", width: 2, height: 1, decoration: "flat") {
            state "default", label: ""
        }

        main("thermostatMulti")
        details(["thermostatMulti",
            "zone", "ventMode", "occupancy",
            "reset", "refresh","lastUpdate",
            "zonebase", "zonedelta", "zonetemporary"
        ])
    }
}

def installed() {
    log.debug "Executing 'installed'"
    initialize()
}

def updated() {
    log.debug "Executing 'updated'"
    initialize()
}

def configure() {
    log.debug "Executing 'configure'"
    initialize()
}

private initialize() {
    log.debug "Executing 'initialize'"

    // for HealthCheck
    sendEvent(name: "DeviceWatch-DeviceStatus", value: "online")
    sendEvent(name: "healthStatus", value: "online")
    sendEvent(name: "DeviceWatch-Enroll", value: [protocol: "cloud", scheme:"untracked"].encodeAsJson(), displayed: false)

    sendEvent(name: "zone", value: ZONE_MODE.ACTIVE)
    sendEvent(name: "temperature", value: DEFAULT_TEMPERATURE, unit: "°F")
    sendEvent(name: "heatingSetpoint", value: DEFAULT_HEATING_SETPOINT, unit: "°F")
    sendEvent(name: "coolingSetpoint", value: 4, unit: "°F")
    sendEvent(name: "thermostatMode", value: DEFAULT_MODE)
    sendEvent(name: "thermostatFanMode", value: DEFAULT_FAN_MODE)
    sendEvent(name: "thermostatOperatingState", value: DEFAULT_OP_STATE)
    sendEvent(name: "zoneBase", value: DEFAULT_THERMOSTAT_SETPOINT)
    sendEvent(name: "zoneDelta", value: 0)
    sendEvent(name: "zoneTemporary", value: 0)
    sendEvent(name: "fanState", value: DEFAULT_FAN_STATE)
    setZoneBase(65);
    setZoneDelta(1);
    updateThermostatSetpoint()

    state.lastUserSetpointMode = DEFAULT_PREVIOUS_STATE
    unschedule()
}

// parse events into attributes
def parse(String description) {
    log.trace "Executing parse $description"
    def parsedEvents
    def pair = description?.split(":")
    if (!pair || pair.length < 2) {
        log.warn "parse() could not extract an event name and value from '$description'"
    } else {
        String name = pair[0]?.trim()
        if (name) {
            name = name.replaceAll(~/\W/, "_").replaceAll(~/_{2,}?/, "_")
        }
        parsedEvents = createEvent(name: name, value: pair[1]?.trim())
    }
    return parsedEvents
}

def refresh() {
    log.trace "Executing refresh"
    sendEvent(name: "thermostatMode", value: getThermostatMode())
    sendEvent(name: "thermostatFanMode", value: getFanMode())
    sendEvent(name: "thermostatOperatingState", value: getOperatingState())
    sendEvent(name: "coolingSetpoint", value: getCoolingSetpoint(), unit: "°F")
    sendEvent(name: "heatingSetpoint", value: getHeatingSetpoint(), unit: "°F")
    sendEvent(name: "temperature", value: getTemperature(), unit: "°F")
    sendEvent(name: "zoneBase", value: getZoneBase())
    sendEvent(name: "zoneDelta", value: getZoneDelta())
    sendEvent(name: "zoneTemporary", value: getZoneTemporary())
    setZoneBase(getZoneBase());
    setZoneDelta(getZoneDelta());
    setZoneOccupancy(getZoneOccupancy());
    updateThermostatSetpoint()
}

def setLastUpdate() {
	def timeStamp = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
    log.trace "Executing 'setLastUpdate' ${timeStamp}"
	sendEvent(name: "lastUpdate", value: timeStamp)
}

def zoneActive() {
    log.trace "Executing 'active'"
    parent.setZoneActive(ZONE_MODE.ACTIVE)
    sendEvent(name: "zone", value: ZONE_MODE.ACTIVE)
}

def zoneInactive() {
    log.trace "Executing 'inactive'"
    parent.setZoneActive(ZONE_MODE.INACTIVE)
    sendEvent(name: "zone", value: ZONE_MODE.INACTIVE)
}

def zoneAuto() {
    log.trace "Executing 'zoneAuto'"
    parent.setZoneActive(ZONE_MODE.AUTO)
    sendEvent(name: "zone", value: ZONE_MODE.AUTO)
}

// Thermostat mode
private String getThermostatMode() {
    return device.currentValue("thermostatMode") ?: DEFAULT_MODE
}

def setThermostatMode(String value) {
    log.trace "Executing 'setThermostatMode' ${value}"
    if (value in SUPPORTED_MODES) {
        sendEvent(name: "thermostatMode", value: value)
    } else {
        log.warn "'$value' is not a supported mode. Please set one of ${SUPPORTED_MODES.join(', ')}"
    }
}

private String cycleMode() {
    log.trace "Executing 'cycleMode'"
    String nextMode = nextListElement(SUPPORTED_MODES, getThermostatMode())
    setThermostatMode(nextMode)
    return nextMode
}

def off() {
	setThermostatMode(MODE.OFF)
}

def heat() {
	setThermostatMode(MODE.HEAT)
}

def auto() {
	setThermostatMode(MODE.AUTO)
}

def emergencyHeat() {
	setThermostatMode(MODE.EHEAT)
}

def cool() {
	setThermostatMode(MODE.COOL)
}

private Boolean isThermostatOff() {
    return getThermostatMode() == MODE.OFF
}

// Fan mode
private String getFanMode() {
    return device.currentValue("thermostatFanMode") ?: DEFAULT_FAN_MODE
}

def setThermostatFanMode(String value) {
    if (value in SUPPORTED_FAN_MODES) {
	    log.trace "Set Fan Mode: $value"
	    parent.setZoneVentMode(value)
        sendEvent(name: "thermostatFanMode", value: value)
    } else {
        log.warn "'$value' is not a supported fan mode. Please set one of ${SUPPORTED_FAN_MODES.join(', ')}"
    }
}

private String cycleFanMode() {
    log.trace "Executing 'cycleFanMode'"
    String nextMode = nextListElement(SUPPORTED_FAN_MODES, getFanMode())
    setThermostatFanMode(nextMode)
    return nextMode
}

def fanOn() {
	setThermostatFanMode(FAN_MODE.ON)
}

def fanOff() {
	setThermostatFanMode(FAN_MODE.OFF)
}

def fanAuto() {
	setThermostatFanMode(FAN_MODE.AUTO)
}

def fanCirculate() {
	setThermostatFanMode(FAN_MODE.CIRCULATE)
}


private String nextListElement(List uniqueList, currentElt) {
    if (uniqueList != uniqueList.unique().asList()) {
        throw InvalidPararmeterException("Each element of the List argument must be unique.")
    } else if (!(currentElt in uniqueList)) {
        throw InvalidParameterException("currentElt '$currentElt' must be a member element in List uniqueList, but was not found.")
    }
    Integer listIdxMax = uniqueList.size() -1
    Integer currentEltIdx = uniqueList.indexOf(currentElt)
    Integer nextEltIdx = currentEltIdx < listIdxMax ? ++currentEltIdx : 0
    String nextElt = uniqueList[nextEltIdx] as String
    return nextElt
}

// operating state
private String getOperatingState() {
    String operatingState = device.currentValue("thermostatOperatingState")?:OP_STATE.IDLE
    return operatingState
}

def setOperatingState(String operatingState) {
    log.debug "Executing 'setOperatingState' ${operatingState}"
    sendEvent(name: "thermostatOperatingState", value: operatingState)
	// update last time stamp
	def timeStamp = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
	sendEvent(name: "lastUpdate", value: timeStamp)
     
}

// setpoint
def Integer getThermostatSetpoint() {
    def ts = device.currentState("thermostatSetpoint")
    return ts ? ts.getIntegerValue() : DEFAULT_THERMOSTAT_SETPOINT
}

def updateThermostatSetpoint() {
    def zoneBase = getZoneBase()
    def zoneDelta = getZoneDelta()
    def zoneTemporary = getZoneTemporary()
    setThermostatSetpoint(zoneBase + zoneDelta + zoneTemporary)
}

def setThermostatSetpoint(Double degreesF) {
    def newSp = boundInt(degreesF as Integer, FULL_SETPOINT_RANGE)
    log.trace "Executing 'setThermostatSetpoint' $newSp"
    sendEvent(name: "thermostatSetpoint", value: newSp)
}

private Integer getHeatingSetpoint() {
    def hs = device.currentState("heatingSetpoint")
    return hs ? hs.getIntegerValue() : DEFAULT_HEATING_SETPOINT
}

def setHeatingSetpoint(Double degreesF) {
    def newSp = boundInt(degreesF as Integer, HEATING_SETPOINT_RANGE)
    log.trace "Executing 'setHeatingSetpoint' $newSp"
    sendEvent(name: "heatingSetpoint", value: newSp, unit: "F")
}

private heatUp() {
    log.trace "Executing 'heatUp'"
    def newHsp = getHeatingSetpoint() + 1
    setHeatingSetpoint(newHsp)
}

private heatDown() {
    log.trace "Executing 'heatDown'"
    def newHsp = getHeatingSetpoint() - 1
    setHeatingSetpoint(newHsp)
}

private Integer getCoolingSetpoint() {
    def cs = device.currentState("coolingSetpoint")
    return cs ? cs.getIntegerValue() : DEFAULT_COOLING_SETPOINT
}

def setCoolingSetpoint(Double degreesF) {
    def newSp = boundInt(degreesF as Integer, COOLING_SETPOINT_RANGE)
    log.trace "Executing 'setCoolingSetpoint' $newSp"
    sendEvent(name: "coolingSetpoint", value: newSp, unit: "F")
}

def setTimerSetpoint(Double degreesF) {
    def newSp = boundInt(degreesF as Integer, DELAY_SETPOINT_RANGE)
    log.trace "Executing 'setTimerSetpoint' $newSp"
    sendEvent(name: "coolingSetpoint", value: newSp, unit: "F")
}

private coolUp() {
    log.trace "Executing 'coolUp'"
    def newCsp = getCoolingSetpoint() + 1
    setCoolingSetpoint(newCsp)
}

private coolDown() {
    log.trace "Executing 'coolDown'"
    def newCsp = getCoolingSetpoint() - 1
    setCoolingSetpoint(newCsp)
}

private setpointUp() {
    log.trace "Executing 'setpointUp'"
    def newSp = getZoneTemporary() + 1
    setZoneTemporary(newSp)
}

private setpointDown() {
    log.trace "Executing 'setpointDown'"
    def newSp = getZoneTemporary() - 1
    setZoneTemporary(newSp)
}

// simulated temperature
private Integer getTemperature() {
    def ts = device.currentState("temperature")
    Integer currentTemp = DEFAULT_TEMPERATURE
    try {
        currentTemp = ts.integerValue
    } catch (all) {
        log.warn "Encountered an error getting Integer value of temperature state. Value is '$ts.stringValue'. Reverting to default of $DEFAULT_TEMPERATURE"
        setTemperature(DEFAULT_TEMPERATURE)
    }
    return currentTemp
}

// changes the "room" temperature for the simulation
def setTemperature(Double newTemp) {
    log.debug "Executing 'setTemperature' ${newTemp}"
    sendEvent(name:"temperature", value: newTemp)
}

// changes the "room" temperature for the simulation
def setZoneBase(Double newTemp) {
    log.debug "Executing 'setZoneBase' ${newTemp}"
    sendEvent(name:"zoneBase", value: newTemp)
    updateThermostatSetpoint()
}

def Double getZoneBase() {
    return device.currentValue("zoneBase") ?: 60
}

// changes the "room" temperature for the simulation
def setZoneDelta(Double newTemp) {
    log.debug "Executing 'setZoneDelta' ${newTemp}"
    sendEvent(name:"zoneDelta", value: newTemp)
    updateThermostatSetpoint()
}

def Double getZoneDelta() {
    return device.currentValue("zoneDelta") ?: 0
}

def setZoneOccupancy(String newState) {
    log.debug "Executing 'setZoneOccupancy' ${newState}"
    sendEvent(name:"occupancy", value: newState)
}

def String getZoneOccupancy() {
    return device.currentValue("occupancy") ?: ZONE_OCCUPANCY.DISABLED
}

// changes the "room" temperature for the simulation
def setZoneTemporary(Double newTemp) {
    log.debug "Executing 'setZoneTemporary' ${newTemp}"
    sendEvent(name:"zoneTemporary", value: newTemp)
    updateThermostatSetpoint()
}

def Double getZoneTemporary() {
    return device.currentValue("zoneTemporary") ?: 0
}

/**
 * Ensure an integer value is within the provided range, or set it to either extent if it is outside the range.
 * @param Number value         The integer to evaluate
 * @param IntRange theRange     The range within which the value must fall
 * @return Integer
 */
private Integer boundInt(Number value, IntRange theRange) {
    value = Math.max(theRange.getFrom(), Math.min(theRange.getTo(), value))
    return value.toInteger()
}
