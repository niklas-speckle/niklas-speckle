#include "sensor.h"


sensor_data::sensor_data(){
    //constructor for object
        this->temperature = 0;
        this->humidity = 0;
        this->light_intensity =0;
        this->pollution_VOC=0;
    };
void sensor_data::set_sensor(float temperature, float humindity, int light_intensity, float  VOC){
    //setter for object
        this->temperature = temperature;
        this->humidity = humindity;
        this->light_intensity = light_intensity;
        this->pollution_VOC =  VOC;
    };
void sensor_data::print() {
    //print attributes of object in terminal
        Serial.print("Temperature : ");
        Serial.println(this->temperature);
        Serial.print("Humidity : ");
        Serial.println(this->humidity);
        Serial.print("light_intensity : ");
        Serial.println(this->light_intensity);
        Serial.print("pollution_VOC : ");
        Serial.println(this->pollution_VOC);
    };
float sensor_data::gettemp(){
    return this->temperature;
    };
float sensor_data::gethum(){
    return this->humidity;
};
float sensor_data::getpollution(){
    return this->pollution_VOC;
};
float sensor_data::getlight(){
    return (float) this->light_intensity;
};



bool getButton(Workmode *workmode){
    //function to read the buttons and if something got pressed its getting stored in workmode
    if(!digitalRead(INOFFICE_PIN)){
        *workmode = Workmode::AVAILABLE;
        return true; 
    }
    else if(!digitalRead(DEEPWORK_PIN)){
        *workmode = Workmode::DEEP_WORK;
        return true;
    }
    else if(!digitalRead(MEETING_PIN)){
        *workmode = Workmode::MEETING;
        return true;
    }
    else if(!digitalRead(OUTOFFOFFICE_PIN)){
        *workmode = Workmode::OUT_OF_OFFICE;
        return true;
    }
    return false;
};

String getWorkmode(Workmode *workmode){
    //function to convert Enum in String
   switch (*workmode){
    case  Workmode::AVAILABLE:
        return "AVAILABLE";
    case Workmode::DEEP_WORK:
        return "DEEP_WORK";
    case Workmode::MEETING:
        return "MEETING";
    case Workmode::OUT_OF_OFFICE:
        return "OUT_OF_OFFICE";
    default: 
        return "Error Workmode not Set";
   }
};




void data_average(sensor_data *data, sensor_data *average, const int length){
    //calculate the average of a list of data points and store it in average
    float temp = 0;
    float hum  = 0;
    float pollution = 0;
    int light = 0;


    //calculate sum of the data 
    for(int i = 0; i < length; i++){
        temp +=data[i].gettemp();
        hum  +=data[i].gethum();
        pollution += data[i].getpollution();
        light += data[i].getlight();
    }
    //set new average from sum 
    average->set_sensor(temp/(float) length,hum/(float) length,light/ length,pollution/(float) length);
}

int read_light_intensity(void){
    //read the light intensity from the diode
    //returns and int 
    return analogRead(PIN_LIGHT_INTENSITY);
};


Adafruit_BME680 *creatbme(void){
    //creat a static BME object
    //set the sampling
    //return a pointer to the static object
    static Adafruit_BME680 bme;
    

    // Set up oversampling and filter initialization
    bme.setTemperatureOversampling(BME680_OS_8X);
    bme.setHumidityOversampling(BME680_OS_2X);
    bme.setPressureOversampling(BME680_OS_4X);
    bme.setIIRFilterSize(BME680_FILTER_SIZE_3);
    bme.setGasHeater(320, 150);
    if (!bme.begin()) {
        Serial.println(F("Could not find a valid BME680 sensor, check wiring!"));
        while (1);
    }

    //return pointer to bme 
    return &bme;
}