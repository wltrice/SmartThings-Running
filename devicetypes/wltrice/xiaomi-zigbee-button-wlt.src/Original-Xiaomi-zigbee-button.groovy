/**
 *  Original Xiaomi Zigbee Button
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
  * Based on original DH by Eric Maycock 2015 and Rave from Lazcad
 *  change log:
 *  25.01.2018 added virtualApp button on tile
 *  added 100% battery max
 *  fixed battery parsing problem
 *  added lastcheckin attribute and tile
 *  added a means to also push button in as tile on smartthings app
 *  fixed ios tile label problem and battery bug 
 *  sulee: change battery calculation
 *  sulee: changed to work as a push button
 *  sulee: added endpoint for Smartthings to detect properly
 *  sulee: cleaned everything up
 *  bspranger: renamed to bspranger to remove confusion of a4refillpad
 *  renamed from bspranger to wltrice to prevent comfusion of which DH I'm running
 *
 *  Fingerprint Endpoint data:
 *  zbjoin: {"dni":"xxxx","d":"xxxxxxxxxxx","capabilities":"80","endpoints":[{"simple":"01 0104 5F01 01 03 0000 FFFF 0006 03 0000 0004 FFFF","application":"03","manufacturer":"LUMI","model":"lumi.sensor_switch.aq2"}],"parent":"0000","joinType":1}
 *     endpoints data
 *        01 - endpoint id
 *        0104 - profile id
 *        5F01 - device id
 *        01 - ignored
 *        03 - number of in clusters
 *        0000 ffff 0006 - inClusters
 *        03 - number of out clusters
 *        0000 0004 ffff - outClusters
 *        manufacturer "LUMI" - must match manufacturer field in fingerprint
 *        model "lumi.sensor_switch.aq2" - must match model in fingerprint
 *        deviceJoinName: whatever you want it to show in the app as a Thing
 *
 */
preferences {
	input ("holdTime", "number", title: "Minimum time in seconds for a press to count as \"held\"", defaultValue: 4, displayDuringSetup: false)
        input name: "PressType", type: "enum", options: ["Toggle", "Momentary"], description: "Effects how the button toggles", defaultValue: "Toggle", displayDuringSetup: true
    	input name: "dateformat", type: "enum", title: "Set Date Format\n US (MDY) - UK (DMY) - Other (YMD)", description: "Date Format", required: false, options:["US","UK","Other"]
	input description: "Only change the settings below if you know what you're doing", displayDuringSetup: false, type: "paragraph", element: "paragraph", title: "ADVANCED SETTINGS"
	input name: "voltsmax", title: "Max Volts\nA battery is at 100% at __ volts\nRange 2.8 to 3.4", type: "decimal", range: "2.8..3.4", defaultValue: 3, required: false
	input name: "voltsmin", title: "Min Volts\nA battery is at 0% (needs replacing) at __ volts\nRange 2.0 to 2.7", type: "decimal", range: "2..2.7", defaultValue: 2.5, required: false
} 
 
metadata {
    definition (name: "Xiaomi Button", namespace: "bspranger", author: "bspranger") {
        capability "Battery"
        capability "Button"
        capability "Configuration"
        capability "Sensor"
	capability "Health Check"
        capability "Momentary"
        
        attribute "lastpressed", "string"
        attribute "lastpressedDate", "string"
        attribute "batterylevel", "string"
        attribute "lastCheckin", "string"
        attribute "lastCheckinDate", "Date"
        attribute "batteryRuntime", "String"

        fingerprint endpointId: "01", profileId: "0104", deviceId: "0104", inClusters: "0000,0003,FFFF,0019", outClusters: "0000,0004,0003,0006,0008,0005,0019", manufacturer: "LUMI", model: "lumi.sensor_switch", deviceJoinName: "Original Xiaomi Button"

        command "resetBatteryRuntime"
}
    
    simulator {
          status "button 1 pressed": "on/off: 0"
          status "button 1 released": "on/off: 1"
    }
    
    tiles(scale: 2) {

        multiAttributeTile(name:"button", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute ("device.button", key: "PRIMARY_CONTROL") {
                   attributeState("pushed", label:'Push', action: "momentary.push", backgroundColor:"#00a0dc")
                attributeState("released", label:'Push', action: "momentary.push", backgroundColor:"#ffffff", nextState: "pushed")
             }
            tileAttribute("device.lastpressed", key: "SECONDARY_CONTROL") {
                attributeState "default", label:'Last Pressed: ${currentValue}'
            }
        }        
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "default", label:'${currentValue}%', unit:"",
			backgroundColors:[
                [value: 10, color: "#bc2323"],
                [value: 26, color: "#f1d801"],
                [value: 51, color: "#44b621"]
			]
        }
        valueTile("lastcheckin", "device.lastCheckin", decoration: "flat", inactiveLabel: false, width: 4, height: 1) {
            state "default", label:'Last Checkin:\n${currentValue}'
        }
	valueTile("batteryRuntime", "device.batteryRuntime", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
	    state "batteryRuntime", label:'Battery Changed: ${currentValue} - Tap to reset Date', unit:"", action:"resetBatteryRuntime"
	}  	    
        main (["button"])
        details(["button","battery","lastcheckin","batteryRuntime"])
   }
}

