package com.world.i2v.i2vauthapi.dto.request;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String phoneNumber;
    private String alternatePhone;
    private String workEmail;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
}