package ma.locavite.sale.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = VehicleConditionValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidVehicleCondition {

    String message() default "L'état du véhicule ne correspond pas à ses caractéristiques";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
