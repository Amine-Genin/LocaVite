package ma.locavite.sale.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ma.locavite.sale.dto.SaleListingRequest;

public class VehicleConditionValidator implements ConstraintValidator<ValidVehicleCondition, SaleListingRequest> {

    @Override
    public boolean isValid(SaleListingRequest request, ConstraintValidatorContext context) {
        if (request == null || request.etat() == null) {
            return true;
        }

        String error = request.vehicleDetailsValidationError();
        if (error == null) {
            return true;
        }

        String field = request.vehicleDetailsValidationField();
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(error)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
