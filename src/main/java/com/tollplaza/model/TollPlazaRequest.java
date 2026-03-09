package com.tollplaza.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to find toll plazas between two pincodes")
public class TollPlazaRequest {
    
    @Schema(
        description = "Source pincode (6-digit Indian postal code)",
        example = "110001",
        pattern = "^[0-9]{6}$",
        required = true
    )
    @NotNull(message = "Source pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Source pincode must be 6 digits")
    private String sourcePincode;
    
    @Schema(
        description = "Destination pincode (6-digit Indian postal code, must differ from source)",
        example = "560001",
        pattern = "^[0-9]{6}$",
        required = true
    )
    @NotNull(message = "Destination pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Destination pincode must be 6 digits")
    private String destinationPincode;

    public TollPlazaRequest() {
    }

    public TollPlazaRequest(String sourcePincode, String destinationPincode) {
        this.sourcePincode = sourcePincode;
        this.destinationPincode = destinationPincode;
    }

    public String getSourcePincode() {
        return sourcePincode;
    }

    public void setSourcePincode(String sourcePincode) {
        this.sourcePincode = sourcePincode;
    }

    public String getDestinationPincode() {
        return destinationPincode;
    }

    public void setDestinationPincode(String destinationPincode) {
        this.destinationPincode = destinationPincode;
    }

    @jakarta.validation.constraints.AssertTrue(message = "Source and destination pincodes cannot be the same")
    public boolean isDifferentPincodes() {
        if (sourcePincode == null || destinationPincode == null) {
            return true;
        }
        return !sourcePincode.equals(destinationPincode);
    }

    @Override
    public String toString() {
        return "TollPlazaRequest{" +
                "sourcePincode='" + sourcePincode + '\'' +
                ", destinationPincode='" + destinationPincode + '\'' +
                '}';
    }
}
