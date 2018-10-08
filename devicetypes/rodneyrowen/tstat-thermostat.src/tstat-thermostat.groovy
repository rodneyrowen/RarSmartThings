/**
 *  Copyright 2017 SmartThings
 *
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
@Field List SUPPORTED_MODES = [MODE.OFF, MODE.HEAT, MODE.COOL]
//@Field List SUPPORTED_MODES = [MODE.OFF, MODE.HEAT, MODE.AUTO, MODE.COOL]
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

// defaults
@Field final String   DEFAULT_MODE = MODE.OFF
@Field final String   DEFAULT_FAN_MODE = FAN_MODE.AUTO
@Field final String   DEFAULT_OP_STATE = OP_STATE.IDLE
@Field final String   DEFAULT_PREVIOUS_STATE = OP_STATE.HEATING
@Field final String   DEFAULT_SETPOINT_TYPE = SETPOINT_TYPE.HEATING
@Field final Integer  DEFAULT_TEMPERATURE = 72
@Field final Integer  DEFAULT_HEATING_SETPOINT = 68
@Field final Integer  DEFAULT_COOLING_SETPOINT = 80
@Field final Integer  DEFAULT_THERMOSTAT_SETPOINT = 70


metadata {
    // Automatically generated. Make future change here.
    definition (name: "Tstat Thermostat", namespace: "rodneyrowen", author: "rrowen") {
    capability "Sensor"
    capability "Actuator"
    capability "Health Check"

    capability "Thermostat"
    capability "Configuration"
    capability "Refresh"

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
    command "setSchedule", ["string"]
        
    command "heatUp"
    command "heatDown"
    command "coolUp"
    command "coolDown"
    command "setpointUp"
    command "setpointDown"

    command "cycleMode"
    command "cycleFanMode"

    command "setTemperature", ["number"]

    attribute "schedule", "string"
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

        standardTile("mode", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
            state "off",            action: "cycleMode", nextState: "updating", icon: "st.thermostat.heating-cooling-off", backgroundColor: "#CCCCCC", defaultState: true
            state "heat",           action: "cycleMode", nextState: "updating", icon: "st.thermostat.heat"
            state "cool",           action: "cycleMode", nextState: "updating", icon: "st.thermostat.cool"
            state "auto",           action: "cycleMode", nextState: "updating", icon: "st.thermostat.auto"
            state "emergency heat", action: "cycleMode", nextState: "updating", icon: "st.thermostat.emergency-heat"
            state "updating", label: "Working"
        }

        standardTile("fanMode", "device.thermostatFanMode", width: 2, height: 2, decoration: "flat") {
            state "off",       action: "cycleFanMode", nextState: "updating", icon: "st.thermostat.fan-off", backgroundColor: "#CCCCCC", defaultState: true
            state "auto",      action: "cycleFanMode", nextState: "updating", icon: "st.thermostat.fan-auto"
            state "on",        action: "cycleFanMode", nextState: "updating", icon: "st.thermostat.fan-on"
            state "circulate", action: "cycleFanMode", nextState: "updating", icon: "st.thermostat.fan-circulate"
            state "updating", label: "Working"
        }

        valueTile("coolingSetpointSm", "device.coolingSetpoint", width: 2, height: 1, decoration: "flat") {
            state "cool", label: '${currentValue}°F', unit: "°F", backgroundColor: "#00A0DC"
        }
        valueTile("heatingSetpointSm", "device.heatingSetpoint", width: 2, height: 1, decoration: "flat") {
            state "heat", label:'${currentValue}', unit: "°F", backgroundColor:"#E86D13"
        }

		valueTile("heatingSetpoint", "device.heatingSetpoint", width: 2, height: 2, decoration: "flat") {
            state "heat", label:'Heat\n${currentValue} °F', unit: "°F", backgroundColor:"#E86D13"
        }
        standardTile("heatDown", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "heat", action: "heatDown", icon: "st.thermostat.thermostat-down"
        }
        standardTile("heatUp", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "heat", action: "heatUp", icon: "st.thermostat.thermostat-up"
        }

        valueTile("coolingSetpoint", "device.coolingSetpoint", width: 2, height: 2, decoration: "flat") {
            state "cool", label: 'Cool\n${currentValue} °F', unit: "°F", backgroundColor: "#00A0DC"
        }
        standardTile("coolDown", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "cool", action: "coolDown", icon: "st.thermostat.thermostat-down"
        }
        standardTile("coolUp", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "cool", action: "coolUp", icon: "st.thermostat.thermostat-up"
        }

        valueTile("roomTemp", "device.temperature", width: 2, height: 2, decoration: "flat") {
            state "default", label:'${currentValue} °F', unit: "°F", backgroundColors: [
                // Celsius Color Range
                [value:  0, color: "#153591"],
                [value:  7, color: "#1E9CBB"],
                [value: 15, color: "#90D2A7"],
                [value: 23, color: "#44B621"],
                [value: 29, color: "#F1D801"],
                [value: 33, color: "#D04E00"],
                [value: 36, color: "#BC2323"],
                // Fahrenheit Color Range
                [value: 40, color: "#153591"],
                [value: 44, color: "#1E9CBB"],
                [value: 59, color: "#90D2A7"],
                [value: 74, color: "#44B621"],
                [value: 84, color: "#F1D801"],
                [value: 92, color: "#D04E00"],
                [value: 96, color: "#BC2323"]
            ]
        }
        valueTile("title", "device.switch", width: 2, height: 1, decoration: "flat") {
            state "default", label: "Schedule:"
        }
        valueTile("schedule", "device.schedule", width: 4, height: 1, decoration: "flat") {
            state "default", label: '${currentValue}'
        }

        valueTile("refresh", "device.switch", width: 2, height: 2, decoration: "flat") {
            state "default", label: "Refresh", action: "refresh"
        }
        valueTile("reset", "device.switch", width: 2, height: 2, decoration: "flat") {
            state "default", label: "Reset to Defaults", action: "configure"
        }

        main("roomTemp")
        details(["thermostatMulti",
            "title", "schedule",
            "mode",
            "heatingSetpoint",
            "coolingSetpoint",
            "fanMode",
            "refresh", "reset"
        ])
//            "heatDown", "heatUp",
//            "mode",
//            "coolDown", "coolUp",
//            "heatingSetpoint",
//            "coolingSetpoint",
//            "fanMode",
//            "refresh", "reset"
    }
}

def installed() {
    log.trace "Executing 'installed'"
    initialize()
    done()
}

def updated() {
    log.trace "Executing 'updated'"
    initialize()
    done()
}

def configure() {
    log.trace "Executing 'configure'"
    initialize()
    done()
}

private initialize() {
    log.trace "Executing 'initialize'"

    // for HealthCheck
    sendEvent(name: "DeviceWatch-DeviceStatus", value: "online")
    sendEvent(name: "healthStatus", value: "online")
    sendEvent(name: "DeviceWatch-Enroll", value: [protocol: "cloud", scheme:"untracked"].encodeAsJson(), displayed: false)

    sendEvent(name: "temperature", value: DEFAULT_TEMPERATURE, unit: "°F")
    sendEvent(name: "heatingSetpoint", value: DEFAULT_HEATING_SETPOINT, unit: "°F")
    sendEvent(name: "heatingSetpointMin", value: HEATING_SETPOINT_RANGE.getFrom(), unit: "°F")
    sendEvent(name: "heatingSetpointMax", value: HEATING_SETPOINT_RANGE.getTo(), unit: "°F")
    sendEvent(name: "thermostatSetpoint", value: DEFAULT_THERMOSTAT_SETPOINT, unit: "°F")
    sendEvent(name: "coolingSetpoint", value: DEFAULT_COOLING_SETPOINT, unit: "°F")
    sendEvent(name: "coolingSetpointMin", value: COOLING_SETPOINT_RANGE.getFrom(), unit: "°F")
    sendEvent(name: "coolingSetpointMax", value: COOLING_SETPOINT_RANGE.getTo(), unit: "°F")
    sendEvent(name: "thermostatMode", value: DEFAULT_MODE)
    sendEvent(name: "thermostatFanMode", value: DEFAULT_FAN_MODE)
    sendEvent(name: "thermostatOperatingState", value: DEFAULT_OP_STATE)
    sendEvent(name: "schedule", value: "Default")

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
    done()
    return parsedEvents
}

def refresh() {
    log.trace "Executing refresh"
    sendEvent(name: "thermostatMode", value: getThermostatMode())
    sendEvent(name: "thermostatFanMode", value: getFanMode())
    sendEvent(name: "thermostatOperatingState", value: getOperatingState())
    sendEvent(name: "thermostatSetpoint", value: getThermostatSetpoint(), unit: "°F")
    sendEvent(name: "coolingSetpoint", value: getCoolingSetpoint(), unit: "°F")
    sendEvent(name: "heatingSetpoint", value: getHeatingSetpoint(), unit: "°F")
    sendEvent(name: "temperature", value: getTemperature(), unit: "°F")
    done()
}

// Thermostat mode
private String getThermostatMode() {
    return device.currentValue("thermostatMode") ?: DEFAULT_MODE
}

def setThermostatMode(String value) {
    log.trace "Executing 'setThermostatMode' $value"
    if (value in SUPPORTED_MODES) {
        sendEvent(name: "thermostatMode", value: value)
    } else {
        log.warn "'$value' is not a supported mode. Please set one of ${SUPPORTED_MODES.join(', ')}"
    }
    done()
}

private String cycleMode() {
    log.trace "Executing 'cycleMode'"
    String nextMode = nextListElement(SUPPORTED_MODES, getThermostatMode())
    setThermostatMode(nextMode)
    done()
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
        sendEvent(name: "thermostatFanMode", value: value)
    } else {
        log.warn "'$value' is not a supported fan mode. Please set one of ${SUPPORTED_FAN_MODES.join(', ')}"
    }
}

private String cycleFanMode() {
    log.trace "Executing 'cycleFanMode'"
    String nextMode = nextListElement(SUPPORTED_FAN_MODES, getFanMode())
    setThermostatFanMode(nextMode)
    done()
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
    if (operatingState in OP_STATE.values()) {
	    log.trace "Set OP State: $operatingState"
        sendEvent(name: "thermostatOperatingState", value: operatingState)
    } else {
        log.warn "'$operatingState' is not a supported operating state. Please set one of ${OP_STATE.values().join(', ')}"
    }
}

def setSchedule(String newSchedule) {
    log.trace "Set Schedule: $newSchedule"
    sendEvent(name: "schedule", value: newSchedule)
}

// setpoint
def Integer getThermostatSetpoint() {
    def ts = device.currentState("thermostatSetpoint")
    return ts ? ts.getIntegerValue() : DEFAULT_THERMOSTAT_SETPOINT
}

def setThermostatSetpoint(Double degreesF) {
    def newSp = boundInt(degreesF as Integer, FULL_SETPOINT_RANGE)
    log.trace "Executing 'setThermostatSetpoint' $newSp"
    sendEvent(name: "thermostatSetpoint", value: newSp)
    done()
}

private Integer getHeatingSetpoint() {
    def hs = device.currentState("heatingSetpoint")
    return hs ? hs.getIntegerValue() : DEFAULT_HEATING_SETPOINT
}

def setHeatingSetpoint(Double degreesF) {
    def newSp = boundInt(degreesF as Integer, HEATING_SETPOINT_RANGE)
    log.trace "Executing 'setHeatingSetpoint' $newSp"
    sendEvent(name: "heatingSetpoint", value: newSp, unit: "F")
    done()
}

private heatUp() {
    log.trace "Executing 'heatUp'"
    def newHsp = getHeatingSetpoint() + 1
    setHeatingSetpoint(newHsp)
    done()
}

private heatDown() {
    log.trace "Executing 'heatDown'"
    def newHsp = getHeatingSetpoint() - 1
    setHeatingSetpoint(newHsp)
    done()
}

private Integer getCoolingSetpoint() {
    def cs = device.currentState("coolingSetpoint")
    return cs ? cs.getIntegerValue() : DEFAULT_COOLING_SETPOINT
}

def setCoolingSetpoint(Double degreesF) {
    def newSp = boundInt(degreesF as Integer, COOLING_SETPOINT_RANGE)
    log.trace "Executing 'setCoolingSetpoint' $newSp"
    sendEvent(name: "coolingSetpoint", value: newSp, unit: "F")
    done()
}

private coolUp() {
    log.trace "Executing 'coolUp'"
    def newCsp = getCoolingSetpoint() + 1
    setCoolingSetpoint(newCsp)
    done()
}

private coolDown() {
    log.trace "Executing 'coolDown'"
    def newCsp = getCoolingSetpoint() - 1
    setCoolingSetpoint(newCsp)
    done()
}

private setpointUp() {
    log.trace "Executing 'setpointUp'"
    def newSp = getThermostatSetpoint() + 1
    setThermostatSetpoint(newSp)
    done()
}

private setpointDown() {
    log.trace "Executing 'setpointDown'"
    def newSp = getThermostatSetpoint() - 1
    setThermostatSetpoint(newSp)
    done()
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
def setTemperature(newTemp) {
    sendEvent(name:"temperature", value: newTemp)
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

/**
 * Just mark the end of the execution in the log
 */
private void done() {
    log.trace "---- DONE ----"
}