package io.google.placestore.model;

public class PlaceLatLng {
    
    private double latitude;
    private double longitude;
    private String address;
    
    private String name;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public PlaceLatLng(){
    }
    
    public PlaceLatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public PlaceLatLng(double latitude, double longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }
    
    public PlaceLatLng(double latitude, double longitude, String address, String name) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.name = name;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
