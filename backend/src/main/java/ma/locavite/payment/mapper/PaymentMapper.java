package ma.locavite.payment.mapper;

import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.dto.PaymentRequest;
import ma.locavite.payment.dto.PaymentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toResponse(Payment payment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    Payment toEntity(PaymentRequest request);
}