//adds functionality to press the centre tile as a virtualApp Button
def push() {
	log.debug "Virtual App Button Pressed"
	def now = formatDate()
	def nowDate = new Date(now).getTime()
        sendEvent(name: "lastpressed", value: now, displayed: false)
        sendEvent(name: "lastpressedDate", value: nowDate, displayed: false) 
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "$device.displayName app button was pushed", isStateChange: true)
	sendEvent(name: "button", value: "released", data: [buttonNumber: 1], descriptionText: "$device.displayName app button was released", isStateChange: true)
}

def parse(String description) {
    log.debug "${device.displayName}: Parsing '${description}'"

    //  send event for heartbeat    
    def now = formatDate()	
    def nowDate = new Date(now).getTime()
    sendEvent(name: "lastCheckin", value: now)
    sendEvent(name: "lastCheckinDate", value: nowDate, displayed: false) 

    Map map = [:]

    if (description?.startsWith('on/off: ')) 
    {
        map = parseCustomMessage(description) 
        sendEvent(name: "lastpressed", value: now, displayed: false)
        sendEvent(name: "lastpressedDate", value: nowDate, displayed: false) 
    }
    else if (description?.startsWith('catchall:')) 
    {
        map = parseCatchAllMessage(description)
    }
    else if (description?.startsWith("read attr - raw: "))
    {
        map = parseReadAttrMessage(description)  
    }
    log.debug "${device.displayName}: Parse returned $map"
    def results = map ? createEvent(map) : null

    return results;
}

private Map parseReadAttrMessage(String description) {
    def buttonRaw = (description - "read attr - raw:")
    Map resultMap = [:]

    def cluster = description.split(",").find {it.split(":")[0].trim() == "cluster"}?.split(":")[1].trim()
    def attrId = description.split(",").find {it.split(":")[0].trim() == "attrId"}?.split(":")[1].trim()
    def value = description.split(",").find {it.split(":")[0].trim() == "value"}?.split(":")[1].trim()
    def model = value.split("01FF")[0]
    def data = value.split("01FF")[1]
    log.debug "cluster: ${cluster}, attrId: ${attrId}, value: ${value}, model:${model}, data:${data}"
    
    if (data[4..7] == "0121") {
    	def BatteryVoltage = (Integer.parseInt((data[10..11] + data[8..9]),16))
        resultMap = getBatteryResult(BatteryVoltage)
        log.debug "${device.displayName}: Parse returned $resultMap"
        createEvent(resultMap)
    }

    if (cluster == "0000" && attrId == "0005")  {
        resultMap.name = 'Model'
        resultMap.value = ""
        resultMap.descriptionText = "device model"
        // Parsing the model
        for (int i = 0; i < model.length(); i+=2) 
        {
            def str = model.substring(i, i+2);
            def NextChar = (char)Integer.parseInt(str, 16);
            resultMap.value = resultMap.value + NextChar
        }
        return resultMap
    }
    
    return [:]    
}

