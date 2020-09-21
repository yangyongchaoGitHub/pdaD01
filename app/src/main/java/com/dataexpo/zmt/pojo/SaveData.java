package com.dataexpo.zmt.pojo;

public class SaveData {
    private Integer id;
    private String eucode;
    private String name;
    private String idcard;
    private String temp;
    private String time;
    private String address;
    //数据类型，0离线模式数据，1在线模式数据
    private Integer modeType;

    public SaveData(){}

    public SaveData(int id, String eucode, String time, String name, String idcard, String temperature, Integer modeType, String address) {
        this.id = id;
        this.eucode = eucode;
        this.time = time;
        this.name = name;
        this.idcard = idcard;
        this.temp = temperature;
        this.modeType = modeType;
        this.address = address;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getModeType() {
        return modeType;
    }

    public void setModeType(Integer modeType) {
        this.modeType = modeType;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getEucode() {
        return eucode;
    }

    public void setEucode(String eucode) {
        this.eucode = eucode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdcard() {
        return idcard;
    }

    public void setIdcard(String idcard) {
        this.idcard = idcard;
    }

    public String getTemp() {
        return temp;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }
}
