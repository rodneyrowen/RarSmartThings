/**
 *
 *  Copyright 2018 RAR
 *  Modified to create a Thermostat for a whole house fan app.
 *  Abstracted the following from normal thermostat definitions to the following:
 *
 *  Fan Mode Modes:
 *    OFF:   Turn fan off
 *    ON:    Turn the fan on
 *    AUTO:  Turn fan on\off automatically based on selected set points and 
 *    Circulate:  Turn the fan on for N minutes
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


metadata {
    // Automatically generated. Make future change here.
    definition (name: "FanThermostat", namespace: "rodneyrowen", author: "rrowen") {
        capability "Sensor"
        capability "Actuator"
        capability "Health Check"

        capability "Thermostat"
        capability "Configuration"
        capability "Refresh"

      	attribute "outTemp", "number"
      	attribute "fanState", "string"
		attribute "lastUpdate", "string"
        
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
        command "timerUp"
        command "timerDown"

        command "cycleMode"
        command "cycleFanMode"

        command "setTemperature", ["number"]
        command "setOutdoorTemp", ["number"]
        command "setFanState", ["string"]
    }
    
    tiles(scale: 2) {
        multiAttributeTile(name:"thermostatMulti", type:"generic", width:6, height:4) {
            tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
                attributeState("temperature",label:'${currentValue}°',
					backgroundColors:[
					[value: 32, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 92, color: "#d04e00"],
					[value: 98, color: "#bc2323"]
                    ])
            }
            tileAttribute("device.thermostatSetpoint", key: "VALUE_CONTROL") {
                attributeState("VALUE_UP", action: "setpointUp")
                attributeState("VALUE_DOWN", action: "setpointDown")
            }
            tileAttribute("device.thermostatOperatingState", key: "SECONDARY_CONTROL") {
                attributeState("default", label: '${currentValue}')
            }
        }

        valueTile("thermostatMode", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
            state "off", label:'Thermostat Off', backgroundColor:"#00a0dc"
            state "cool", label:'Thermostat Cool', backgroundColor:"#cccccc"
            state "heat", label:'Thermostat Heat', backgroundColor:"#cccccc"
            state "auto", label:'Thermostat Auto', backgroundColor:"#cccccc"
            state "default", label: '${currentValue}', backgroundColor:"#cccccc"
        }

		standardTile("fanOff", "device.thermostatFanMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "off", icon: "st.thermostat.fan-off", backgroundColor: "#00a0dc", action: "fanOff"
            state "default", icon: "st.thermostat.fan-off", backgroundColor: "#FFFFFF", action: "fanOff"
		}
		standardTile("fanAuto", "device.thermostatFanMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "auto", icon: "st.thermostat.fan-auto", backgroundColor: "#00a0dc", action: "fanAuto"
            state "default", icon: "st.thermostat.fan-auto", backgroundColor: "#FFFFFF", action: "fanAuto"
		}
		standardTile("fanDelay", "device.thermostatFanMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "circulate", label: "Timed", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", action: "fanCirculate"
            state "default", label: "Timed", icon: "st.thermostat.fan-on", backgroundColor: "#FFFFFF", action: "fanCirculate"
		}

		standardTile("fanMode", "device.thermostatFanMode", width: 2, height: 2, decoration: "flat") {
            state "off",       action: "fanOn", label: "OFF", nextState: "on", icon: "st.thermostat.fan-off", backgroundColor: "#FFFFFF", defaultState: true
            state "auto",      action: "fanOff", label: "Auto", nextState: "off", icon: "st.thermostat.fan-auto", backgroundColor: "#00a0dc"
            state "on",        action: "fanOff", label: "ON", nextState: "off", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc"
            state "circulate", action: "fanOff", label: "TIMED", nextState: "off", icon: "st.thermostat.fan-circulate", backgroundColor: "#00a0dc"
            state "updating", label: "Working", backgroundColor: "#00a0dc"
        }

        valueTile("timeValue", "device.coolingSetpoint", width: 2, height: 1, decoration: "flat") {
            state "default", label: '${currentValue} hours', backgroundColor: "#FFFFFF"
        }

        standardTile("timeDown", "device.coolingSetpoint", width: 2, height: 1, decoration: "flat") {
            state "default", action: "timerDown", icon: "st.thermostat.thermostat-down"
        }

		standardTile("timeUp", "device.coolingSetpoint", width: 2, height: 1, decoration: "flat") {
            state "default", action: "timerUp", icon: "st.thermostat.thermostat-up"
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
        valueTile("outdoorTemp", "device.outTemp", width: 2, height: 2, decoration: "flat") {
            state "default", label:'${currentValue} °F', unit: "°F", backgroundColors: [
                [value: 31, color: "#153591"],
                [value: 44, color: "#1e9cbb"],
                [value: 59, color: "#90d2a7"],
                [value: 74, color: "#44b621"],
                [value: 84, color: "#f1d801"],
                [value: 95, color: "#d04e00"],
                [value: 96, color: "#bc2323"]
			]
        }

        valueTile("fanState", "device.fanState", width: 2, height: 2, decoration: "flat") {
            state "on", icon: "st.thermostat.fan-on", backgroundColor: "#00A0DC"
            state "off", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff"
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

        main("fanMode")
        details(["thermostatMulti",
            "fanOff", "fanDelay", "fanAuto",
            "timeDown", "timeValue", "timeUp",
            "blank21", "statusLabel", "blank21",
            "thermostatMode", "outdoorTemp", "fanState",
            "reset", "refresh","lastUpdate"
        ])
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
    sendEvent(name: "coolingSetpoint", value: 4, unit: "°F")
    sendEvent(name: "coolingSetpointMin", value: COOLING_SETPOINT_RANGE.getFrom(), unit: "°F")
    sendEvent(name: "coolingSetpointMax", value: COOLING_SETPOINT_RANGE.getTo(), unit: "°F")
    sendEvent(name: "thermostatMode", value: DEFAULT_MODE)
    sendEvent(name: "thermostatFanMode", value: DEFAULT_FAN_MODE)
    sendEvent(name: "thermostatOperatingState", value: DEFAULT_OP_STATE)
    sendEvent(name: "outTemp", value: DEFAULT_OUT_TEMP, unit: "°F")
    sendEvent(name: "fanState", value: DEFAULT_FAN_STATE)

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
    sendEvent(name: "outTemp", value: getOutdoorTemp(), unit: "°F")
    sendEvent(name: "fanState", value: getFanState())
    done()
}

def setOutdoorTemp(Double degreesF) {
    log.trace "Executing 'setOutdootTemp' $degreesF"
    sendEvent(name: "outTemp", value: degreesF as Integer, unit: "F")
    done()
}

private Integer getOutdoorTemp() {
    def ts = device.currentState("outTemp")
    Integer currentTemp = DEFAULT_OUT_TEMP
    try {
        currentTemp = ts.integerValue
    } catch (all) {
        log.warn "Encountered an error getting Integer value of temperature state. Value is '$ts.stringValue'. Reverting to default of $DEFAULT_TEMPERATURE"
        setOutdoorTemp(DEFAULT_OUT_TEMP)
    }
    return currentTemp
}

def setFanState(String newFanState) {
    log.trace "Executing 'setFanState' $newFanState"
     sendEvent(name: "fanState", value: newFanState)
}

def setLastUpdate() {
    log.trace "Executing 'setFanState' $newFanState"
     sendEvent(name: "fanState", value: newFanState)
}

private String getFanState() {
    return device.currentValue("fanState") ?: DEFAULT_FAN_STATE
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

def setThermostatSetpoint(Double degreesF) {
    def newSp = boundInt(degreesF as Integer, COOLING_SETPOINT_RANGE)
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

def setTimerSetpoint(Double degreesF) {
    def newSp = boundInt(degreesF as Integer, DELAY_SETPOINT_RANGE)
    log.trace "Executing 'setTimerSetpoint' $newSp"
    sendEvent(name: "coolingSetpoint", value: newSp, unit: "F")
    done()
}

private timerUp() {
    log.trace "Executing 'timerUp'"
    def newCsp = getCoolingSetpoint() + 1
    setTimerSetpoint(newCsp)
    if (getFanMode() == FAN_MODE.CIRCULTE) {
        sendEvent(name: "thermostatFanMode", value: FAN_MODE.CIRCULTE)
    }
    done()
}

private timerDown() {
    log.trace "Executing 'timerDown'"
    def newCsp = getCoolingSetpoint() - 1
    setTimerSetpoint(newCsp)
    if (getFanMode() == FAN_MODE.CIRCULTE) {
        sendEvent(name: "thermostatFanMode", value: FAN_MODE.CIRCULTE)
    }
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
    if (getFanMode() == FAN_MODE.AUTO) {
        sendEvent(name: "thermostatFanMode", value: FAN_MODE.AUTO)
    }
    done()
}

private setpointDown() {
    log.trace "Executing 'setpointDown'"
    def newSp = getThermostatSetpoint() - 1
    setThermostatSetpoint(newSp)
    if (getFanMode() == FAN_MODE.AUTO) {
        sendEvent(name: "thermostatFanMode", value: FAN_MODE.AUTO)
    }
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