private Map parseCatchAllMessage(String description) {
    def i
    Map resultMap = [:]
    def cluster = zigbee.parse(description)
    log.debug cluster
    if (cluster) {
        switch(cluster.clusterId) {
            case 0x0000:
                def MsgLength = cluster.data.size();
                for (i = 0; i < (MsgLength-3); i++)
                {
                    // Original Xiaomi CatchAll does not have identifiers, first UINT16 is Battery
                    if ((cluster.data.get(0) == 0x02) && (cluster.data.get(1) == 0xFF))
                    {
                        if (cluster.data.get(i) == 0x21) // check the data ID and data type
                        {
                            // next two bytes are the battery voltage.
                            resultMap = getBatteryResult((cluster.data.get(i+2)<<8) + cluster.data.get(i+1))
                            return resultMap
                        }
                    }
                }
            break
        }
    }
    return resultMap
}

private Map getBatteryResult(rawValue) {
    def rawVolts = rawValue / 1000
    def minVolts
    def maxVolts

    if(voltsmin == null || voltsmin == "")
    	minVolts = 2.5
    else
   	minVolts = voltsmin
    
    if(voltsmax == null || voltsmax == "")
    	maxVolts = 3.0
    else
	maxVolts = voltsmax
	
    def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
    def roundedPct = Math.min(100, Math.round(pct * 100))

    def result = [
        name: 'battery',
        value: roundedPct,
        unit: "%",
        isStateChange:true,
        descriptionText : "${device.displayName} raw battery is ${rawVolts}v"
    ]
    
    log.debug "${device.displayName}: ${result}"
    return createEvent(result)
}

private Map parseCustomMessage(String description) {
    def result = [:]
    if (description?.startsWith('on/off: ')) 
    {
        if (PressType == "Toggle")
        {
            if ((state.button != "pushed") && (state.button != "released")) 
            {
                state.button = "released"
            }
            if (description == 'on/off: 0')
            {
                if (state.button == "released") 
                {
                   result = getContactResult("pushed")
                   state.button = "pushed"
                }
                else 
                {
                   result = getContactResult("released")
                   state.button = "released"
                }
            }
        }
        else
        {
            if (description == 'on/off: 0')
            {
                result = getContactResult("pushed")
            } 
            else if (description == 'on/off: 1')
            {
                result = getContactResult("released")
            }
        }
        return result
    }
}

private Map getContactResult(value) {
    def descriptionText = "${device.displayName} was ${value == 'pushed' ? 'pushed' : 'released'}"
    return [
        name: 'button',
        value: value,
	data: [buttonNumber: "1"],
        descriptionText: descriptionText,
	isStateChange: true
    ]
}

def resetBatteryRuntime() {
    def now = formatDate(true)
    sendEvent(name: "batteryRuntime", value: now)
}

def configure() {
    log.debug "${device.displayName}: configuring"
    state.battery = 0
    state.button = "released"
    checkIntervalEvent("configure");
}

def installed() {
    state.battery = 0
    state.button = "released"
    checkIntervalEvent("installed");
}

def updated() {
    checkIntervalEvent("updated");
}

private checkIntervalEvent(text) {
    // Device wakes up every 1 hours, this interval allows us to miss one wakeup notification before marking offline
    log.debug "${device.displayName}: Configured health checkInterval when ${text}()"
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

def formatDate(batteryReset) {
    def correctedTimezone = ""

    if (!(location.timeZone)) {
        correctedTimezone = TimeZone.getTimeZone("GMT")
        log.error "${device.displayName}: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app."
        sendEvent(name: "error", value: "", descriptionText: "ERROR: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app.")
    } 
    else {
        correctedTimezone = location.timeZone
    }
    if (dateformat == "US" || dateformat == "" || dateformat == null) {
        if (batteryReset)
            return new Date().format("MMM dd yyyy", correctedTimezone)
        else
            return new Date().format("EEE MMM dd yyyy h:mm:ss a", correctedTimezone)
    }
    else if (dateformat == "UK") {
        if (batteryReset)
            return new Date().format("dd MMM yyyy", correctedTimezone)
        else
            return new Date().format("EEE dd MMM yyyy h:mm:ss a", correctedTimezone)
        }
    else {
        if (batteryReset)
            return new Date().format("yyyy MMM dd", correctedTimezone)
        else
            return new Date().format("EEE yyyy MMM dd h:mm:ss a", correctedTimezone)
    }
}
