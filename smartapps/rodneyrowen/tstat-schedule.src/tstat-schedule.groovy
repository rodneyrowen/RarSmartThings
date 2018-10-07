/**
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
definition(
    name: "Tstat Schedule",
    namespace: "rodneyrowen",
    author: "rrowen",
    description: "Tstat Schedule Child",
    category: "My Apps",
    parent: "rodneyrowen:Tstat Master",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "Schedule", title: "Set Schedule", install: true, uninstall: true) {
        section("Schedule Name") {
            input(name: "scheduleName", type: "text", title: "Name of this Schedule", required: true)
        }
        section("Set points") {
            input "coolingSetpoint", "number", title: "Cooling Setpoint", required: true
            input "heatingSepoint", "number", title: "Heating Setpoint", required: true
        }
        section("Priority") {
            input(name: "priority", type: "enum", title: "Priority", required: true, options: ["Inactive","Low","Medium","High"])
        }
        section("Apply Settings When...") {
	        input "sModes", "mode", title: "Only when mode is", multiple: true, required: false
        }
        section("On Which Days") {
            input "days", "enum", title: "Select Days of the Week", required: false, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday"]
        }
        section("Between what times?") {
            input "fromTime", "time", title: "From", required: false
            input "toTime", "time", title: "To", required: false
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    def name = settings.scheduleName 
    app.updateLabel("Schedule-${name}") 
    log.debug "Installed with settings: ${settings}"
}

def Integer getHeatingSetpoint() {
    def temp = settings.heatingSepoint
    temp = temp as Integer
    return temp ? temp : 60
}

def Integer getCoolingSetpoint() {
    def temp = settings.coolingSepoint
    temp = temp as Integer
    return temp ? temp : 85
}

def Integer convertPriority(strText) {
    def curPriority = settings.priority
    def curValue = 0
    if (curPriority == "High") {
        curValue = 3
    } else if (curPriority == "Medium") {
        curValue = 2
    } else if (curPriority == "Low") {
        curValue = 1
    }

    return curValue
}

def Integer isActive() {
    log.trace "IsActive: ${location.mode} Modes: ${settings.sModes}"
    def state = 0
    if (settings.sModes) {
        if (sModes.contains(location.mode)) {
            state = convertPriority(settings.priority)
		    log.trace "Schedule Priority: ${settings.priority} Returning: ${state}"
        }
    }  
    //def df = new java.text.SimpleDateFormat("EEEE")
    // Ensure the new date object is set to local time zone
    //df.setTimeZone(location.timeZone)
    //def day = df.format(new Date())
    //Does the preference input Days, i.e., days-of-week, contain today?
    //def dayCheck = days.contains(day)
    //if (dayCheck) {
        //def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
        //if (between) {
            //roomLight.on()
       // } else {
            //roomLight.off()
       // }
   // }
   return state
}
