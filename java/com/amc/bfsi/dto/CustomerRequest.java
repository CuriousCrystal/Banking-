package com.amc.bfsi.dto;

import jakarta.validation.constraints.*;

public class CustomerRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 60, message = "Name must be between 3 and 60 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    private String email;

    @NotBlank(message = "City is required")
    private String city;

    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]", message = "PAN must look like ABCDE1234F")
    private String panNumber;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }
}
