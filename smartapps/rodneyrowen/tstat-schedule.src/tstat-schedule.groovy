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
    section("Set points") {
        input "coolingSetpoint", "number", title: "Cooling Setpoint"
        input "heatingSepoint", "number", title: "Heating Setpoint"
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
    def name = app.getLabel() 
    app.updateLabel("Schedule-${name}") 
}

def Integer getHeatingSetpoint() {
    def temp = settings.heatingSepoint
    return temp ? temp.getIntegerValue() : 60
}

def Integer getCoolingSetpoint() {
    def temp = settings.coolingSepoint
    return temp ? temp.getIntegerValue() : 60
}