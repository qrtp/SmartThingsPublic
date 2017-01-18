/**
 *  Whole House Fan
 *
 *  Copyright 2014 Brian Steere
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
    name: "Whole House Fan",
    namespace: "afewremarks",
    author: "Brian Steere & Mark West",
    description: "Toggle a whole house fan (switch) when: Outside is cooler than inside, Inside is above x temp, Inxide is above x humidty",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan%402x.png"
)


preferences {
	section("Outdoor") {
		input "outTemp", "capability.temperatureMeasurement", title: "Outdoor Thermometer", required: true
	}
    
    section("Indoor") {
    	input "inTemp", "capability.temperatureMeasurement", title: "Indoor Thermometer", required: true
        input "inHumidity", "capability.relativeHumidityMeasurement", title: "Indoor Humidity", required: true
        input "minTemp", "number", title: "Minimum Indoor Temperature"
        input "maxHumidity", "number", title: "Maximum Indoor Humidity"
        input "fans", "capability.switch", title: "Vent Fan", multiple: true, required: true
    }
    
    section("Windows/Doors") {
    	paragraph "[Optional] Only turn on the fan if at least one of these is open"
        input "checkContacts", "enum", title: "Check windows/doors", options: ['Yes', 'No'], required: true 
    	input "contacts", "capability.contactSensor", title: "Windows/Doors", multiple: true, required: false
    }
    
    section("Notifications") {
    	input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false
    	input "phone", "phone", title: "Send a Text Message?", required: false
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

def initialize() {
	state.fanRunning = false;
    
    subscribe(outTemp, "temperature", "checkThings");
    subscribe(inTemp, "temperature", "checkThings");
    subscribe(inHumidity, "humidity", "checkThings");
    subscribe(contacts, "contact", "checkThings");
}

def checkThings(evt) {
	def outsideTemp = settings.outTemp.currentTemperature
    def insideTemp = settings.inTemp.currentTemperature
    def insideHumidity = settings.inHumidity.currentHumidity
    def somethingOpen = settings.checkContacts == 'No' || settings.contacts?.find { it.currentContact == 'open' }
    
    log.debug "Inside Temp: $insideTemp, Inside Humidity: $insideHumidity, Outside Temp: $outsideTemp, Something Open: $somethingOpen"
    
    def shouldRun = true;
    
    if(insideTemp < outsideTemp) {
    	log.debug "Not running due to insideTemp > outdoorTemp"
    	shouldRun = false;
    }
    
    if(insideTemp < settings.minTemp) {
    	log.debug "Not running due to insideTemp < minTemp"
    	shouldRun = false;
    }
    
    if(!somethingOpen) {
    	log.debug "Not running due to nothing open"
        shouldRun = false;
    }

    if(!shouldRun && insideHumidity > settings.maxHumidity) {
        log.debug "Running due to humidity threshold"
        shouldRun = true;
    }
    
    if(shouldRun && !state.fanRunning) {
    	fans.on();
        state.fanRunning = true;
        sendPush("Turn on the house fan now.")
    } else if(!shouldRun && state.fanRunning) {
    	fans.off();
        state.fanRunning = false;
        sendPush("Turn off the house fan now.")
    }
}
