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
 *  Copyright 2018 RAR
 *  Modified to create a Thermostat for a whole house fan.
 *  Abstracted the following from normal thermostat defintions to the following:
 *  Thermostat Modes:
 *   value used is read from the real thermostat 
 *   is auto mode, the fan will only turn on when the thermostat is 'off'
 *  Fan Mode Modes:
 *    OFF:   Turn fan off
 *    ON:    Turn fan on
 *    AUTO:  Turn fan on\off automatically based on selected set points and 
 *    Circultate:  Turn fan on for N minutes
 *    default : off
 *
 *  OpMode
 *   cooling:  Fan is on
 *   idle:     Fan is off
 *
 *   Used the hello mode to select which set points to use based on preferences.
 *   Cool Setpoints map to Schedule A
 *   Heat Setpoints map to Schedule B
 *   User will be able to select which schedule to use for a given mode.
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
@Field final List RUNNING_OP_STATES = [OP_STATE.COOLING, OP_STATE.FAN]

// config - TODO: move these to a pref page
@Field List SUPPORTED_MODES = [MODE.OFF, MODE.HEAT, MODE.AUTO, MODE.COOL, MODE.EHEAT]
@Field List SUPPORTED_FAN_MODES = [FAN_MODE.OFF, FAN_MODE.AUTO, FAN_MODE.ON]

@Field final Float    THRESHOLD_DEGREES = 1.0
@Field final Integer  SIM_HVAC_CYCLE_SECONDS = 15
@Field final Integer  DELAY_EVAL_ON_MODE_CHANGE_SECONDS = 3

@Field final Integer  INC_SETPOINT = 1
@Field final Integer  DEC_SETPOINT = -1

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
@Field final String   DEFAULT_FAN_MODE = FAN_MODE.OFF
@Field final String   DEFAULT_OP_STATE = OP_STATE.IDLE
@Field final String   DEFAULT_PREVIOUS_STATE = OP_STATE.HEATING
@Field final String   DEFAULT_SETPOINT_TYPE = SETPOINT_TYPE.HEATING
@Field final Integer  DEFAULT_TEMPERATURE = 72
@Field final Integer  DEFAULT_HEATING_SETPOINT = 68
@Field final Integer  DEFAULT_COOLING_SETPOINT = 80
@Field final Integer  DEFAULT_THERMOSTAT_SETPOINT = DEFAULT_HEATING_SETPOINT


metadata {
    // Automatically generated. Make future change here.
    definition (name: "Fan Thermostat All", namespace: "rodneyrowen", author: "rrowen") {
        capability "Sensor"
        capability "Actuator"
        capability "Health Check"

        capability "Thermostat"
        capability "Configuration"
        capability "Refresh"

		//attribute "outTemp", "number"

        command "tempUp"
        command "tempDown"
        command "heatUp"
        command "heatDown"
        command "coolUp"
        command "coolDown"
        command "setpointUp"
        command "setpointDown"
        command "outUp"
        command "outDown"

        command "cycleMode"
        command "cycleFanMode"

        command "setTemperature", ["number"]
        command "delayedEvaluate"
        command "runSimHvacCycle"
    }

	section("Outdoor") {
		input "outTemp", "capability.temperatureMeasurement", title: "Outdoor Thermometer"
	}
    
    section("Indoor") {
    	input "inTemp", "capability.temperatureMeasurement", title: "Indoor Thermometer"
        input "fans", "capability.switch", title: "Vent Fan"
    }
    
    section("Thermostat") {
    	input "thermostat", "capability.thermostat", title: "Thermostat"
    }    

//	preferences {
//    	input "inTemp", "capability.temperatureMeasurement", title: "Indoor Thermometer"
//    	input "outTemp", "capability.temperatureMeasurement", title: "Outdoor Thermometer"
//    	input "fans", "capability.switch", title: "Fan Switch"
//    	input "thermostat", "capability.thermostat", title: "Thermostat"
//       	input "setpointA", "number", title: "Set Point A"
    	//input "aModes", "mode", title: "Set Point A mode(s)"
//    	input "setpointB", "n//umber", title: "Set Point B"
    	//input "scheduleBmodes", "mode", title: "select a mode(s)", multiple: true
//	}

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
            tileAttribute("device.device.thermostatOperatingState", key: "SECONDARY_CONTROL") {
                attributeState("default", label: '${currentValue}', icon: "st.thermostat.fan-off")
            }
        }

        valueTile("thermostatMode", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
            state "off", label:'Thermostat Off', backgroundColor:"#00a0dc"
            state "cool", label:'Thermostat Cool', backgroundColor:"#cccccc"
            state "heat", label:'Thermostat Heat', backgroundColor:"#cccccc"
            state "auto", label:'Thermostat Auto', backgroundColor:"#cccccc"
            state "default", label: 'Unknown', backgroundColor:"#cccccc"
        }

        valueTile("thermostatOpState", "device.thermostatOperatingState", width: 2, height: 2, decoration: "flat") {
            state "cooling", label: 'Cooling On', backgroundColor:"#00a0dc"
            state "fan only", label: 'Fan On', backgroundColor:"#00a0dc"
            state "pending cool", label: 'Cool Pending', backgroundColor:"#00609c"
            state "idle", label: 'Off', backgroundColor:"#FFFFFF"
            state "default", label: '${currentValue}', backgroundColor:"#FFFFFF"
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
        
		standardTile("fanMode", "device.thermostatFanMode", width: 2, height: 2, decoration: "flat") {
            state "off",       action: "cycleFanMode", label: "OFF", nextState: "updating", icon: "st.thermostat.fan-off", backgroundColor: "#FFFFFF", defaultState: true
            state "auto",      action: "cycleFanMode", label: "Auto", nextState: "updating", icon: "st.thermostat.fan-auto", backgroundColor: "#00a0dc"
            state "on",        action: "cycleFanMode", label: "ON", nextState: "updating", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc"
            state "circulate", action: "cycleFanMode", label: "DELAY", nextState: "updating", icon: "st.thermostat.fan-circulate", backgroundColor: "#00a0dc"
            state "updating", label: "Working", backgroundColor: "#00a0dc"
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
        
        standardTile("mode", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
            state "off",            action: "cycleMode", nextState: "updating", icon: "st.thermostat.heating-cooling-off", backgroundColor: "#CCCCCC", defaultState: true
            state "heat",           action: "cycleMode", nextState: "updating", icon: "st.thermostat.heat"
            state "cool",           action: "cycleMode", nextState: "updating", icon: "st.thermostat.cool"
            state "auto",           action: "cycleMode", nextState: "updating", icon: "st.thermostat.auto"
            state "emergency heat", action: "cycleMode", nextState: "updating", icon: "st.thermostat.emergency-heat"
            state "updating", label: "Working"
        }

        valueTile("tempIn", "device.temperature", width: 2, height: 2, decoration: "flat") {
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
        standardTile("tempDown", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "room", action: "tempDown", icon: "st.thermostat.thermostat-down"
        }
        standardTile("tempUp", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "room", action: "tempUp", icon: "st.thermostat.thermostat-up"
        }

        standardTile("outdoorDown", "state.outTemp", width: 1, height: 1, decoration: "flat") {
            state "default", label: "outdoor", action: "outDown", icon: "st.thermostat.thermostat-down"
        }
        standardTile("outdoorUp", "state.outTemp", width: 1, height: 1, decoration: "flat") {
            state "default", label: "outdoor", action: "outUp", icon: "st.thermostat.thermostat-up"
        }

        // To modify the simulation environment
        valueTile("simControlLabel", "device.switch", width: 4, height: 1, decoration: "flat") {
            state "default", label: "Simulated Environment Control"
        }

        valueTile("blank1x1", "device.switch", width: 1, height: 1, decoration: "flat") {
            state "default", label: ""
        }
        valueTile("blank2x2", "device.switch", width: 2, height: 2, decoration: "flat") {
            state "default", label: ""
        }

        valueTile("reset", "device.switch", width: 2, height: 2, decoration: "flat") {
            state "default", label: "Reset to Defaults", action: "configure"
        }

        main("fanMode")
        details(["thermostatMulti",
            "thermostatMode", "roomTemp", "outdoorTemp",
            "fanMode", "thermostatOpState", "blank2x2",
            "blank1x1", "simControlLabel", "blank1x1",
            //"mode","tempUp", "outdoorUp",
            "coolingSetpoint",
            "heatingSetpoint",
            "reset",
            "coolUp",
            "coolDown",
            "heatUp",
			//"tempDown", "outdoorDown",
            "heatDown"
        ])
    }
}


def installed() {
    log.trace "Executing 'installed'"
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
    sendEvent(name: "checkInterval", value: 12 * 60, displayed: false, data: [protocol: "cloud", scheme: "untracked"])

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

    subscribe(outTemp, "temperature", "evaluateOperatingState");
    subscribe(temperature, "temperature", "evaluateOperatingState");
    subscribe(thermostat, "thermostatMode", "evaluateOperatingState");
    
    state.isHvacRunning = false
    state.lastOperatingState = DEFAULT_OP_STATE
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


def ping() {
    log.trace "Executing ping"
    refresh()
    // done() called by refresh()
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
	def tm = settings.thermostat.currentValue('thermostatMode')
    //return device?.currentValue("thermostatMode") ?: DEFAULT_MODE
    return tm
}

def setThermostatMode(String value) {
    log.trace "Executing 'setThermostatMode' $value"
    if (value in SUPPORTED_MODES) {
        sendEvent(name: "thermostatMode", value: value)
        evaluateOperatingState()
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

private Boolean isThermostatOff() {
    return getThermostatMode() == MODE.OFF
}

// Fan mode
private String getFanMode() {
    return device.currentValue("thermostatFanMode") ?: DEFAULT_FAN_MODE
}

def setThermostatFanMode(String value) {
    if (value in SUPPORTED_FAN_MODES) {
        sendEvent(name: "thermostatFanMode", value: value)
        evaluateOperatingState()
    } else {
        log.warn "'$value' is not a supported fan mode. Please set one of ${SUPPORTED_FAN_MODES.join(', ')}"
    }
}

private cycleFanMode() {
    log.trace "Executing 'cycleFanMode'"
    //String nextMode = nextListElement(SUPPORTED_FAN_MODES, getFanMode())
    //setThermostatFanMode(nextMode)
    parent.cycleFaneMode()
    done()
//    return nextMode
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

private setOperatingState(String operatingState) {
    if (operatingState in OP_STATE.values()) {
        sendEvent(name: "thermostatOperatingState", value: operatingState)
        log.debug "Set operating state: $operatingState"
        state.lastOperatingState = operatingState
        if (operatingState in OP_STATE.values()) {
        	setFanState(true)
		} else {
        	setFanState(false)
        }
        
    } else {
        log.warn "'$operatingState' is not a supported operating state. Please set one of ${OP_STATE.values().join(', ')}"
    }
}

def setFanState(shouldRun) {
    if(shouldRun && !state.fanRunning) {
    	fans.on();
        state.fanRunning = true;
        log.debug "Turn Fan On"
    } else if(!shouldRun && state.fanRunning) {
    	fans.off();
        state.fanRunning = false;
        log.debug "Turn Fan Off"
    }
}

// setpoint
private Integer getThermostatSetpoint() {
    def ts = device.currentState("thermostatSetpoint")
    return ts ? ts.getIntegerValue() : DEFAULT_THERMOSTAT_SETPOINT
}

def setThermostatSetpoint(Integer degreesF) {
    log.trace "Executing 'setThermostatSetpoint' $degreesF"
    sendEvent(name:"thermostatSetpoint", value: degreesF as Integer )
    evaluateOperatingState()
    done()
}

private setpointUp() {
    log.trace "Executing 'setpointUp'"
    def newValue = changeSetpoint(getThermostatSetpoint(), INC_SETPOINT)
    setThermostatSetpoint(newValue)
    done()
}

private setpointDown() {
    log.trace "Executing 'setpointDown'"
    def newValue = changeSetpoint(getThermostatSetpoint(), DEC_SETPOINT)
    setThermostatSetpoint(newValue)
    done()
}

private Integer getHeatingSetpoint() {
    def hs = device.currentState("heatingSetpoint")
    return hs ? hs.getIntegerValue() : DEFAULT_HEATING_SETPOINT
}

def setHeatingSetpoint(Integer degreesF) {
    log.trace "Executing 'setHeatingSetpoint' $degreesF"
    sendEvent(name:"heatingSetpoint", value: degreesF as Integer )
    done()
}

private heatUp() {
    log.trace "Executing 'heatUp'"
    def newValue = changeSetpoint(getHeatingSetpoint(), INC_SETPOINT)
    setHeatingSetpoint(newValue)
    done()
}

private heatDown() {
    log.trace "Executing 'heatDown'"
    def newValue = changeSetpoint(getHeatingSetpoint(), DEC_SETPOINT)
    setHeatingSetpoint(newValue)
    done()
}

private Integer getCoolingSetpoint() {
    def cs = device.currentState("coolingSetpoint")
    return cs ? cs.getIntegerValue() : DEFAULT_COOLING_SETPOINT
}

def setCoolingSetpoint(Double degreesF) {
    log.trace "Executing 'setCoolingSetpoint' $degreesF"
    sendEvent(name:"coolingSetpoint", value: degreesF as Integer )
    done()
}

private coolUp() {
    log.trace "Executing 'coolUp'"
    def newValue = changeSetpoint(getCoolingSetpoint(), INC_SETPOINT)
    setCoolingSetpoint(newValue)
    done()
}

private coolDown() {
    log.trace "Executing 'coolDown'"
    def newValue = changeSetpoint(getCoolingSetpoint(), DEC_SETPOINT)
    setCoolingSetpoint(newValue)
    done()
}

// simulated temperatures
private Integer getTemperature() {
	def outsideTemp = settings.outTemp.currentValue('temperature')
    log.trace "Outdoor Temp $outTemp.stringValue"

//    def ts = settings.inTemp.currentValue('temperature')
//    Integer currentTemp = DEFAULT_TEMPERATURE
//    try {
//        currentTemp = ts.integerValue
//    } catch (all) {//
//        log.warn "E//ncountered an error getting Integer value of temperature state. Value is '$ts.stringValue'. Reverting to default of $DEFAULT_TEMPERATURE"
//        setTemperat//ure(DEFAULT_TEMPERATURE)
   // }
    return outsideTemp
}

// changes the "room" temperature for the simulation
private setTemperature(newTemp) {
    //sendEvent(name:"temperature", value: newTemp)
    evaluateOperatingState()
} 

private tempUp() {
    def newTemp = getTemperature() ? getTemperature() + 1 : DEFAULT_TEMPERATURE
    setTemperature(newTemp)
}

private tempDown() {
    def newTemp = getTemperature() ? getTemperature() - 1 : DEFAULT_TEMPERATURE
    setTemperature(newTemp)
}

private Integer getOutTemp() {
    def ts = settings.outTemp.currentValue('temperature')
    //def ts = device.currentState("heatingSetpoint")
    Integer currentTemp = DEFAULT_TEMPERATURE
    try {
        currentTemp = ts.integerValue
    } catch (all) {
        log.warn "Encountered an error getting Integer value of temperature state. Value is '$ts.stringValue'. Reverting to default of $DEFAULT_TEMPERATURE"
        setTemperature(DEFAULT_TEMPERATURE)
    }
    return currentTemp
}

// changes the "outdoor" temperature for the simulation
private setOutTemp(newTemp) {
    //sendEvent(name: "outTemp", value: newTemp)
    evaluateOperatingState()
} 

private outUp() {
    def newTemp = getOutTemp() ? getOutTemp() + 1 : DEFAULT_TEMPERATURE
    setOutTemp(newTemp)
}

private outDown() {
    def newTemp = getOutTemp() ? getOutTemp() - 1 : DEFAULT_TEMPERATURE
    setOutTemp(newTemp)
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

private Integer changeSetpoint(Number value, Number change) {
    value = value + change
    return boundInt(value, FULL_SETPOINT_RANGE)
}

private evaluateSetpoints() {
    Integer newASetpoint = 70;
    Integer newBSetpoint = 72;

	setCoolingSetpoint(newASetpoint)
	setHeatingSetpoint(newBSetpoint)
    
    setThermostatSetpoint(newASetpoint)
}

// sets the thermostat setpoint and operating state and starts the "HVAC" or lets it end.
private evaluateOperatingState(Map overrides) {

    // check for override values, otherwise use current state values
    //Integer currentTemp = overrides.find { key, value -> 
    //        "$key".toLowerCase().startsWith("curr")|"$key".toLowerCase().startsWith("temp")
    //    }?.value?:getTemperature() as Integer
    //Integer heatingSetpoint = overrides.find { key, value -> "$key".toLowerCase().startsWith("heat") }?.value?:getHeatingSetpoint() as Integer
    //Integer coolingSetpoint = overrides.find { key, value -> "$key".toLowerCase().startsWith("cool") }?.value?:getCoolingSetpoint() as Integer

    String tsMode = getFanMode()

    if (tsMode == FAN_MODE.AUTO) {
        def outsideTemp = getOutTemp()
        def currentTemp = getTemperature()
        def thermostatMode = getThermostatMode()
        def setPoint = getThermostatSetpoint()
        log.debug "evaluate theromstat: $thermostatMode, temp: $currentTemp, outdoor: $outTemp, setpoint: $setPoint"
		if ( (thermostatMode == MODE.OFF) &&
             (outsideTemp < currentTemp) &&
             (currentTemp > setPoint) ) {
        	setOperatingState(OP_STATE.COOLING)
        } else {
        	setOperatingState(OP_STATE.PEND_COOL)
        }
    } else if (tsMode in FAN_MODE.ON) {
        log.debug "evaluate theromstat: FAN MODE ON"
        setOperatingState(OP_STATE.FAN)
    }
    else {
        log.debug "evaluate theromstat: FAN MODE ON"
        setOperatingState(OP_STATE.IDLE)
    }
}

/**
 * Just mark the end of the execution in the log
 */
private void done() {
    log.trace "---- DONE ----"
}
